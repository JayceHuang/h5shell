package com.tencent.doh.pluginframework.webview;

import com.tencent.doh.pluginframework.util.LogUtils;
import com.tencent.doh.plugins.AppPlugin;
import com.tencent.doh.plugins.AudioPlugin;
import com.tencent.doh.plugins.DevicePlugin;
import com.tencent.doh.plugins.ImagePlugin;
import com.tencent.doh.plugins.SharePlugin;
import com.tencent.doh.plugins.LocationPlugin;
import com.tencent.doh.plugins.UiPlugin;
import com.tencent.doh.plugins.WxPlugin;
import com.tencent.doh.plugins.XgPushPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SimpleTimeZone;

public class WebViewPluginConfig {
    public static final String TAG = WebViewPluginConfig.class.getSimpleName();
	private  static List<PluginInfo> list = new ArrayList<>();
    private  static Map<String, PluginInfo> map = new HashMap<String, PluginInfo>(); //新的插件列表
    static {


        //release

//        list = new PluginInfo[]{
//                new PluginInfo(MediaApiPlugin.class, "media", "mqq.media.* API", "1.0"),
//                new PluginInfo(AppApiPlugin.class, "app", "mqq.app.* API", "1.0"),
//                new PluginInfo(EventPlugin.class, "event", "mqq.event.* API", "1.0")
//        };



        //sdk的plugin
        PluginInfo[] sdkPluginInfoList = new PluginInfo[]{
                //new PluginInfo(EventPlugin.class, "event", "mqq.event.* API", "1.0"),
                new PluginInfo(DevicePlugin.class, "device", "mqq.device.* API", "1.0"),
                //new PluginInfo(SensorPlugin.class, "sensor", "mqq.sensor.* API", "1.0"),
                new PluginInfo(ImagePlugin.class, "image", "tencent image api", "1.0"),
                new PluginInfo(SharePlugin.class,"share", "mqq.share.* API", "1.0"),
                new PluginInfo(XgPushPlugin.class, "xgpush", "XG push API", "1.0"),
                new PluginInfo(WxPlugin.class, "wx", "tencent wx api", "1.0"),
                new PluginInfo(AudioPlugin.class,"audio","tencent audio api","1.0"),
                new PluginInfo(AppPlugin.class,"app","tencent app api","1.0"),
                new PluginInfo(LocationPlugin.class,"location","tencent location api","1.0"),
                new PluginInfo(UiPlugin.class,"ui","tencent ui api","1.0")

        };

        addPluginInfo(sdkPluginInfoList);
    }

    public static void addPluginInfo(PluginInfo[] pluginInfoList){
        if (pluginInfoList == null)
            return;
        int listLen = list.size();
        for (int i = 0, len = pluginInfoList.length; i < len; ++i){
            PluginInfo p = pluginInfoList[i];
            if (!checkNamespaceValid(p)){
                LogUtils.e(TAG,"addPluginInfo error! the namespace \"" + p.namespace + "\" is already exist!");
                continue;
            }
            p.index = i + listLen + 1;
            list.add(p);
            if (p.namespace != null && p.namespace.length() > 0) {
                map.put(p.namespace, p);
            }

        }
    }

    public static List<PluginInfo> getPluginList() {
        return list;
    }

    public static Map<String, PluginInfo> getPluginMap() {
        return map;
    }

    private static boolean checkNamespaceValid(PluginInfo pluginInfo){
        if (pluginInfo == null)
            return false;

        PluginInfo p = pluginInfo;
        if (p.namespace != null && p.namespace.length() > 0){
            if (map.containsKey(p.namespace)){
                return false;
            }
        }

        return true;
    }
}
