package com.tencent.photoview;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * ImageViewer图片浏览器 数据结构
 *
 */
public class ImageViewerData implements Parcelable {
    
    public String mLocalPath;
    public String mUri;

    public ImageViewerData() {
    }

    /**
     * 构造函数
     * @param uri    网络uri
     * @param localPath  本地path路径
     */
    public ImageViewerData(String uri, String localPath) {
        mLocalPath = localPath;
        mUri = uri;
    }
    
    public ImageViewerData(Parcel source){
        mLocalPath = source.readString();
        mUri = source.readString();
    }

    @Override
    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        // TODO Auto-generated method stub
        parcel.writeString(mLocalPath);
        parcel.writeString(mUri);
    }
    
    public static final Parcelable.Creator<ImageViewerData> CREATOR = new Creator<ImageViewerData>() {
        
        @Override
        public ImageViewerData[] newArray(int arg0) {
            // TODO Auto-generated method stub
            return new ImageViewerData[arg0];
        }
        
        @Override
        public ImageViewerData createFromParcel(Parcel arg0) {
            // TODO Auto-generated method stub
            return new ImageViewerData(arg0);
        }
    };

}
