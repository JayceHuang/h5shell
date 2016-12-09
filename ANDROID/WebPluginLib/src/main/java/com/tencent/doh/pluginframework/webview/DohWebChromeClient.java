package com.tencent.doh.pluginframework.webview;

import android.text.TextUtils;
import com.tencent.doh.pluginframework.util.LogUtils;
import com.tencent.smtt.export.external.interfaces.ConsoleMessage;
import com.tencent.smtt.export.external.interfaces.JsPromptResult;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebView;

public class DohWebChromeClient extends WebChromeClient {

	private static final String TAG = DohWebChromeClient.class.getSimpleName();

	private WebViewPluginEngine mPluginEngine;

	public DohWebChromeClient() {
	}

	/* package */ void setPluginEngine(WebViewPluginEngine pluginEngine) {
		mPluginEngine = pluginEngine;
	}

	@Override
	public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
		super.onConsoleMessage(consoleMessage);
		// ping 得通的话，使用console.log来处理
		String msg = consoleMessage.message();
		if(TextUtils.equals("pingJsbridge://",msg)){
			if(mPluginEngine != null && mPluginEngine.getRuntime() != null){
				WebView webView = mPluginEngine.getRuntime().getWebView();
				if(webView != null){
					String script = "javascript:window.{ACTION}_AVAILABLE=true;".replace("{ACTION}","CONSOLE");
					webView.loadUrl(script);
					LogUtils.d(TAG+"pingJsbridge", " !!!!! console ok !!!!! ");
				}
				return true;
			}
		}

		LogUtils.d(TAG+"onConsoleMessage", " by onConsoleMessage : " + msg);
		if (mPluginEngine == null) {
			LogUtils.d(TAG+"onConsoleMessage", "mPluginEngine is null");
		} else if (mPluginEngine.canHandleJsRequest(msg)) {
			return true;
		} else if (mPluginEngine.handleRequest(msg)) {
			return true;
		}

		return super.onConsoleMessage(consoleMessage);
	}

	@Override
	public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
		if(TextUtils.equals("pingJsbridge://",defaultValue)){
			if(mPluginEngine !=null && mPluginEngine.getRuntime() != null){
				WebView webView = mPluginEngine.getRuntime().getWebView();
				if(webView != null){
					String script = "javascript:window.{ACTION}_AVAILABLE=true;".replace("{ACTION}","PROMPT");
					webView.loadUrl(script);
					LogUtils.d(TAG+"pingJsbridge", " !!!!! prompt ok !!!!! ");
				}
				result.confirm();
				return true;
			}
		}

		LogUtils.d(TAG+"onJsPrompt", " by onJsPrompt : " + defaultValue);
		if (mPluginEngine == null) {
			LogUtils.d(TAG+"onJsPrompt", "mPluginEngine is null");
		} else if (mPluginEngine.canHandleJsRequest(defaultValue)) {
			result.confirm();
			return true;
		} else if (mPluginEngine.handleRequest(defaultValue)) {
			result.confirm();
			return true;
		}

		return super.onJsPrompt(view, url, message, defaultValue, result);
	}
}
