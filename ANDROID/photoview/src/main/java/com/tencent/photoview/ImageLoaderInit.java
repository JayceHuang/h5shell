package com.tencent.photoview;

import android.content.Context;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

/**
 * Created by benpeng on 2016/8/1.
 */
public class ImageLoaderInit {
    public static  boolean hasInit = false;
    public static  void init(Context context)
    {
        if (hasInit)
            return;
        hasInit = true;

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
    }
}
