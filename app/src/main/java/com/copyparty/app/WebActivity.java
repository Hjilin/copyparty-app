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
import android.webkit.WebChromeClient;
import android.webkit.ValueCallback;
import android.content.Intent;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import androidx.core.app.NotificationCompat;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class WebActivity extends Activity {
    private WebView web;
    private ValueCallback<Uri[]> mFilePathCallback;

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
        web.addJavascriptInterface(new HttpJS(), "AndroidHttp");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) { return false; }
        });
        web.setWebChromeClient(new WebChromeClient() {
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                mFilePathCallback = filePathCallback;
                Intent i = fileChooserParams.createIntent();
                try { startActivityForResult(Intent.createChooser(i, "选择文件"), 1); }
                catch (Exception e) { mFilePathCallback = null; return false; }
            return true;
            }
        });
        web.loadUrl("file:///android_asset/web/index.html");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && mFilePathCallback != null) {
            Uri[] results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            if (results != null && results.length > 0) {
                Uri uri = results[0];
                String path = copyUriToFile(uri);
                String name = getFileName(uri);
                mFilePathCallback.onReceiveValue(results);
                mFilePathCallback = null;
                web.evaluateJavascript("window.__onFilePicked && __onFilePicked('" + path + "','" + name + "')", null);
            } else {
                mFilePathCallback.onReceiveValue(null);
                mFilePathCallback = null;
            }
        }
    }

    private String copyUriToFile(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            File out = new File(getFilesDir(), "upload_" + System.currentTimeMillis() + ".tmp");
            FileOutputStream fos = new FileOutputStream(out);
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) fos.write(buf, 0, n);
            fos.close(); is.close();
            return out.getAbsolutePath();
        } catch (Exception e) { return null; }
    }

    private String getFileName(Uri uri) {
        String result = "file";
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) { cursor.moveToFirst(); int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (idx >= 0) result = cursor.getString(idx); cursor.close(); }
        return result;
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

    public static class HttpJS {
        @JavascriptInterface
        public String request(String method, String url) {
            return doRequest(method, url, null, null);
        }
        @JavascriptInterface
        public String requestWithBody(String method, String url, String body) {
            return doRequest(method, url, body, null);
        }
        @JavascriptInterface
        public String requestWithHeader(String method, String url, String headerName, String headerValue) {
            return doRequest(method, url, null, new String[]{headerName, headerValue});
        }

        private String doRequest(String method, String url, String body, String[] extraHeader) {
            try {
                URL u = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                conn.setRequestMethod(method);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("User-Agent", "CopyPartyApp");
                conn.setRequestProperty("Accept", "text/plain,*/*");
                if (extraHeader != null) {
                    conn.setRequestProperty(extraHeader[0], extraHeader[1]);
                }
                if (body != null) {
                    conn.setDoOutput(true);
                    byte[] b = body.getBytes("UTF-8");
                    OutputStream os = conn.getOutputStream();
                    os.write(b);
                    os.flush();
                    os.close();
                }
                int code = conn.getResponseCode();
                InputStream is = (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream();
                String resp = "";
                if (is != null) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line).append("\n");
                    br.close();
                    resp = sb.toString();
                }
                JSONObject r = new JSONObject();
                r.put("code", code);
                r.put("body", resp);
                return r.toString();
            } catch (Exception e) {
                try {
                    JSONObject r = new JSONObject();
                    r.put("code", 0);
                    r.put("body", e.getMessage());
                    return r.toString();
                } catch (Exception ignored) { return "{\"code\":0,\"body\":\"err\"}"; }
            }
        }

        @JavascriptInterface
        public String uploadFile(String url, String filePath, String fileName) {
            try {
                String boundary = "----CopyParty" + System.currentTimeMillis();
                URL u = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(60000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                java.io.File file = new java.io.File(filePath);
                if (!file.exists()) return "{\"code\":0,\"body\":\"file not found: " + filePath + "\"}";
                OutputStream os = conn.getOutputStream();
                String head = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\nContent-Type: application/octet-stream\r\n\r\n";
                os.write(head.getBytes("UTF-8"));
                java.io.FileInputStream fis = new java.io.FileInputStream(file);
                byte[] buf = new byte[8192];
                int n;
                while ((n = fis.read(buf)) != -1) os.write(buf, 0, n);
                fis.close();
                os.write(("\r\n--" + boundary + "--\r\n").getBytes("UTF-8"));
                os.flush();
                os.close();
                int code = conn.getResponseCode();
                JSONObject r = new JSONObject();
                r.put("code", code);
                r.put("body", "upload done");
                return r.toString();
            } catch (Exception e) {
                try {
                    JSONObject r = new JSONObject();
                    r.put("code", 0);
                    r.put("body", e.getMessage());
                    return r.toString();
                } catch (Exception ignored) { return "{\"code\":0,\"body\":\"upload err\"}"; }
            }
        }
    }
}
