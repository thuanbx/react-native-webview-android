package com.burnweb.rnwebview;

import android.annotation.SuppressLint;

import android.net.Uri;
import android.graphics.Bitmap;
import android.os.Build;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.content.Intent;
import android.content.pm.PackageManager;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.common.SystemClock;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.uimanager.events.EventDispatcher;
import android.widget.Toast;
import android.content.Context;

class RNWebView extends WebView implements LifecycleEventListener {

    private final EventDispatcher mEventDispatcher;
    private final RNWebViewManager mViewManager;

    private String charset = "UTF-8";
    private String baseUrl = "file:///";
    private String injectedJavaScript = null;
    private boolean allowUrlRedirect = false;

    private String currentUrl = "";
    private String shouldOverrideUrlLoadingUrl = "";
    

    protected class EventWebClient extends WebViewClient {
        public boolean shouldOverrideUrlLoading(WebView view, String url){
            int navigationType = 0;
             Intent intent;

            if(url.contains("market")){
                String playStoreMarketPrefix = "market://details?id=";
                getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(playStoreMarketPrefix + "air.com.adobe.connectpro")));
                return true;
            }
            if (url.contains("intent")) {
                if(!isPackageInstalled("air.com.adobe.connectpro",getContext())){
                    Toast.makeText(getContext(), "Bạn chưa cai đặt ứng dung adobe connect vui lòng click vào nút  'Get Adobe Connect Mobile' ", Toast.LENGTH_LONG).show();                   
                    // view.loadDataWithBaseURL(shouldOverrideUrlLoadingUrl, "<html></html>", "text/html", "UTF-8", null);
                    return true;
                }

                intent = new Intent(Intent.ACTION_VIEW);
                String[] split = url.split("#");
                String data = split[0].replaceFirst("intent", "connectpro");
                intent.setData(Uri.parse(data));
                getContext().startActivity(intent);
                return true;
            }

            if (currentUrl.equals(url) || url.equals("about:blank")) { // for regular .reload() and html reload.
                navigationType = 3;
            }

            shouldOverrideUrlLoadingUrl = url;
            mEventDispatcher.dispatchEvent(new ShouldOverrideUrlLoadingEvent(getId(), SystemClock.nanoTime(), url, navigationType));

            return true;
        }

        public void onPageFinished(WebView view, String url) {

                if (url.contains("intent")) {
                    if(!isPackageInstalled("air.com.adobe.connectpro",getContext())){
                        // Toast.makeText(getContext(), "Chua cai day app adobe connect", Toast.LENGTH_LONG).show();                   
                        view.loadDataWithBaseURL(shouldOverrideUrlLoadingUrl, "<html></html>", "text/html", "UTF-8", null);
                        return;
                    }
                }
            mEventDispatcher.dispatchEvent(new NavigationStateChangeEvent(getId(), SystemClock.nanoTime(), view.getTitle(), false, url, view.canGoBack(), view.canGoForward()));

            currentUrl = url;

            if(RNWebView.this.getInjectedJavaScript() != null) {
                view.loadUrl("javascript:(function() {\n" + RNWebView.this.getInjectedJavaScript() + ";\n})();");
            }
        }

        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            mEventDispatcher.dispatchEvent(new NavigationStateChangeEvent(getId(), SystemClock.nanoTime(), view.getTitle(), true, url, view.canGoBack(), view.canGoForward()));
        }

        private boolean isPackageInstalled(String packagename, Context context) {
            try {
                PackageManager packageManager = context.getPackageManager();
                packageManager.getPackageInfo(packagename, 0);
                return true;
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        }
    }

    protected class CustomWebChromeClient extends WebChromeClient {
        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            getModule().showAlert(url, message, result);
            return true;
        }

        // For Android 4.1+
        @SuppressWarnings("unused")
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
            getModule().startFileChooserIntent(uploadMsg, acceptType);
        }

        // For Android 5.0+
        @SuppressLint("NewApi")
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            return getModule().startFileChooserIntent(filePathCallback, fileChooserParams.createIntent());
        }
    }

    protected class GeoWebChromeClient extends CustomWebChromeClient {
        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            callback.invoke(origin, true, false);
        }
    }

    public RNWebView(RNWebViewManager viewManager, ThemedReactContext reactContext) {
        super(reactContext);

        mViewManager = viewManager;
        mEventDispatcher = reactContext.getNativeModule(UIManagerModule.class).getEventDispatcher();

        this.getSettings().setJavaScriptEnabled(true);
        this.getSettings().setBuiltInZoomControls(false);
        this.getSettings().setDomStorageEnabled(true);
        this.getSettings().setGeolocationEnabled(false);
        this.getSettings().setPluginState(WebSettings.PluginState.ON);
        this.getSettings().setAllowFileAccess(true);
        this.getSettings().setAllowFileAccessFromFileURLs(true);
        this.getSettings().setAllowUniversalAccessFromFileURLs(true);
        this.getSettings().setLoadsImagesAutomatically(true);
        this.getSettings().setBlockNetworkImage(false);
        this.getSettings().setBlockNetworkLoads(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        this.setWebViewClient(new EventWebClient());
        this.setWebChromeClient(getCustomClient());

        this.addJavascriptInterface(RNWebView.this, "webView");
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getCharset() {
        return this.charset;
    }

    public void setAllowUrlRedirect(boolean a) {
        this.allowUrlRedirect = a;
    }

    public boolean getAllowUrlRedirect() {
        return this.allowUrlRedirect;
    }

    public void setInjectedJavaScript(String injectedJavaScript) {
        this.injectedJavaScript = injectedJavaScript;
    }

    public String getInjectedJavaScript() {
        return this.injectedJavaScript;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void shouldOverrideWithResult(RNWebView view, ReadableArray args) {
        if (!args.getBoolean(0)) {
            view.loadUrl(shouldOverrideUrlLoadingUrl);
        }
    }

    public String getBaseUrl() {
        return this.baseUrl;
    }

    public CustomWebChromeClient getCustomClient() {
        return new CustomWebChromeClient();
    }

    public GeoWebChromeClient getGeoClient() {
        return new GeoWebChromeClient();
    }

    public RNWebViewModule getModule() {
        return mViewManager.getPackage().getModule();
    }

    @Override
    public void onHostResume() {

    }

    @Override
    public void onHostPause() {

    }

    @Override
    public void onHostDestroy() {
        destroy();
    }

    @Override
    public void onDetachedFromWindow() {
        this.loadDataWithBaseURL(this.getBaseUrl(), "<html></html>", "text/html", this.getCharset(), null);
        super.onDetachedFromWindow();
    }

    @JavascriptInterface
     public void postMessage(String jsParamaters) {
        mEventDispatcher.dispatchEvent(new MessageEvent(getId(), jsParamaters));
    }
}
