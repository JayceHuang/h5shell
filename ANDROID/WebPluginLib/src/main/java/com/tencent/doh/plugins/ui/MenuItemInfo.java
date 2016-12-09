package com.tencent.doh.plugins.ui;

/**
 * Created by benpeng on 2016/8/23.
 */
public class MenuItemInfo {

    private String menuJsId;

    private int resId;

    private String args;

    private String title;

    public MenuItemInfo(String _menuJsId,int _resId,String _args,String _title){
        menuJsId = _menuJsId;
        resId = _resId;
        args = _args;
        title = _title;
    }

    public String getMenuJsId() {
        return menuJsId;
    }

    public void setMenuJsId(String menuJsId) {
        this.menuJsId = menuJsId;
    }

    public int getResId() {
        return resId;
    }

    public void setResId(int resId) {
        this.resId = resId;
    }

    public String getArgs() {
        return args;
    }

    public void setArgs(String args) {
        this.args = args;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
