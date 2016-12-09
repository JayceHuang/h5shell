package com.tencent.doh.plugins;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Build;
import android.view.Display;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.tencent.doh.pluginframework.Config;
import com.tencent.doh.pluginframework.util.MResource;
import com.tencent.doh.pluginframework.webview.WebViewPlugin;
import com.tencent.doh.plugins.ui.MenuItemInfo;
import com.tencent.doh.plugins.ui.UiCallback;
import com.tencent.doh.plugins.ui.UiInterface;
import com.tencent.doh.ui.widget.sliderbar.FontSliderBar;
import com.tencent.smtt.sdk.WebSettings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by benpeng on 2016/8/23.
 */
public class UiPlugin extends WebViewPlugin implements UiCallback {

    private LinkedHashMap<String, MenuItemInfo> mMenuItemsMap = null;
    private String mCallback = null;


    public static final String mDefaultFontJsId = Config.DEFAULT_FONT_JS_ID;
    public static final String mDefaultRefreshJsId = Config.DEFAULT_REFRESH_JS_ID;

    private Dialog mWebFontDlg;

    @Override
    protected boolean handleJsRequest(String url, String pkgName, String method, String... args) {

        if ("setTitle".equalsIgnoreCase(method)) {
            handleSetTitle(args);
        } else if ("showCloseButton".equalsIgnoreCase(method)) {
            handleShowCloseButton(args);
        } else if ("hideCloseButton".equalsIgnoreCase(method)) {
            handleHideCloseButton(args);
        } else if ("setOptionsMenus".equalsIgnoreCase(method)) {
            handleSetOptionsMenus(args);
        } else if ("showOptionsMenu".equalsIgnoreCase(method)) {
            handleShowOptionsMenu(args);
        } else if ("hideOptionsMenu".equalsIgnoreCase(method)) {
            handleHideOptionsMenu(args);
        } else if ("onOptionsMenuClick".equalsIgnoreCase(method)) {
            handleOnOptionsMenuClick(args);
        } else {
            return false;
        }

        return true;
    }

    public boolean implUiInterface() {
        return mRuntime.getActivity() != null && mRuntime.getActivity() instanceof UiInterface;
    }

    private void handleSetTitle(String[] args) {
        JSONObject reqParam;
        String title;

        try {
            reqParam = new JSONObject(args[0]);
            title = reqParam.optString("title");

            if (implUiInterface()) {
                UiInterface uiInterface = (UiInterface) mRuntime.getActivity();
                uiInterface.setTitle(title);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleShowCloseButton(String[] args) {
        try {
            if (implUiInterface()) {
                UiInterface uiInterface = (UiInterface) mRuntime.getActivity();
                uiInterface.showCloseButton();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleHideCloseButton(String[] args) {
        try {
            if (implUiInterface()) {
                UiInterface uiInterface = (UiInterface) mRuntime.getActivity();
                uiInterface.hideCloseButton();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleSetOptionsMenus(String[] args) {
        JSONObject reqParam;
        JSONArray menusArray;
        try {
            reqParam = new JSONObject(args[0]);
            menusArray = reqParam.optJSONArray("menus");
            List<JSONObject> menuItems = jsonArrayToList(menusArray);
            mMenuItemsMap = createMenuItemsMap(menuItems);
            //showOptionsMenu的时候再传过去
//            if (implUiInterface()) {
//                UiInterface uiInterface = (UiInterface) mRuntime.getActivity();
//                uiInterface.setOptionsMenus(menuItemsMap);
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private LinkedHashMap<String, MenuItemInfo> createDefaultMenuItemsMap() {
        LinkedHashMap<String, MenuItemInfo> menuItemsMap = new LinkedHashMap<>();
        MenuItemInfo menuFontItemInfo = new MenuItemInfo(mDefaultFontJsId, Config.getResId(mDefaultFontJsId), null, "设置字体");
        MenuItemInfo menuRefreshItemInfo = new MenuItemInfo(mDefaultRefreshJsId, Config.getResId(mDefaultRefreshJsId), null, "刷新");

        menuItemsMap.put(mDefaultFontJsId, menuFontItemInfo);
        menuItemsMap.put(mDefaultRefreshJsId, menuRefreshItemInfo);

        return menuItemsMap;
    }

    private LinkedHashMap<String, MenuItemInfo> createMenuItemsMap(List<JSONObject> menuItems) {

        if (menuItems == null || menuItems.size() == 0)
            return null;

        LinkedHashMap<String, MenuItemInfo> menuItemsMap = new LinkedHashMap<>();

        JSONObject reqParamItem = null;
        String jsId;
        int localResId;
        String name;
        JSONObject args = null;
        String argsStr = null;
        for (int i = 0; i < menuItems.size(); i++) {
            try {
                argsStr = null;
                args = null;

                reqParamItem = menuItems.get(i);
                jsId = reqParamItem.optString("id");

                localResId = Config.getResId(jsId);

                name = reqParamItem.optString("name");
                if (reqParamItem.has("args")) {
                    args = reqParamItem.optJSONObject("args");
                    if (args != null)
                        argsStr = args.toString();
                }

                MenuItemInfo menuItemInfo = new MenuItemInfo(jsId, localResId, argsStr, name);
                menuItemsMap.put(jsId, menuItemInfo);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return menuItemsMap;
    }

    private List<JSONObject> jsonArrayToList(JSONArray jsonArray) {
        if (jsonArray == null || jsonArray.length() <= 0)
            return null;

        List<JSONObject> retList = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                retList.add(jsonArray.getJSONObject(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return retList;
    }


    private void handleHideOptionsMenu(String[] args) {
        try {
            mMenuItemsMap = null;
            if (implUiInterface()) {
                UiInterface uiInterface = (UiInterface) mRuntime.getActivity();
                uiInterface.setOptionsMenus(null);
                uiInterface.hideOptionsMenu();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleShowOptionsMenu(String[] args) {
        try {
            if (implUiInterface()) {
                UiInterface uiInterface = (UiInterface) mRuntime.getActivity();
                if (mMenuItemsMap == null || mMenuItemsMap.size() == 0)
                    mMenuItemsMap = createDefaultMenuItemsMap();
                uiInterface.setOptionsMenus(mMenuItemsMap);
                uiInterface.showOptionsMenu();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void handleOnOptionsMenuClick(String[] args) {
        JSONObject reqParam;
        try {
            reqParam = new JSONObject(args[0]);
            mCallback = reqParam.optString(KEY_CALLBACK);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @Override
    public void onCallback(MenuItemInfo menuItemInfo) {
        try {
            if (menuItemInfo == null)
                return;

            String jsId = menuItemInfo.getMenuJsId();

            if (interceptDefaultJsId(jsId))
                return;

            if (mCallback == null)
                return;

            JSONObject result = new JSONObject();
            JSONObject outObject = new JSONObject(menuItemInfo.getArgs());
            result.put("id", jsId);
            result.put("args",outObject);
            callJs(mCallback, getResult(result));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private boolean interceptDefaultJsId(String jsId) {
        if (jsId == null)
            return false;

        if (jsId.equals(mDefaultRefreshJsId)) {
            refreshWeb();
            return true;
        } else if (jsId.equals(mDefaultFontJsId)) {
            showWebFontDlg();
            return true;
        }

        return false;
    }

    private void refreshWeb() {
        // NOTE: 3.0 - 4.0.3，使用mRuntime.getWebView().reload()，URL带#会会出现“找不到页面”错误
        // 改用javascript方式刷新
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1){
            mRuntime.getWebView().reload();
        }else {
            String url = mRuntime.getWebView().getUrl();
            mRuntime.getWebView().loadUrl("javascript:window.location.href='" + url + "';");
        }
    }

    private void showWebFontDlg() {
        buildFontDlg();
        if (!mWebFontDlg.isShowing())
            mWebFontDlg.show();
    }

    private void buildFontDlg() {
        int dlgStyle = MResource.getIdByName(mRuntime.getContext(), "style", "Txwp_DlgStyle");
        int dlgViewId = MResource.getIdByName(mRuntime.getContext(), "layout", "txwp_dlg_view");
        int dlgViewHolderId = MResource.getIdByName(mRuntime.getContext(), "id", "view_holder");

        mWebFontDlg = new Dialog(mRuntime.getActivity(), dlgStyle);
        mWebFontDlg.setContentView(dlgViewId);
        FrameLayout layout = (FrameLayout) mWebFontDlg.findViewById(dlgViewHolderId);
        FontSliderBar sliderBar = createFontSliderBar();
        layout.addView(sliderBar, new FrameLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT));

        Window window = mWebFontDlg.getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        WindowManager windowManager = mRuntime.getActivity().getWindowManager();
        Display display = windowManager.getDefaultDisplay(); // 获取屏幕宽、高用
        params.height = (int) (display.getHeight() * 0.15);

        window.setGravity(Gravity.BOTTOM);
        window.setAttributes(params);
        mWebFontDlg.setCancelable(true);
        mWebFontDlg.setCanceledOnTouchOutside(true);
    }

    private FontSliderBar createFontSliderBar() {
        int colorF3 = MResource.getIdByName(mRuntime.getContext(), "color", "txwp_f3f3f3");
        FontSliderBar sliderBar = new FontSliderBar(mRuntime.getContext());
        sliderBar.setTickCount(4).setTickHeight(20).setBarColor(Color.GRAY)
                .setTextColor(Color.DKGRAY).setTextPadding(20).setTextSize(20)
                .setThumbRadius(24).setThumbColorNormal(Color.LTGRAY).setThumbColorPressed(Color.LTGRAY)
                .withAnimation(false).applay();
        sliderBar.setBackgroundResource(colorF3);
        sliderBar.setThumbIndex(Config.getWebTextSizeIndex());
        sliderBar.setOnSliderBarChangeListener(new FontSliderBar.OnSliderBarChangeListener() {
            @Override
            public void onIndexChanged(FontSliderBar rangeBar, int index) {
                WebSettings.TextSize textSize = Config.getWebTextSize(index);
                WebSettings webSettings = mRuntime.getWebView().getSettings();
                webSettings.setTextSize(textSize);
                Config.setWebTextSizeIndex(index);
            }
        });
        return sliderBar;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        if (implUiInterface()) {
            UiInterface uiInterface = (UiInterface) mRuntime.getActivity();
            uiInterface.onOptionsMenuClick(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (implUiInterface()) {
            UiInterface uiInterface = (UiInterface) mRuntime.getActivity();
            uiInterface.onOptionsMenuClick(null);
        }
        mCallback = null;

        if (mWebFontDlg != null)
            mWebFontDlg.dismiss();
        mWebFontDlg = null;
    }
}
