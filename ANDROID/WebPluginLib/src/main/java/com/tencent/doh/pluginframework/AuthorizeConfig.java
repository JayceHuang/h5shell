package com.tencent.doh.pluginframework;

import android.content.Context;

import com.tencent.doh.pluginframework.util.Utils;

/**
 * User: azraellong
 * Date: 13-8-8
 * Time: 下午5:02
 */
public abstract class AuthorizeConfig {
    final static String TAG = AuthorizeConfig.class.getSimpleName();

    private static Class<? extends AuthorizeConfig> sClass;

    protected static AuthorizeConfig sConfig;
    public static AuthorizeConfig getInstance(Context context) {
        if (sConfig == null) {
            synchronized (AuthorizeConfig.class) {
                if (sConfig == null) {
                    assert sClass != null : "AuthorizeConfig.setClass must call first";
                    try {
                        sConfig = (AuthorizeConfig) Utils.getConstructor(sClass, Context.class).newInstance(context);
                    } catch (Exception e) {
                        throw new RuntimeException("fail to new instance");
                    }
                }
            }
        }
        return sConfig;
    }

    public static void setClass(Class<? extends AuthorizeConfig> cz) {
        sClass = cz;
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
		return true;
	}
	
	public boolean hasSchemeRight(String url, String scheme) {
		return true;
    }

	public String getExtraString(String string, Object object) {
		// TODO Auto-generated method stub
		return null;
	}
}

