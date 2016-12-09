package com.tencent.doh.plugins.ui;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by benpeng on 2016/8/23.
 */
public interface UiInterface {

    //设置标题
    void setTitle(CharSequence title);

    //显示关闭按钮
    void showCloseButton();

    //隐藏关闭按钮
    void hideCloseButton();

    //显示菜单，默认包含字体和刷新菜单
    void showOptionsMenu();

    //隐藏菜单
    void hideOptionsMenu();

    //设置菜单列表
    void setOptionsMenus(LinkedHashMap<String,MenuItemInfo> menus);

    //菜单点击事件
    void onOptionsMenuClick(UiCallback callback);

}
