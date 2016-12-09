package com.tencent.photoview;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;


import com.viewpagerindicator.CirclePageIndicator;
import java.util.List;


public class ImageViewerActivity extends Activity {

    private String TAG = "ImageViewerActivity";
    private ViewPager mViewPager;
    private CirclePageIndicator mIndicator;
    private ImageViewerAdapter mAdapter;
    private int mCurrentIndex = 0;

    // 对应点击的图片的下标
    public final static String INTENT_EX_CURRENT_INDEX = "currentIndex";

    //新结构ImageViewData 相应了解页面需求把对应方式做得更通用一点
    public final static String INTENT_EX_IMAGE_DATA = "image_data";
    public List<String> mListImageData;

    // 保存浏览图片所用
    public final static int SAVE_BITMAP = 0;
    public final static int SAVE_NOTICE_DATA_INDEX = 1;

    public final static int SAVE_BITMAP_SUCESS = 0;
    public final static int SAVE_BITMAP_FAIL = 1;

    public static String IMG_POSTFIX = ".jpg";

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case SAVE_BITMAP_SUCESS:

                    Toast.makeText(getApplicationContext(), MResource.getIdByName(getApplicationContext(),"string","txwp_imageviewer_sava_bitmap_sucess"), Toast.LENGTH_LONG)
                            .show();
                    break;
                case SAVE_BITMAP_FAIL:
                    Toast.makeText(getApplicationContext(), MResource.getIdByName(getApplicationContext(),"string","txwp_imageviewer_sava_bitmap_fail"), Toast.LENGTH_LONG).show();
                default:
                    break;
            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ImageLoaderInit.init(this);
        initData();
        setContentView(MResource.getIdByName(getApplicationContext(),"layout","txwp_activity_image_viewer"));
        initView();
    }

    public void initView() {
        mViewPager = (ViewPager) findViewById(MResource.getIdByName(getApplicationContext(),"id","image_viewer_viewpager"));
        mIndicator = (CirclePageIndicator) findViewById(MResource.getIdByName(getApplicationContext(),"id","image_viewer_indicator"));

        mAdapter = new ImageViewerAdapter(mListImageData, this);
        mViewPager.setAdapter(mAdapter);
        mIndicator.setViewPager(mViewPager);
        mIndicator.setSnap(true);
        mIndicator.setCurrentItem(mCurrentIndex);

        mAdapter.setOnItemClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                finish();
            }
        });
    }

    public void initData() {
        parseIntent();
    }

    public void parseIntent() {
        Bundle bundle = getIntent().getExtras();
        mCurrentIndex = bundle.getInt(INTENT_EX_CURRENT_INDEX, 0);
        mListImageData = bundle.getStringArrayList(INTENT_EX_IMAGE_DATA);
    }

    public void onBackPress() {
        finish();
    }

}
