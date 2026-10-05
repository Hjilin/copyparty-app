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
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import androidx.core.app.NotificationCompat;
import org.json.JSONArray;

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
