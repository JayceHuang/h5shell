package com.tencent.photoview;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.polites.android.GestureImageView;
import com.tencent.photoview.ImageManager.SimpleImageListener;

import java.util.List;

public class ImageViewerAdapter extends PagerAdapter {

    //改为使用新图像结构
    private List<String> mImageData;
    private Context mContext;
    private LayoutInflater mInflate;
    private View.OnClickListener onItemClickListener;
    private View.OnLongClickListener onLongClickListener;

    public ImageViewerAdapter(List<String> imageData, Context context) {
        mImageData = imageData;
        mContext = context;
        mInflate = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        if (mImageData != null)
            return mImageData.size();
        return 0;
    }

    @Override
    public int getItemPosition(Object object) {
        // TODO Auto-generated method stub
        return POSITION_NONE;
    }

    @Override
    public boolean isViewFromObject(View arg0, Object arg1) {
        // TODO Auto-generated method stub
        return arg0 == arg1;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View myView = mInflate.inflate(MResource.getIdByName(mContext,"layout","txwp_item_imager_viewer"), null);

        final GestureImageView imageView = (GestureImageView) myView
                .findViewById(MResource.getIdByName(mContext,"id","image"));
        final ProgressBar loadProcess = (ProgressBar) myView.findViewById(MResource.getIdByName(mContext,"id","load_process"));
        final TextView loadFailView = (TextView) myView.findViewById(MResource.getIdByName(mContext,"id","load_fail"));

        final String uri = mImageData.get(position);

        loadProcess.setVisibility(View.VISIBLE);
        imageView.setVisibility(View.VISIBLE);

        ImageManager.ImageListener listener = new SimpleImageListener() {

            @Override
            public void onFailed(String url, ImageView imageView,
                                 Throwable e) {
                        // TODO Auto-generated method stub
                loadFailView.setVisibility(View.VISIBLE);
                loadProcess.setVisibility(View.GONE);

            }

            @Override
            public void onComplete(String url, ImageView imageView,
                                   Bitmap bitmap) {
                loadProcess.setVisibility(View.GONE);
                imageView.setTag(bitmap);
                imageView.setVisibility(View.VISIBLE);

            }
        };

        // 加载大图
        ImageManager.getInstance().displayImage(mContext, uri, imageView,
                null, listener);


        if (onItemClickListener != null)
            imageView.setOnClickListener(onItemClickListener);
        if (onLongClickListener != null)
            imageView.setOnLongClickListener(onLongClickListener);
        container.addView(myView);
        return myView;

    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        // TODO Auto-generated method stub
        container.removeView((View) object);
        if (object instanceof GestureImageView) {
            // TODO:
            // ImageLoader.getInstance().cancelDisIplayTask((GestureImageView)
            // object);
        }

    }


    public void setOnItemClickListener(View.OnClickListener listener) {
        onItemClickListener = listener;
    }

    public void setOnItemLongClickListener(View.OnLongClickListener listener) {
        onLongClickListener = listener;
    }

    public String getFileUri(String uri) {
        if (uri == null)
            return null;
        if (uri.startsWith("file:///"))
            return uri;
        return "file:///" + uri;

    }

}
