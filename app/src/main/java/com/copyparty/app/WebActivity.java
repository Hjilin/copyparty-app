package com.copyparty.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;

public class WebActivity extends Activity {
    private WebView web;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int port = getSharedPreferences("copyparty", MODE_PRIVATE).getInt("port", 5301);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        web = new WebView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        web.setLayoutParams(lp);
        root.addView(web);
        setContentView(root);

        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(true);
        web.getSettings().setAllowContentAccess(true);
        web.getSettings().setMediaPlaybackRequiresUserGesture(false);
        web.getSettings().setUseWideViewPort(true);
        web.getSettings().setLoadWithOverviewMode(true);
        web.getSettings().setBuiltInZoomControls(true);
        web.getSettings().setDisplayZoomControls(false);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            web.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // 注入移动端适配 CSS + 中文化 JS
                view.evaluateJavascript(JS_INJECT, null);
            }
        });

        String url = "http://127.0.0.1:" + port + "/";
        web.loadUrl(url);
    }

    private static final String JS_INJECT =
        "(function(){" +
        "var css=document.createElement('style');" +
        "css.textContent='body{font-size:16px!important;-webkit-text-size-adjust:100%!important;margin:0!important;padding:8px!important}'+
        "'.filelist table{width:100%!important;font-size:15px!important;border-collapse:collapse}'+
        "'.filelist td,.filelist th{padding:10px 6px!important;word-break:break-all}'+
        "'.navbar,.toolbar{padding:8px!important;flex-wrap:wrap!important;gap:6px!important}'+
        "'.navbar a,.toolbar a{font-size:14px!important;padding:6px 10px!important}'+
        "'.breadcrumb{font-size:14px!important;overflow-x:auto;white-space:nowrap}'+
        "'.filesize{font-size:13px!important}'+
        "'.breadcrumb span{display:inline-block;padding:2px 4px}'+
        "'.topbar{padding:6px 8px!important}'+
        "'.topbar *{font-size:14px!important}'+
        "'.btn,button,a.btn{padding:8px 14px!important;font-size:14px!important;border-radius:6px!important}'+
        "'.rightpane,.sidebar{display:none!important}'+
        "'.mainpane{width:100%!important;float:none!important}'+
        "'input,select{font-size:16px!important;padding:8px!important}';"+
        "document.head.appendChild(css);" +
        "var map={'File Name':'文件名','Size':'大小','Type':'类型','Modified':'修改时间','Download':'下载','Upload':'上传','Delete':'删除','Rename':'重命名','New Folder':'新建文件夹','Upload File':'上传文件','Home':'首页','Up':'上级目录','Search':'搜索','Logout':'退出登录','Login':'登录','Password':'密码','Username':'用户名','Name':'名称','Files':'文件','Folders':'文件夹','Free space':'可用空间','Total space':'总空间','control-panel':'控制面板','prev':'上一个','next':'下一个','up':'上级','copy':'复制','move':'移动','view':'查看','preview':'预览','edit':'编辑','Permissions':'权限','Owner':'所有者'};"+
        "function walk(n){if(n.nodeType===3){var t=n.textContent;if(map[t.trim()]){n.textContent=t.replace(t.trim(),map[t.trim()])}}else if(n.nodeType===1&&n.tagName!=='SCRIPT'&&n.tagName!=='STYLE'){for(var i=0;i<n.childNodes.length;i++)walk(n.childNodes[i])}}"+
        "walk(document.body);" +
        "var obs=new MutationObserver(function(){walk(document.body)});"+
        "obs.observe(document.body,{childList:true,subtree:true,characterData:true});" +
        "})();";

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }
}