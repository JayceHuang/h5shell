package com.tencent.doh.plugins;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.Toast;

import com.lzy.imagepicker.ImagePicker;
import com.lzy.imagepicker.bean.ImageItem;
import com.lzy.imagepicker.ui.ImageGridActivity;
import com.lzy.imagepicker.ui.ImagePreviewActivity;
import com.tencent.doh.pluginframework.Config;
import com.tencent.doh.pluginframework.util.FileUtils;
import com.tencent.doh.pluginframework.util.ImageUtils;
import com.tencent.doh.pluginframework.util.LRUCache;
import com.tencent.doh.pluginframework.util.LogUtils;
import com.tencent.doh.pluginframework.util.MResource;
import com.tencent.doh.pluginframework.util.StorageUtils;
import com.tencent.doh.pluginframework.webview.WebViewPlugin;
import com.tencent.doh.plugins.cloud.CloudManager;
import com.tencent.doh.plugins.cloud.CloudUtils;
import com.tencent.doh.plugins.cloud.DownloadManager;
import com.tencent.doh.plugins.cloud.UploadManager;
import com.tencent.download.Downloader;
import com.tencent.download.core.DownloadResult;
import com.tencent.photoview.ImageLoaderInit;
import com.tencent.photoview.ImageViewerActivity;
import com.tencent.smtt.export.external.interfaces.WebResourceResponse;
import com.tencent.upload.task.ITask;
import com.tencent.upload.task.IUploadTaskListener;
import com.tencent.upload.task.data.FileInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.SimpleTimeZone;

/**
 * Created by benpeng on 2016/7/28.
 */
public class ImagePlugin extends WebViewPlugin {

    public final static byte CODE_OPEN_CAMERA = 1;
    public final static byte CODE_PICK_PHOTO = 2;
    public final static byte CODE_REQUEST_PREVIEW = 3;

    public static final String IMAGE_COMPRESSED_DIR = "/image_compressed";//文件夹名字
    public final static String LOCAL_RES_HEADER = "doh://image/";

    // 用于格式化日期,作为日志文件名的一部分
    private    DateFormat formatter;
    private static LRUCache<String, String> localIdLruCache = new LRUCache<String, String>(100);

    private static LRUCache<String, String> remoteIdLruCache = new LRUCache<String, String>(100);
    final static String CAMERA_PHOTO_PATH = "camera_photo_path";
    final static String KEY_GET_PICTURE_PARAM = "getPictureParam";

    final static String KEY_TAKE_PICTURE_PARAM = "takePictureParam";
    final static int SHOW_ONLY_CAMERA = 1;
    final static int SHOW_ONLY_ALBUM = SHOW_ONLY_CAMERA + 1;
    final static int SHOW_CAMERA_ALBUM = SHOW_ONLY_ALBUM + 1;

    private Dialog loadingDialog;
    private boolean mCompressed;
    private int mSizeTypeId;
    private File imageCacheDir;

    @Override
    protected void onCreate() {
        super.onCreate();
        //每3天清除一次缓存,和IOS保持一致
        FileUtils.deleteTimeoutFile(getImageCachePath(), 3);
        ImageLoaderInit.init(mRuntime.getActivity());
        initCacheFile();
    }

    private void initCacheFile() {
        formatter  = new SimpleDateFormat("yyyy-MM-dd");
        //图片缓存文件夹
        String path =getImageCachePath();
        imageCacheDir = new File(path);
        if (!imageCacheDir.exists()) {
            imageCacheDir.mkdirs();
        }
        //添加屏蔽文件
        try {
            File  nomedia = new File(imageCacheDir,"/.nomedia");
        if (! nomedia.exists()){
            nomedia.createNewFile();
        }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getImageCachePath(){
        return Config.getCacheFileDirPath()+IMAGE_COMPRESSED_DIR;
    }

    @Override
    protected boolean handleJsRequest(String url, String pkgName, String method, String... args) {
        if ("chooseImage".equalsIgnoreCase(method)) {
            handleChooseImage(args);
        } else if ("previewImage".equalsIgnoreCase(method)) {
            handlePreviewImage(args);
        } else if ("uploadImage".equalsIgnoreCase(method)) {
            handleUploadImage(args);
        } else if ("downloadImage".equalsIgnoreCase(method)) {
            handleDownloadImage(args);
        } else {
            return false;
        }
        return true;
    }

    @Override
    protected Object handleEvent(String url, int type) {
        if (!url.startsWith(LOCAL_RES_HEADER) && type != WebViewPlugin.EVENT_LOAD_RESOURCE)
            return super.handleEvent(url, type);

        String realPath = getRealLocalPath(url);
        if (realPath == null)
            return null;

        File realPathFile = new File(realPath);
        if (realPathFile.exists()) {

            String mime = "image/*";

//            if (realPath.contains(".jpg") || realPath.contains(".gif") || realPath.contains(".png")
//                    || realPath.contains(".jpeg")) {
//                mime = "image/*";
//            }

            WebResourceResponse res = null;
            try {
                res = new WebResourceResponse(mime, "utf-8", new FileInputStream(realPathFile));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            return res;
        }
        return null;
    }


    public String getRealLocalPath(String withHeaderPath) {
        if (withHeaderPath == null)
            return null;

        if (!withHeaderPath.startsWith(LOCAL_RES_HEADER)) {
            throw new IllegalArgumentException(
                    "Invalid input file path");
        }

        int beginIndex = withHeaderPath.indexOf(LOCAL_RES_HEADER);

        String readFileMd5 = withHeaderPath.substring(beginIndex + LOCAL_RES_HEADER.length());

        return localIdLruCache.get(readFileMd5);
    }

    private void handlePreviewImage(String[] args) {
        {
            JSONObject reqParam;
            final String callback;
            try {
                reqParam = new JSONObject(args[0]);
                callback = reqParam.optString(KEY_CALLBACK);
                JSONArray imgJsonArray = reqParam.getJSONArray("urls");
                String cutImgUrl = reqParam.getString("current");

                List<String> imgList = jsonArrayToList(imgJsonArray);

                if (imgList == null || imgList.size() <= 0)
                    return;

                int cur = imgList.indexOf(cutImgUrl);
                if (cur < 0)
                    cur = 0;

                enterImageViewer((ArrayList<String>) imgList, cur);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private List<String> jsonArrayToList(JSONArray jsonArray) {
        if (jsonArray == null || jsonArray.length() <= 0)
            return null;

        List<String> retList = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                retList.add(jsonArray.getString(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return retList;
    }

    private int getSourceType(JSONObject reqParam) {
        try {
            if (reqParam == null || !reqParam.has("sourceType"))
                return SHOW_CAMERA_ALBUM;

            JSONArray sourceTypeArray = reqParam.getJSONArray("sourceType");
            int size = sourceTypeArray.length();
            if (size == 0) {
                return SHOW_CAMERA_ALBUM;
            }

            String sourceTypeName;
            boolean hasCamera = false;
            boolean hasAlbum = false;
            for (int i = 0; i < size; i++) {
                sourceTypeName = sourceTypeArray.get(i).toString();
                if (sourceTypeName == null)
                    continue;
                if (sourceTypeName.equalsIgnoreCase("camera"))
                    hasCamera = true;
                else if (sourceTypeName.equalsIgnoreCase("album"))
                    hasAlbum = true;
            }

            if (hasCamera && hasAlbum)
                return SHOW_CAMERA_ALBUM;
            else if (hasAlbum)
                return SHOW_ONLY_ALBUM;
            else if (hasCamera)
                return SHOW_ONLY_CAMERA;
            else
                return SHOW_CAMERA_ALBUM;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return SHOW_CAMERA_ALBUM;
    }

    private int getSizeType(JSONObject reqParam) {
        try {
            if (reqParam == null || !reqParam.has("sizeType"))
                return ImagePicker.ORIGINAL_COMPRESSED_SIZE;

            JSONArray sizeTypeArray = reqParam.getJSONArray("sizeType");
            int size = sizeTypeArray.length();
            if (size == 0) {
                return ImagePicker.ORIGINAL_COMPRESSED_SIZE;
            }

            String sizeTypeName;
            boolean hasOriginal = false;
            boolean hasCompressed = false;
            for (int i = 0; i < size; i++) {
                sizeTypeName = sizeTypeArray.get(i).toString();
                if (sizeTypeName == null)
                    continue;
                if (sizeTypeName.equalsIgnoreCase("original"))
                    hasOriginal = true;
                else if (sizeTypeName.equalsIgnoreCase("compressed"))
                    hasCompressed = true;
            }

            if (hasOriginal && hasCompressed)
                return ImagePicker.ORIGINAL_COMPRESSED_SIZE;
            else if (hasOriginal)
                return ImagePicker.ORIGINAL_SIZE_ONLY;
            else if (hasCompressed)
                return ImagePicker.COMPRESSED_SIZE_ONLY;
            else
                return ImagePicker.ORIGINAL_COMPRESSED_SIZE;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ImagePicker.ORIGINAL_COMPRESSED_SIZE;
    }

   private void commonRspError(String  callbackId,int code,String msg){
       JSONObject resp = new JSONObject();
       callJs(callbackId, getResult(code, msg, resp));
   }

    private boolean handleChooseImage(String[] args) {
        if (args.length == 1) {
            JSONObject reqParam;
            final String callback;
            try {
                reqParam = new JSONObject(args[0]);
                callback = reqParam.optString(KEY_CALLBACK);

                int maxCount = 9;//默认为9
                if (reqParam.has("count")) {
                    maxCount = reqParam.getInt("count");
                }
                if (maxCount <= 0 || maxCount > 9){
                    commonRspError(callback,-2,"error!! count must  between 1 and 9.");
                    return true;
                }

                mSizeTypeId = getSizeType(reqParam);

                int sourceTypeId = getSourceType(reqParam);
                if (sourceTypeId == SHOW_ONLY_CAMERA) {
                    return handleOnlyCamera(reqParam);
                } else {
                    handleNotCameraPic(callback, maxCount, sourceTypeId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        } else {
            return false;
        }
    }

    private void handleNotCameraPic(String callback, int maxCount, int sourceTypeId) {
        boolean showCamera = true;
        PreferenceManager.getDefaultSharedPreferences(mRuntime.context).edit()
                .putString(KEY_GET_PICTURE_PARAM, callback)
                .commit();
        if (sourceTypeId == SHOW_ONLY_ALBUM) {
            showCamera = false;
        }

        startActivityChooseImage(showCamera, maxCount);
    }

    private void cameraCompletePreview(String path) {
        Intent intent = new Intent(mRuntime.getActivity(), ImagePreviewActivity.class);
        intent.putExtra(ImagePicker.EXTRA_SELECTED_IMAGE_POSITION, 0);
        ArrayList<ImageItem> reqImages = new ArrayList<>(1);
        ImageItem item = new ImageItem();
        item.path = path;
        item.size = FileUtils.getFileSize(path);
        reqImages.add(item);
        ImagePicker.getInstance().setSelectLimit(1);
        ImagePicker.getInstance().setSizeTypeId(mSizeTypeId);
        ImagePicker.getInstance().addSelectedImageItem(0, item, true);
        intent.putExtra(ImagePreviewActivity.IS_CAMERA_PREVIEW,true);
        intent.putExtra(ImagePicker.EXTRA_IMAGE_ITEMS, reqImages);
        startActivityForResult(intent, CODE_REQUEST_PREVIEW);
    }

    private boolean handleOnlyCamera(JSONObject reqParam) {
        String callback = reqParam.optString(KEY_CALLBACK);

        String basePath = Config.getCacheFileDirPath();
        File folder = new File(basePath);
        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                Toast.makeText(mRuntime.context, "无SD卡，请插入SD卡后再试", Toast.LENGTH_SHORT).show();
                JSONObject resp = new JSONObject();
                callJs(callback, getResult(-1, "", resp));
                return true;
            }
        }
        // 兼容moto手机：拍照后不返回照片的处理方法，先将拍照图片的path存到setting
        String path = basePath + System.currentTimeMillis() + ".jpg";
        Uri uploadPhotoUri = Uri.fromFile(new File(path));
        // PreferenceManager.getDefaultSharedPreferences(context).edit().putString(AppConstants.Preferences.CAMERA_PHOTO_PATH, path).commit();
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uploadPhotoUri);
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 100);

        try {
            startActivityForResult(intent, CODE_OPEN_CAMERA);
            PreferenceManager.getDefaultSharedPreferences(mRuntime.context).edit()
                    .putString(CAMERA_PHOTO_PATH, path)
                    .putString(KEY_TAKE_PICTURE_PARAM, reqParam.toString())
                    .commit();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(mRuntime.context, "相机启动失败", Toast.LENGTH_SHORT).show();
        }

        return true;
    }


    private void startActivityChooseImage(boolean showCamera, int maxNum) {
        if (mRuntime.getActivity() == null) {
            return;
        }
        ImagePicker imagePicker = ImagePicker.getInstance();
        imagePicker.setShowCamera(showCamera);
        imagePicker.setSelectLimit(maxNum);
        imagePicker.setSizeTypeId(mSizeTypeId);
        Intent intent = new Intent(mRuntime.getActivity(), ImageGridActivity.class);
        startActivityForResult(intent, CODE_PICK_PHOTO);
    }


    private void enterImageViewer(ArrayList<String> datas, int itemIndex) {
        if (mRuntime.getActivity() == null) {
            return;
        }
        Intent intent = new Intent(mRuntime.getActivity(), ImageViewerActivity.class);
        Bundle mBundle = new Bundle();
        mBundle.putStringArrayList(ImageViewerActivity.INTENT_EX_IMAGE_DATA, datas);
        mBundle.putInt(ImageViewerActivity.INTENT_EX_CURRENT_INDEX, itemIndex);
        intent.putExtras(mBundle);
        mRuntime.getActivity().startActivity(intent);
    }

    private void handleUploadImage(String[] args) {
        JSONObject reqParam;
        final String callback;
        try {
            reqParam = new JSONObject(args[0]);
            callback = reqParam.optString(KEY_CALLBACK);

            String localId = reqParam.getString("localId");

            String realPath = getRealLocalPath(localId);

            if (realPath == null)
                return;

            int isShowProgressTips = 1;
            if (reqParam.has("isShowProgressTips")) {
                isShowProgressTips = reqParam.getInt("isShowProgressTips");
            }

            if (isShowProgressTips == 1) {
                showloadingDialog();
            }

            String name = getName(realPath);
            UploadManager.getInstance().uploadSingleFile(CloudManager.PHOTO_BUKET, realPath, name, new IUploadTaskListener() {
                @Override
                public void onUploadSucceed(FileInfo fileInfo) {
                    dismissDialog();
                    String fileUrl = fileInfo.url;
                    try {
                        JSONObject result = new JSONObject();
                        result.put("serverId", remotePathToCache(fileUrl));
                        callJs(callback, getResult(result));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onUploadFailed(int i, String s) {
                    dismissDialog();
                    try {
                        JSONObject result = new JSONObject();
                        callJs(callback, getResult(i, s, result));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onUploadProgress(long l, long l1) {
                    LogUtils.i(TAG, l + "  " + l1);
                }

                @Override
                public void onUploadStateChange(ITask.TaskState taskState) {

                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleDownloadImage(String[] args) {
        JSONObject reqParam;
        final String callback;
        try {
            reqParam = new JSONObject(args[0]);
            callback = reqParam.optString(KEY_CALLBACK);
            String urlId = reqParam.getString("serverId");

            String remotePath = remoteIdLruCache.get(urlId);
            if (remotePath == null)
                return;

            int isShowProgressTips = 1;
            if (reqParam.has("isShowProgressTips")) {
                isShowProgressTips = reqParam.getInt("isShowProgressTips");
            }
            if (isShowProgressTips == 1) {
                showloadingDialog();
            }

            DownloadManager.getInstance().download(remotePath, new Downloader.DownloadListener() {
                @Override
                public void onDownloadCanceled(String s) {
                    dismissDialog();
                    downloadCallback(callback, -1, null);
                }

                @Override
                public void onDownloadFailed(String s, DownloadResult downloadResult) {
                    dismissDialog();
                    downloadResult.getMessage();
                    downloadCallback(callback, downloadResult.getErrorCode(), null);
                }

                @Override
                public void onDownloadSucceed(String s, DownloadResult downloadResult) {
                    dismissDialog();
                    downloadCallback(callback, 0, downloadResult.getPath());
                }

                @Override
                public void onDownloadProgress(String s, long l, float v) {

                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void downloadCallback(String callback, int code, String path) {
        try {
            JSONObject result = new JSONObject();
            if (code == 0) {
                result.put("localId", LOCAL_RES_HEADER + localPathToCache(path));
                callJs(callback, getResult(result));
            } else if (code == -1) {
                callJs(callback, getResult(code, "download canceled", result));
            } else {
                callJs(callback, getResult(code, "download fail", result));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ArrayList<String> getImagesPath(Intent data) {
        ArrayList<ImageItem> images = (ArrayList<ImageItem>) data.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS);
        if (images == null)
            return null;

        ArrayList<String> resultImages = new ArrayList<>(images.size());
        String imagePath;
        for (int i = 0; i < images.size(); i++) {
            imagePath = images.get(i).path;
            resultImages.add(imagePath);
        }

        return resultImages;
    }


    @Override
    public void onActivityResult(Intent data, byte requestCode, int resultCode) {
        super.onActivityResult(data, requestCode, resultCode);
        ImagePicker.getInstance().clear();
        if (requestCode == CODE_PICK_PHOTO) {
            imagePickerRsp(data,resultCode);
        } else if (requestCode == CODE_OPEN_CAMERA ){
            if (resultCode == Activity.RESULT_OK){
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mRuntime.context);
                String path = pref.getString(CAMERA_PHOTO_PATH, "");
                cameraCompletePreview(path);
            }else if (resultCode == Activity.RESULT_CANCELED){
                cameraRsp(data,resultCode);
            }

        }
        else if (requestCode == CODE_REQUEST_PREVIEW)
            cameraRsp(data,resultCode);
    }

    private void imagePickerRsp(Intent data,int resultCode){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mRuntime.context);
        String getPictureCallBack = pref.getString(KEY_GET_PICTURE_PARAM, "");
        if (resultCode == ImagePicker.RESULT_CODE_ITEMS) {
            ArrayList<String> result = getImagesPath(data);
            if (data != null)
                mCompressed = !data.getBooleanExtra(ImagePicker.EXTRA_RESULT_IS_ORGIN, false);
            retLocalIds(getPictureCallBack, result.toArray(new String[0]));
            pref.edit().remove(KEY_GET_PICTURE_PARAM).commit();
        }else {
            JSONObject resp = new JSONObject();
            callJs(getPictureCallBack, getResult(-1, "", resp));
        }
    }

    private void cameraRsp(Intent data,int resultCode) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mRuntime.context);
        String path = pref.getString(CAMERA_PHOTO_PATH, "");
        String savedParam = pref.getString(KEY_TAKE_PICTURE_PARAM, "");
        PreferenceManager.getDefaultSharedPreferences(mRuntime.context).edit()
                .remove(CAMERA_PHOTO_PATH)
                .remove(KEY_TAKE_PICTURE_PARAM)
                .commit();
        if (TextUtils.isEmpty(savedParam)) {
            return;
        }
        String callback;
        JSONObject json;
        try {
            json = new JSONObject(savedParam);
            callback = json.getString(KEY_CALLBACK);
            if (TextUtils.isEmpty(callback)) {
                return;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        if (data != null)
            mCompressed = !data.getBooleanExtra(ImagePicker.EXTRA_RESULT_IS_ORGIN, false);
        if (resultCode == ImagePicker.RESULT_CODE_ITEMS) {
            String[] paths = new String[]{path};
            retLocalIds(callback, paths);
            return;
        } else {
            JSONObject resp = new JSONObject();
            callJs(callback, getResult(-1, "", resp));
        }
    }

    private String localPathToCache(String realPath) {

        if (realPath == null || realPath.trim().length() == 0) {
            return null;
        }

        String realPathId = CloudUtils.md5(realPath);
        localIdLruCache.put(realPathId, realPath);
        return realPathId;
    }

    private String remotePathToCache(String remotePath) {

        if (remotePath == null || remotePath.trim().length() == 0) {
            return null;
        }

        String remotePathId = CloudUtils.md5(remotePath);
        remoteIdLruCache.put(remotePathId, remotePath);
        return remotePathId;
    }

    public String saveBitmap(Bitmap bm) {
        String time = formatter.format(new Date());
        try {
            File  mImageFile = File.createTempFile(time+FileUtils.FILE_EXTENSION_SEPARATOR, ".jpg", imageCacheDir);
            FileOutputStream out = new FileOutputStream(mImageFile);
            bm.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();
            return  mImageFile.getAbsolutePath() ;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void retLocalIds(final String callback, final String[] paths) {

        if (callback.trim().length() == 0) {
            return;
        }
        showloadingDialog();
        new Thread(new Runnable() {
            @Override
            public void run() {
                JSONObject obj = new JSONObject();
                JSONArray jsonArray = new JSONArray();
                String path;
                for (int i = 0; i < paths.length; i++) {
                    if (mCompressed) {
                        Bitmap bm = ImageUtils.getCompressedImage(paths[i]);
                        path = saveBitmap(bm);
                        jsonArray.put(LOCAL_RES_HEADER + localPathToCache(path));
                    } else {
                        path = paths[i];
                        jsonArray.put(LOCAL_RES_HEADER + localPathToCache(path));
                    }
                }
                try {
                    obj.put("localIds", jsonArray);
                } catch (JSONException e) {
                    JSONObject resp = new JSONObject();
                    callJs(callback, getResult(-1, "", resp));
                }
                callJs(callback, getResult(obj));
                dismissDialog();
            }
        }).start();
    }


    private String getName(String path) {
        if (TextUtils.isEmpty(path)) {
            return null;
        }
        int index = path.lastIndexOf('/');
        if (index < 0) {
            return path;
        } else {
            return path.substring(index + 1, path.length());
        }
    }

    private void showloadingDialog() {
        loadingDialog = new AlertDialog.Builder(mRuntime.getActivity()).create();
        loadingDialog.show();
        loadingDialog.setContentView(MResource.getIdByName(mRuntime.getContext(), "layout", "txwp_view_loading_dialog"));
    }

    private void dismissDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
            loadingDialog = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissDialog();
    }
}
