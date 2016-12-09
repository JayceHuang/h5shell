package com.lzy.imagepicker.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.format.Formatter;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.lzy.imagepicker.ImagePicker;
import com.lzy.imagepicker.MResource;
import com.lzy.imagepicker.bean.ImageItem;
import com.lzy.imagepicker.view.SuperCheckBox;

/**
 * ================================================
 * 作    者：jeasonlzy（廖子尧 Github地址：https://github.com/jeasonlzy0216
 * 版    本：1.0
 * 创建日期：2016/5/19
 * 描    述：
 * 修订历史：
 * ================================================
 */
public class ImagePreviewActivity extends ImagePreviewBaseActivity implements ImagePicker.OnImageSelectedListener, View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    public static final String ISORIGIN = "isOrigin";
    public static final String IS_CAMERA_PREVIEW = "is_camera_preview";

    private boolean isOrigin;                      //是否选中原图
    private boolean isCameraPreview;               //是否来自照相机预览
    private SuperCheckBox mCbCheck;                //是否选中当前图片的CheckBox
    private SuperCheckBox mCbOrigin;               //原图
    private Button mBtnOk;                         //确认图片的选择
    private View bottomBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isOrigin = getIntent().getBooleanExtra(ImagePreviewActivity.ISORIGIN, false);
        isCameraPreview = getIntent().getBooleanExtra(ImagePreviewActivity.IS_CAMERA_PREVIEW, false);

        initData();
        imagePicker.addOnImageSelectedListener(this);

        int btnOkId = MResource.getIdByName(this,"id","btn_ok");
        int bottomBarId = MResource.getIdByName(this,"id","bottom_bar");
        int cbCheckId = MResource.getIdByName(this,"id","cb_check");
        int cbOriginId = MResource.getIdByName(this,"id","cb_origin");

        int originId = MResource.getIdByName(this,"string","txwp_origin");
        final int imageCountId = MResource.getIdByName(this,"string","txwp_preview_image_count");
        final int selectLimitId = MResource.getIdByName(this,"string","txwp_select_limit");

        mBtnOk = (Button) topBar.findViewById(btnOkId);
        mBtnOk.setVisibility(View.VISIBLE);
        mBtnOk.setOnClickListener(this);

        bottomBar = findViewById(bottomBarId);
        bottomBar.setVisibility(View.VISIBLE);

        mCbCheck = (SuperCheckBox) findViewById(cbCheckId);

        mCbOrigin = (SuperCheckBox) findViewById(cbOriginId);
        if (ImagePicker.getInstance().isShowOrgImg())
            mCbOrigin.setVisibility(View.VISIBLE);
        else
            mCbOrigin.setVisibility(View.GONE);

        mCbOrigin.setText(getString(originId));
        mCbOrigin.setOnCheckedChangeListener(this);
        mCbOrigin.setChecked(isOrigin);


        ImageItem item = mImageItems.get(mCurrentPosition);
        boolean isSelected = imagePicker.isSelect(item);
        mTitleCount.setText(getString(imageCountId, mCurrentPosition + 1, mImageItems.size()));
        mCbCheck.setChecked(isSelected);
        //滑动ViewPager的时候，根据外界的数据改变当前的选中状态和当前的图片的位置描述文本
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mCurrentPosition = position;
                ImageItem item = mImageItems.get(mCurrentPosition);
                boolean isSelected = imagePicker.isSelect(item);
                mCbCheck.setChecked(isSelected);
                mTitleCount.setText(getString(imageCountId, mCurrentPosition + 1, mImageItems.size()));
            }
        });
        //当点击当前选中按钮的时候，需要根据当前的选中状态添加和移除图片
        mCbCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageItem imageItem = mImageItems.get(mCurrentPosition);
                int selectLimit = imagePicker.getSelectLimit();
                if (mCbCheck.isChecked() && selectedImages.size() >= selectLimit) {
                    Toast.makeText(ImagePreviewActivity.this, ImagePreviewActivity.this.getString(selectLimitId, selectLimit), Toast.LENGTH_SHORT).show();
                    mCbCheck.setChecked(false);
                } else {
                    imagePicker.addSelectedImageItem(mCurrentPosition, imageItem, mCbCheck.isChecked());
                }
            }
        });

        //初始化当前页面的状态
        onImageSelected(0, null, true);
    }

    private void initData() {
        int type = ImagePicker.getInstance().getSizeTypeId();
        if(type == ImagePicker.ORIGINAL_SIZE_ONLY){
            isOrigin = true;
            ImagePicker.getInstance().setShowOrgImg(false);
        }else if(type == ImagePicker.COMPRESSED_SIZE_ONLY ){
            isOrigin = false;
            ImagePicker.getInstance().setShowOrgImg(false);
        }else if(type == ImagePicker.ORIGINAL_COMPRESSED_SIZE){
            ImagePicker.getInstance().setShowOrgImg(true);
        }
    }
    /**
     * 图片添加成功后，修改当前图片的选中数量
     * 当调用 addSelectedImageItem 或 deleteSelectedImageItem 都会触发当前回调
     */
    @Override
    public void onImageSelected(int position, ImageItem item, boolean isAdd) {
        final int selectCompleteId = MResource.getIdByName(this,"string","txwp_select_complete");
        final int completeId = MResource.getIdByName(this,"string","txwp_complete");
        final int originId = MResource.getIdByName(this,"string","txwp_origin");
        final int originSizeId = MResource.getIdByName(this,"string","txwp_origin_size");
        if (isCameraPreview){
            mBtnOk.setText(getString(completeId));
            mBtnOk.setEnabled(mCbCheck.isChecked());
        }
        else if (imagePicker.getSelectImageCount() > 0) {
            mBtnOk.setText(getString(selectCompleteId, imagePicker.getSelectImageCount(), imagePicker.getSelectLimit()));
            mBtnOk.setEnabled(true);
        } else {
            mBtnOk.setText(getString(completeId));
            mBtnOk.setEnabled(false);
        }

        if (mCbOrigin.isChecked()) {
            long size = 0;
            for (ImageItem imageItem : selectedImages)
                size += imageItem.size;
            if (size == 0){
                mCbOrigin.setText(getString(originId));
            }else {
                String fileSize = Formatter.formatFileSize(this, size);
                mCbOrigin.setText(getString(originSizeId, fileSize));
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        int btnOkId = MResource.getIdByName(this,"id","btn_ok");
        int btnBackId = MResource.getIdByName(this,"id","btn_back");

        if (id == btnOkId) {
            Intent intent = new Intent();
            if (isCameraPreview){
                ImageItem imageItem = new ImageItem();
                if (imagePicker.getTakeImageFile() != null){
                    imageItem.path = imagePicker.getTakeImageFile().getAbsolutePath();
                    imagePicker.clearSelectedImages();
                    imagePicker.addSelectedImageItem(0, imageItem, true);
                }

            }
            intent.putExtra(ImagePicker.EXTRA_RESULT_ITEMS, imagePicker.getSelectedImages());
            intent.putExtra(ImagePicker.EXTRA_RESULT_IS_ORGIN,isOrigin);
            setResult(ImagePicker.RESULT_CODE_ITEMS, intent);
            finish();
        } else if (id == btnBackId) {
            Intent intent = new Intent();
            intent.putExtra(ImagePreviewActivity.ISORIGIN, isOrigin);
            setResult(ImagePicker.RESULT_CODE_BACK, intent);
            finish();
        }
    }



    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra(ImagePreviewActivity.ISORIGIN, isOrigin);
        setResult(ImagePicker.RESULT_CODE_BACK, intent);
        finish();
        super.onBackPressed();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int id = buttonView.getId();
        final int cbOriginId = MResource.getIdByName(this,"id","cb_origin");

        final int originId = MResource.getIdByName(this,"string","txwp_origin");
        final int originSizeId = MResource.getIdByName(this,"string","txwp_origin_size");
        if (id == cbOriginId) {
            if (isChecked) {
                long size = 0;
                for (ImageItem item : selectedImages)
                    size += item.size;
                isOrigin = true;

                if (size == 0){
                    mCbOrigin.setText(getString(originId));
                }else{
                    String fileSize = Formatter.formatFileSize(this, size);
                    mCbOrigin.setText(getString(originSizeId, fileSize));
                }

            } else {
                isOrigin = false;
                mCbOrigin.setText(getString(originId));
            }
        }
    }

    @Override
    protected void onDestroy() {
        imagePicker.removeOnImageSelectedListener(this);
        super.onDestroy();
    }

    /** 单击时，隐藏头和尾 */
    @Override
    public void onImageSingleTap() {
        final int topOutId = MResource.getIdByName(this,"anim","txwp_top_out");
        final int fadeOutId = MResource.getIdByName(this,"anim","txwp_fade_out");
        final int topInId = MResource.getIdByName(this,"anim","txwp_top_in");
        final int fadeInId = MResource.getIdByName(this,"anim","txwp_fade_in");

        if (topBar.getVisibility() == View.VISIBLE) {
            topBar.setAnimation(AnimationUtils.loadAnimation(this, topOutId));
            bottomBar.setAnimation(AnimationUtils.loadAnimation(this, fadeOutId));
            topBar.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
            tintManager.setStatusBarTintResource(MResource.getIdByName(this,"color","txwp_picker_transparent"));//通知栏所需颜色
            //给最外层布局加上这个属性表示，Activity全屏显示，且状态栏被隐藏覆盖掉。
            if (Build.VERSION.SDK_INT >= 16) content.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        } else {
            topBar.setAnimation(AnimationUtils.loadAnimation(this, topInId));
            bottomBar.setAnimation(AnimationUtils.loadAnimation(this, fadeInId));
            topBar.setVisibility(View.VISIBLE);
            bottomBar.setVisibility(View.VISIBLE);
            tintManager.setStatusBarTintResource(MResource.getIdByName(this,"color","txwp_status_bar"));//通知栏所需颜色
            //Activity全屏显示，但状态栏不会被隐藏覆盖，状态栏依然可见，Activity顶端布局部分会被状态遮住
            if (Build.VERSION.SDK_INT >= 16) content.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }
}
