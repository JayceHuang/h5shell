package com.tencent.doh.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tencent.android.tpush.XGPushClickedResult;
import com.tencent.android.tpush.XGPushManager;
import com.tencent.doh.pluginframework.util.LogUtils;
import com.tencent.doh.pluginframework.util.MResource;
import com.tencent.doh.pluginframework.webview.DohWebView;
import com.tencent.doh.pluginframework.webview.WebViewPlugin;
import com.tencent.doh.pluginframework.webview.WebViewPluginContainer;
import com.tencent.doh.plugins.ui.MenuItemInfo;
import com.tencent.doh.plugins.ui.UiCallback;
import com.tencent.doh.plugins.ui.UiInterface;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by trentyang on 2016/8/18.
 * 支持Toolbar
 */
public abstract class DohWebViewActivity extends AppCompatActivity implements UiInterface, WebViewPluginContainer {

    protected Toolbar mToolbar;
    protected TextView mTitle;
    protected DohWebView mWebView;

    protected int mDefaultCloseId = 0;
    protected LinkedHashMap<String, MenuItemInfo> mMenuItemInfoMap = new LinkedHashMap<>();//localResId为索引
    protected UiCallback mUiCallback = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDefaultCloseId = MResource.getIdByName(this, "drawable", "txwp_webview_close_selector");
    }

    @Override
    public void setContentView(int layoutResId) {
        View view = LayoutInflater.from(this).inflate(layoutResId, null);
        setContentView(view);
    }

    @Override
    public void setContentView(View view) {
        LinearLayout contentView = new LinearLayout(this);
        contentView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        contentView.setOrientation(LinearLayout.VERTICAL);
        int toolbar = MResource.getIdByName(this, "layout", "txwp_toolbar");
        mToolbar = (Toolbar) LayoutInflater.from(this).inflate(toolbar, null);
        contentView.addView(mToolbar, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        contentView.addView(view, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        super.setContentView(contentView);
        int titleId = MResource.getIdByName(this, "id", "title");
        mTitle = (TextView) findViewById(titleId);
    }

    public void setNavigationIcon(int resId) {
        if (mToolbar != null) {
            mToolbar.setNavigationIcon(resId);
        }
    }

    public void setNavigationOnClickListener(View.OnClickListener listener) {
        if (mToolbar != null) {
            mToolbar.setNavigationOnClickListener(listener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mWebView != null) {
            mWebView.onResume();
        }
        //这里可以统计所有由信鸽推送引起的打开APP，信鸽SDK内部默认已经统计
        XGPushClickedResult click = XGPushManager.onActivityStarted(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mWebView != null) {
            mWebView.onPause();
        }
        XGPushManager.onActivityStoped(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWebView != null) {
            mWebView.onDestroy();
        }
    }

    @Override
    public void onBackPressed() {
        if (mWebView != null && mWebView.canGoBack()) {
            mWebView.goBack();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public int pluginStartActivityForResult(WebViewPlugin plugin, Intent intent, byte requestCode) {
        if (mWebView != null) {
            return mWebView.startActivityForResult(this, plugin, intent, requestCode);
        }
        return 0;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mWebView != null) {
            mWebView.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        if (mTitle != null) {
            mTitle.setText(title);
        }
    }

    @Override
    public void showCloseButton() {
        if (mToolbar != null) {
            mToolbar.setNavigationIcon(mDefaultCloseId);
            setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
    }

    @Override
    public void hideCloseButton() {
        if (mToolbar != null) {
            mToolbar.setNavigationIcon(null);
        }
    }

    @Override
    public void showOptionsMenu() {
        if (mToolbar == null || mMenuItemInfoMap == null || mMenuItemInfoMap.size() == 0)
            return;
        clearMenu();
        buildMenu();
    }

    private void buildMenu() {
        if (mMenuItemInfoMap == null || mMenuItemInfoMap.size() == 0)
            return;

        SubMenu subMenu = mToolbar.getMenu().addSubMenu(Menu.FIRST, Menu.FIRST, Menu.NONE, null);
        int webviewMoreId = MResource.getIdByName(this, "drawable", "txwp_webview_more");
        subMenu.setIcon(webviewMoreId);
        subMenu.getItem().setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        Iterator iter = mMenuItemInfoMap.entrySet().iterator();
        int index = 0;
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String key = (String) entry.getKey();
            MenuItemInfo val = (MenuItemInfo) entry.getValue();
            if (val != null || val.getTitle() != null) {
                MenuItem menuItem = subMenu.add(Menu.FIRST, key.hashCode(), index++, val.getTitle());
                menuItem.setIcon(val.getResId());
                menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            }
        }
        mToolbar.setOnMenuItemClickListener(onMenuItemClick);
    }

    public void clearMenu() {
        if (mToolbar == null)
            return;
        Menu menu = mToolbar.getMenu();
        menu.clear();
        mToolbar.setOnMenuItemClickListener(null);
    }

    @Override
    public void hideOptionsMenu() {
        clearMenu();
    }

    @Override
    public void setOptionsMenus(LinkedHashMap<String, MenuItemInfo> menus) {
        mMenuItemInfoMap = menus;
    }

    @Override
    public void onOptionsMenuClick(UiCallback callback) {
        mUiCallback = callback;
    }

    private Toolbar.OnMenuItemClickListener onMenuItemClick = new Toolbar.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case Menu.FIRST:
                    LogUtils.d(DohWebViewActivity.class.getSimpleName(), "onMenuItemClick this moreicon");
                    break;
                default:
                    LogUtils.d(DohWebViewActivity.class.getSimpleName(), "onMenuItemClick id:" + menuItem.getItemId());
                    MenuItemInfo menuItemInfo = findItemByKeyHashCode(menuItem.getItemId());
                    if (mUiCallback == null)
                        return true;
                    mUiCallback.onCallback(menuItemInfo);
            }
            return true;
        }
    };

    private MenuItemInfo findItemByKeyHashCode(int hashCode) {
        if (mMenuItemInfoMap == null) {
            return null;
        }

        Iterator iter = mMenuItemInfoMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String key = (String) entry.getKey();
            MenuItemInfo val = (MenuItemInfo) entry.getValue();
            if (key.hashCode() == hashCode)
                return val;
        }
        return null;
    }

}
