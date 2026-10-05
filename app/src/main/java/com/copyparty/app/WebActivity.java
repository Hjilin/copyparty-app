package com.copyparty.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

public class WebActivity extends Activity {
    private WebView web;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int port = getSharedPreferences("copyparty", MODE_PRIVATE).getInt("port", 5301);
        LinearLayout root = new LinearLayout(this);
        root.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        web = new WebView(this);
        web.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        root.addView(web);
        setContentView(root);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setMediaPlaybackRequiresUserGesture(false);
        web.getSettings().setUseWideViewPort(true);
        web.getSettings().setLoadWithOverviewMode(true);
        web.getSettings().setBuiltInZoomControls(true);
        web.getSettings().setDisplayZoomControls(false);
        web.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) { return false; }
            @Override
            public void onPageFinished(WebView v, String url) { v.evaluateJavascript(JS, null); }
        });
        web.loadUrl("http://127.0.0.1:" + port + "/");
    }

    private static final String JS = "(function(){var s=document.createElement('style');s.textContent='body{font-size:16px!important;margin:0!important;padding:8px!important}table{width:100%!important;font-size:15px!important;border-collapse:collapse}td,th{padding:10px 6px!important;word-break:break-all}.navbar,.toolbar{padding:8px!important;flex-wrap:wrap!important;gap:6px!important}a{font-size:14px!important;padding:6px 10px!important}button{padding:8px 14px!important;font-size:14px!important;border-radius:6px!important}.rightpane,.sidebar{display:none!important}.mainpane{width:100%!important;float:none!important}input,select{font-size:16px!important;padding:8px!important}';document.head.appendChild(s);var m={'File Name':'文件名','Size':'大小','Type':'类型','Modified':'修改时间','Date':'日期','Download':'下载','Upload':'上传','Delete':'删除','Rename':'重命名','New Folder':'新建文件夹','Home':'首页','Up':'上级','Search':'搜索','Logout':'退出','Login':'登录','Password':'密码','Username':'用户名','Name':'名称','Files':'文件','Folders':'文件夹','control-panel':'控制面板','prev':'上一个','next':'下一个','up':'上级','copy':'复制','move':'移动','view':'查看','preview':'预览','edit':'编辑','switches':'开关','tooltips':'提示','the grid':'网格视图','thumbs':'缩略图','sel':'选择','dsel':'取消选','dl':'下载','dotfiles':'隐藏文件','qdel':'快速删除','first':'置顶','nsort':'名称排序','readme':'说明文件','autogrid':'自动网格','Enable':'启用','filesize':'文件大小','theme':'主题','classic dark':'经典暗色','language':'语言','English':'英语','folder download':'文件夹打包','tar':'tar包','pax':'pax包','tgz':'tgz包','txz':'txz包','zip':'zip包','zip_dos':'zip(DOS)','zip_crc':'zip(CRC)','up2k switches':'上传开关','write access':'可写','free of':'可用','GiB':'GB','view':'查看','control':'控制','panel':'面板'};function w(n){if(n.nodeType===3){var t=n.textContent;var k=t.trim();if(m[k]&&k.length>1){n.textContent=t.replace(k,m[k])}}else if(n.nodeType===1&&n.tagName!=='SCRIPT'&&n.tagName!=='STYLE'&&n.tagName!=='SELECT'){for(var i=0;i<n.childNodes.length;i++)w(n.childNodes[i])}}w(document.body);new MutationObserver(function(){w(document.body)}).observe(document.body,{childList:true,subtree:true,characterData:true});})();";

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}