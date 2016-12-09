package com.tencent.doh.ui.activity;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.doh.pluginframework.util.ApkUtils;
import com.tencent.doh.pluginframework.util.LocationUtils;
import com.tencent.doh.pluginframework.util.MResource;
import com.tencent.doh.pluginframework.util.NetworkUtils;
import com.tencent.doh.pluginframework.util.Utils;
import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.geolocation.TencentLocationListener;
import com.tencent.map.geolocation.TencentLocationManager;
import com.tencent.map.geolocation.TencentLocationRequest;
import com.tencent.mapsdk.raster.model.BitmapDescriptorFactory;
import com.tencent.mapsdk.raster.model.Circle;
import com.tencent.mapsdk.raster.model.CircleOptions;
import com.tencent.mapsdk.raster.model.LatLng;
import com.tencent.mapsdk.raster.model.Marker;
import com.tencent.mapsdk.raster.model.MarkerOptions;
import com.tencent.tencentmap.mapsdk.map.MapActivity;
import com.tencent.tencentmap.mapsdk.map.MapView;
import com.tencent.tencentmap.mapsdk.map.TencentMap;

import static com.tencent.upload.common.Global.context;

/**
 * Created by shibinhuang on 2016/8/24.
 */
public class LocationActivity extends MapActivity  implements
        TencentLocationListener {

    public final static String LONGITUDE = "longitude"; //经度
    public final static String LATITUDE = "latitude"; //纬度
    public final static String SCALE = "scale"; //缩放级别
    public final static String NAME = "name"; //位置名
    public final static String ADDRESS = "address"; //地址详细名

    private final static String PACKAGE_BAIDU = "com.baidu.BaiduMap";
    private final static String PACKAGE_GAODE= "com.autonavi.minimap";
    private final static String PACKAGE_TENCENT = "com.tencent.map";
    private final static String PACKAGE_GOOGLE = "com.google.android.maps";

    private TencentLocationManager mLocationManager;

    private double mLongitude = 0;//经度
    private double mLatitude = 0;//纬度
    private String mAdress  ;//地址
    private String mName  ;//超链接
    private TextView mAdressText  ;
    private TextView mNameText  ;
    private int mSize = 0;//大小维度

    private TencentMap mTencentMap;
    private MapView mMapView;

    private TencentLocation mLocation;

    private Circle mAccuracyCircle;
    private Marker mLocationMarker;

    private ImageButton mShowLocation;
    private ImageButton mShowUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(MResource.getIdByName(getApplicationContext(),"layout","txwp_activity_location"));
        init();
        initData();
        bindListener();
    }

    protected void init() {
        mMapView = (MapView) findViewById(MResource.getIdByName(getApplicationContext(),"id","mapviewOverlay"));
        mTencentMap = mMapView.getMap();
        mTencentMap.setOnMarkerClickListener(new TencentMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                return false;
            }
        });
        mShowLocation = (ImageButton) findViewById(MResource.getIdByName(getApplicationContext(),"id","btn_show_location"));
        mShowUrl = (ImageButton) findViewById(MResource.getIdByName(getApplicationContext(),"id","btn_show_url"));
        mAdressText = (TextView) findViewById(MResource.getIdByName(getApplicationContext(),"id","tv_adress"));
        mNameText = (TextView) findViewById(MResource.getIdByName(getApplicationContext(),"id","tv_name"));
        mLocationManager = TencentLocationManager.getInstance(this);
        mLocationManager.setCoordinateType(TencentLocationManager.COORDINATE_TYPE_GCJ02);
    }

    private  void initData(){
        mLongitude = getIntent().getDoubleExtra(LONGITUDE, 0);
        mLatitude = getIntent().getDoubleExtra(LATITUDE, 0);
        mSize = getIntent().getIntExtra(SCALE, 10);
        mAdress = getIntent().getStringExtra(ADDRESS);
        mName = getIntent().getStringExtra(NAME);
        LatLng latLngLocation = new LatLng(mLatitude, mLongitude);
        mAdressText.setText(mAdress);
        mNameText.setText(mName);
        int defaultResId = MResource.getIdByName(this, "drawable", "txwp_location_target");
        MarkerOptions mMarkerOptions = new MarkerOptions().
                position(latLngLocation).
                icon(BitmapDescriptorFactory.fromResource(defaultResId));
        mTencentMap.addMarker(mMarkerOptions);

        //设置缩放级别
        mTencentMap.setZoom(mSize);
        //设置地图中心点
        mTencentMap.setCenter(latLngLocation);

    }

    @Override
    protected void onResume() {
        super.onResume();
       startLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocation();
    }

    public void getMyLocation( ) {
        if (mLocation != null) {
            mMapView.getController().animateTo(LocationUtils.of(mLocation));
        }
    }

    @Override
    public void onLocationChanged(TencentLocation location, int error,
                                  String reason) {
        if (error == TencentLocation.ERROR_OK) {
            mLocation = location;

            LatLng latLngLocation = new LatLng(location.getLatitude(), location.getLongitude());

            // 更新 location Marker
            if (mLocationMarker == null) {
                mLocationMarker = mTencentMap.addMarker(
                        new MarkerOptions().
                                position(latLngLocation) .
                                anchor(0.5f, 0.5f));
            } else {
                mLocationMarker.setPosition(latLngLocation);
            }
            if (mAccuracyCircle == null) {
                mAccuracyCircle = mTencentMap.addCircle(new CircleOptions().
                        center(latLngLocation).
                        radius(location.getAccuracy()).
                        fillColor(0x884433ff).
                        strokeColor(0xaa1122ee).
                        strokeWidth(1));
            } else {
                mAccuracyCircle.setCenter(latLngLocation);
                mAccuracyCircle.setRadius(location.getAccuracy());
            }
        }
    }

    @Override
    public void onStatusUpdate(String name, int status, String desc) {
    }

    private void startLocation() {
        TencentLocationRequest request = TencentLocationRequest.create();
        request.setInterval(5000);
        mLocationManager.requestLocationUpdates(request, this);
    }

    private void stopLocation() {
        mLocationManager.removeUpdates(this);
    }

    protected void bindListener() {
        mShowLocation.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(!NetworkUtils.isNetworkAvailable(getApplicationContext())){
                    Toast.makeText(getApplicationContext(),"无网络",Toast.LENGTH_SHORT).show();
                    return;
                }
                getMyLocation();
            }
        });
        mShowUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!NetworkUtils.isNetworkAvailable(getApplicationContext())){
                    Toast.makeText(getApplicationContext(),"无网络",Toast.LENGTH_SHORT).show();
                    return;
                }
                dispatchMapReq();
            }
        });
    }

    private void dispatchMapReq() {
        try {
            String url;
            if(ApkUtils.isAppInstalledFromSystem(PACKAGE_TENCENT)){
                url =  "qqmap://map/routeplan?type=drive&from="+mLocation.getName()+"&fromcoord="+mLocation.getLatitude()+","+ mLocation.getLongitude()+"&to="+mName+"&tocoord="+mLongitude+","+mLatitude;
                Intent intent = Intent.getIntent(url);
                startActivity(intent);
            }
           else if(ApkUtils.isAppInstalledFromSystem(PACKAGE_BAIDU)) {
                url = "intent://map/direction?origin=latlng:+" + mLocation.getLatitude() + "," + mLocation.getLongitude() + "| name:"+mLocation.getName()+"&destination=" + mName + "&mode=driving&src=yourCompanyName|yourAppName#Intent;scheme=bdapp;package=com.baidu.BaiduMap;end";
                Intent intent = Intent.getIntent(url);
                startActivity(intent);
            }else   if(ApkUtils.isAppInstalledFromSystem(PACKAGE_GAODE)){
                url =     "androidamap://viewMap?sourceApplication= "+mLocation.getName()+ "&poiname= "+mName+ "&lat="+mLocation.getLatitude()+"&"+ mLocation.getLongitude()+"&dev=0";
                Intent intent = Intent.getIntent(url);
                startActivity(intent);
            }
            else {
                Toast.makeText(LocationActivity.this,"请您选择手机里的地图APP或者浏览器",Toast.LENGTH_SHORT).show();
                //android系统都没安装时默认使用google地图
                url =  "http://ditu.google.cn/maps?f=d&source=s_d&saddr="
                        + mLatitude
                        + ","
                        + mLongitude
                        + "&daddr="
                        + mLocation.getLatitude()
                        + ","
                        + mLocation.getLongitude() + "&hl=zh";
                Intent intent = Intent.getIntent(url);
                startActivity(intent);
            }
        }catch (Exception e){
            Toast.makeText(LocationActivity.this,"路径规划错误,请联系管理员",Toast.LENGTH_SHORT).show();
        }




    }
}