package com.copyparty.app;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import androidx.core.app.NotificationCompat;
import org.json.JSONArray;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class WebActivity extends Activity {
    private WebView web;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        web = new WebView(this);
        web.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        root.addView(web);
        setContentView(root);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccessFromFileURLs(true);
        web.getSettings().setAllowUniversalAccessFromFileURLs(true);
        web.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        web.addJavascriptInterface(new NotifyJS(this), "AndroidNotify");
        web.addJavascriptInterface(new LogJS(), "AndroidLog");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) { return false; }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("http://127.0.0.1:5301")) {
                    try {
                        URL u = new URL(url);
                        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                        conn.setConnectTimeout(5000);
                        conn.setReadTimeout(10000);
                        String method = request.getMethod();
                        if ("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method) || "MOVE".equals(method) || "MKCOL".equals(method)) {
                            conn.setRequestMethod("GET");
                        }
                        conn.setRequestProperty("User-Agent", "CopyPartyApp");
                        InputStream is = conn.getInputStream();
                        WebResourceResponse resp = new WebResourceResponse("text/plain; charset=utf-8", "utf-8", is);
                        return resp;
                    } catch (Exception e) {
                        return null;
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }
        });
        web.loadUrl("file:///android_asset/web/index.html");
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    public static class NotifyJS {
        private Context ctx;
        NotifyJS(Context c) { ctx = c; }
        @JavascriptInterface
        public void show(String title, String msg) {
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel ch = new NotificationChannel("upload", "上传通知", NotificationManager.IMPORTANCE_DEFAULT);
                nm.createNotificationChannel(ch);
            }
            NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, "upload")
                .setContentTitle(title).setContentText(msg)
                .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                .setAutoCancel(true);
            nm.notify((int) System.currentTimeMillis(), b.build());
        }
    }

    public static class LogJS {
        @JavascriptInterface
        public String get() {
            return new JSONArray(CopyPartyService.getOpLog()).toString();
        }
    }
}