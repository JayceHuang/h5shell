package com.tencent.doh.pluginframework;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import com.tencent.doh.pluginframework.util.LogUtils;
import com.tencent.doh.pluginframework.util.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * User: azraellong
 * Date: 13-8-8
 * Time: 下午5:02
 */
public class DefaultAuthorizeConfig extends AuthorizeConfig {
    final static String TAG = DefaultAuthorizeConfig.class.getSimpleName();

    Context mContext;
    SharedPreferences mPref;

	private String LOCAL_CONFIG_SCHEMES;

	private Map<?, ?> DEFAULT_SCHEMES_ACCESS_JSON_STRING;

	private JSONObject mSchemesConfig;

	private JSONObject mCmdConfig;

	private String LOCAL_CONFIG_CMD;

	private String DEFAULT_CMD_RIGHT_JSON_STRING = "{\"*\":[\"*\"]}";

    public DefaultAuthorizeConfig(Context context)
    {
		mContext = context;
        mPref = context.getSharedPreferences("WebViewAuthorizeConfig", Context.MODE_MULTI_PROCESS);
	}

	/**
	 *
	 * @param url
	 * @param cmdName
	 * @return
	 *
	 * {
	"*.qq.com": ["*"],
	"*.tencent.com": ["*"],
	"www.51buy.com": ["api.test"],
	"*.paipai.com": ["api.test"]
	}
	 */
	public boolean hasCommandRight(String url, String cmdName) {
        if (url == null) {
            return false;
        }
        Uri uri = Uri.parse(url);
        String curScheme = uri.getScheme();
        if ("file".equals(curScheme)) {
            // 本地文件给予完全权限
            return true;
        } else if (!"http".equals(curScheme) && !"https".equals(curScheme)) {
            // 非http协议, 一概不信任
            return false;
        }
        String domain = uri.getHost();
        JSONObject config = mCmdConfig;
        if (config == null) {
            String configStr = Config.getAuthorizeRule();
            //加载js白名单
            if (configStr != null) {
                try {
                    config = new JSONObject(configStr);
                } catch (JSONException e) {
                    LogUtils.e(TAG,e.getLocalizedMessage());
                }
            }
            if (config == null) {
                try {
                    config = new JSONObject(DEFAULT_CMD_RIGHT_JSON_STRING);
                } catch (JSONException e) {
                    //won't happen
                }
            }
            mCmdConfig = config;
        }
		JSONArray domainNames = config.names();
		if (domainNames == null){
			return false;
		}
		for (int i = 0, len = domainNames.length(); i < len; i++) {
			String name = domainNames.optString(i);
			if (!Utils.isDomainMatch(name, domain)) {
				continue;
			}
			JSONArray apiList = config.optJSONArray(name);
			if (apiList == null){
				continue;
			}
			for (int j = 0, alen = apiList.length(); j < alen; j++) {
				name = apiList.optString(j);
				if (Utils.isDomainMatch(name, cmdName)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean hasSchemeRight(String url, String scheme) {
        if (url == null) { //新开的webview，url为null，此时应该允许http、https、file等协议
            url = "http://localhost/";
        }
        Uri uri = Uri.parse(url);
        String curScheme = uri.getScheme();
        if ("file".equals(curScheme)) {
            // 本地文件给予完全权限
            return true;
        } else if (!"http".equals(curScheme) && !"https".equals(curScheme)) {
            // 非http协议, 一概不信任
            return false;
        }
        String domain = uri.getHost();
        JSONObject config = mSchemesConfig;
        if (config == null) {
            //schemes
            String schemesStr = mPref.getString(LOCAL_CONFIG_SCHEMES, null); //派发给webview插件处理的scheme的白名单
            if (schemesStr != null) {
                try {
                    config = new JSONObject(schemesStr);
                } catch (JSONException e) {
                }
            }
            if (config == null) {
                config = new JSONObject(DEFAULT_SCHEMES_ACCESS_JSON_STRING);
            }
            mSchemesConfig = config;
        }
        JSONArray domainNames = config.names();
        if (domainNames == null){
            return false;
        }
        for (int i = 0, len = domainNames.length(); i < len; i++) {
            String name = domainNames.optString(i);
            if (!Utils.isDomainMatch(name, domain)) {
                continue;
            }
            JSONArray schemeList = config.optJSONArray(name);
            if (schemeList == null){
                continue;
            }
            for (int j = 0, slen = schemeList.length(); j < slen; j++) {
                name = schemeList.optString(j);
                if (Utils.isDomainMatch(name, scheme)) {
                    return true;
                }
            }
        }
        return false;
    }

}

