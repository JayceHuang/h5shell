package com.tencent.photoview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageView;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener;

/**
 * 
 * @author trentyang
 * 封装图片加载库，方便切换不同的库（universalimageloader， glide），上层不用做任何修改
 * 
 * http://nyimage-10007781.image.myqcloud.com/981039256081682432-93782aa5-1584-4c04-a7c4-67faac3fa45d-1446522667533.jpg      => 原图
 * http://nyimage-10007781.image.myqcloud.com/981039256081682432-93782aa5-1584-4c04-a7c4-67faac3fa45d-1446522667533.jpg/216  => 等比例缩放不超过216x216
 * http://nyimage-10007781.image.myqcloud.com/981039256081682432-93782aa5-1584-4c04-a7c4-67faac3fa45d-1446522667533.jpg/438  => 等比例缩放不超过438x438
 * 
 */
public class ImageManager {
    
    private static final String TAG = ImageManager.class.getSimpleName();
    private static final boolean sUseUIL = true; // true=>universalimageloader, false=>glide

    private static class Singleton {
        private static ImageManager sInstance = new ImageManager();
    }

    /**
     * 显示控制选项
     * @author trentyang
     *
     */
    public static class Options {
        public int loadingImageResId; // 加载中图片资源id
        public int failedImageResId; // 加载失败图片资源id
        public int sampleSize; // 为了减少内存使用，2的幂次方
        public boolean useRGB565; // 为了减少内存使用，使用2字节表示一个像素

        public Options(int loadingImageResId, int failedImageResId) {
            this(loadingImageResId, failedImageResId, 0, false);
        }

        public Options(int loadingImageResId, int failedImageResId, int sampleSize) {
            this(loadingImageResId, failedImageResId, sampleSize, false);
        }

        public Options(int loadingImageResId, int failedImageResId, int sampleSize, boolean useRGB565) {
            this.loadingImageResId = loadingImageResId;
            this.failedImageResId = failedImageResId;
            this.sampleSize = sampleSize;
            this.useRGB565 = useRGB565;
        }
    }

    /**
     * 加载监听器
     * @author trentyang
     *
     */
    public interface ImageListener {
        void onFailed(String url, ImageView imageView, Throwable e);

        void onComplete(String url, ImageView imageView, Bitmap bitmap);

        void onProgress(String url, ImageView imageView, long totalSize, long downloadSize);
    }

    /**
     * 加载监听器包装类
     * @author trentyang
     *
     */
    public static class SimpleImageListener implements ImageListener {
        @Override
        public void onFailed(String url, ImageView imageView, Throwable e) {
        }

        @Override
        public void onComplete(String url, ImageView imageView, Bitmap bitmap) {
        }

        @Override
        public void onProgress(String url, ImageView imageView, long totalSize, long downloadSize) {
        }
    }

    public static ImageManager getInstance() {
        return Singleton.sInstance;
    }

    /**
     * 初始化
     * @param context
     */
    public void init(Context context) {
        if (sUseUIL) {
            DisplayImageOptions displayImageOptions = new DisplayImageOptions.Builder() //
                    .cacheInMemory(true) // 使用内存缓存
                    .cacheOnDisk(true) // 使用磁盘缓存
                    // .showImageForEmptyUri(0) //
                    // .showImageOnFail(0) //
                    // .showImageOnLoading(0) //
                    .build();
            ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context.getApplicationContext())
                    .memoryCacheSizePercentage(25) // 内存缓存设置25%的heap
                    .defaultDisplayImageOptions(displayImageOptions) // 默认配置
                    .build();
            ImageLoader.getInstance().init(config);
        } else {
            // glide内部使用ImageView的setTag()保存Request对象，上层有的地方已经使用setTag保存其他数据，这里额外指定另外的tagId
            //ViewTarget.setTagId(R.id.glide_image_tag_id);
        }
    }

    /**
     * 加载图片
     * @param context
     * @param url
     * @param imageView
     */
    public void displayImage(Context context, String url, ImageView imageView) {
        displayImage(context, url, imageView, null, null);
    }

    /**
     * 加载图片
     * @param context
     * @param url
     * @param imageView
     * @param options
     */
    public void displayImage(Context context, String url, ImageView imageView, Options options) {
        displayImage(context, url, imageView, options, null);
    }

    /**
     * 加载图片
     * @param context
     * @param url
     * @param imageView
     * @param options
     * @param listener
     */
    public void displayImage(Context context, String url, ImageView imageView, Options options,
                             final ImageListener listener) {
        if (sUseUIL) {
            DisplayImageOptions.Builder builder = new DisplayImageOptions.Builder();
            builder.cacheInMemory(true).cacheOnDisk(true);
            if (options != null) {
                if (options.sampleSize > 0) {
                    BitmapFactory.Options bmPptions = new BitmapFactory.Options();
                    bmPptions.inSampleSize = options.sampleSize;
                    builder.decodingOptions(bmPptions);
                }
                if (options.useRGB565) {
                    builder.bitmapConfig(Bitmap.Config.RGB_565);
                }
                if (options.loadingImageResId > 0) {
                    builder.showImageOnLoading(options.loadingImageResId);
                }
                if (options.loadingImageResId > 0) {
                    builder.showImageOnFail(options.failedImageResId);
                }
            }
            DisplayImageOptions displayImageOptions = builder.build();
            ImageLoader.getInstance().displayImage(url, imageView, displayImageOptions, new ImageLoadingListener() {
                @Override
                public void onLoadingStarted(String url, View view) {
                    // Nothing
                }

                @Override
                public void onLoadingCancelled(String url, View view) {
                    // Nothing
                }

                @Override
                public void onLoadingFailed(String url, View view, FailReason failReason) {
                    if (listener != null) {
                        listener.onFailed(url, (ImageView) view, failReason != null ? failReason.getCause() : null);
                    }
                }

                @Override
                public void onLoadingComplete(String url, View view, Bitmap bitmap) {
                    if (listener != null) {
                        listener.onComplete(url, (ImageView) view, bitmap);
                    }
                }

            }, new ImageLoadingProgressListener() {

                @Override
                public void onProgressUpdate(String url, View view, int current, int total) {
                    if (listener != null) {
                        listener.onProgress(url, (ImageView) view, total, current);
                    }
                }

            });
        } else {
//            DrawableTypeRequest<String> request = Glide.with(context).load(url);
//            if (options != null) {
//                if (options.sampleSize > 0) {
//                    float sizeMultiplier = 1f / options.sampleSize;
//                    request.thumbnail(sizeMultiplier);
//                }
//                if (options.loadingImageResId > 0) {
//                    request.placeholder(options.loadingImageResId);
//                }
//                if (options.failedImageResId > 0) {
//                    request.error(options.failedImageResId);
//                }
//            }
//            // TODO: 没有进度通知接口
//            request.listener(new RequestListener<String, GlideDrawable>() {
//                @Override
//                public boolean onException(Exception e, String url, Target<GlideDrawable> target,
//                                           boolean isFirstResource) {
//                    if (listener != null) {
//                        listener.onFailed(url, (ImageView) ((ImageViewTarget<GlideDrawable>) target).getView(), e);
//                    }
//                    return false;
//                }
//
//                @Override
//                public boolean onResourceReady(GlideDrawable drawable, String url, Target<GlideDrawable> target,
//                                               boolean isFromMemoryCache, boolean isFirstResource) {
//                    GlideBitmapDrawable bitmapDrawable = (GlideBitmapDrawable) drawable;
//                    if (listener != null) {
//                        listener.onComplete(url, (ImageView) ((ImageViewTarget<GlideDrawable>) target).getView(),
//                                bitmapDrawable.getBitmap());
//                    }
//                    return false;
//                }
//            });
//            request.into(imageView);
        }
    }

}
