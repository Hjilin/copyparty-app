package com.copyparty.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS = "copyparty";

    private TextView tvStatus, tvDetail;
    private View statusDot;
    private EditText etPort;
    private Switch swAutostart;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refreshTask = new Runnable() {
        @Override public void run() {
            refreshUi();
            handler.postDelayed(this, 2000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tv_status);
        tvDetail = findViewById(R.id.tv_detail);
        statusDot = findViewById(R.id.status_dot);
        etPort = findViewById(R.id.et_port);
        swAutostart = findViewById(R.id.sw_autostart);
        TextView btnStart = findViewById(R.id.btn_start);
        TextView btnStop = findViewById(R.id.btn_stop);
        LinearLayout rowLog = findViewById(R.id.row_log);

        // 开机自启开关
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        swAutostart.setChecked(sp.getBoolean("autostart", false));
        etPort.setText(String.valueOf(sp.getInt("port", 5301)));
        swAutostart.setOnCheckedChangeListener((b, checked) ->
                sp.edit().putBoolean("autostart", checked).apply());

        btnStart.setOnClickListener(v -> startServiceAction("com.copyparty.app.action.START"));
        btnStop.setOnClickListener(v -> {
            startService(new Intent(this, CopyPartyService.class)
                    .setAction("com.copyparty.app.action.STOP"));
            refreshUi();
        });
        rowLog.setOnClickListener(v -> showLogDialog());

        // 确保运行包解压（后台）
        new Thread(() -> {
            if (!AssetExtractor.isReady(this)) {
                AssetExtractor.extract(this);
            }
            handler.post(this::refreshUi);
        }).start();

        refreshUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(refreshTask);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refreshTask);
    }

    private void startServiceAction(String action) {
        int port;
        try {
            port = Integer.parseInt(etPort.getText().toString().trim());
            if (port < 1 || port > 65535) { etPort.setError("端口无效"); return; }
        } catch (Exception e) {
            etPort.setError("端口无效"); return;
        }
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit().putInt("port", port).apply();
        Intent i = new Intent(this, CopyPartyService.class).setAction(action);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(i);
        } else {
            startService(i);
        }
        handler.postDelayed(this::refreshUi, 1500);
    }

    private void refreshUi() {
        boolean running = CopyPartyService.isRunning();
        int port = getSharedPreferences(PREFS, MODE_PRIVATE).getInt("port", 5301);
        statusDot.setBackgroundResource(running ? R.drawable.bg_dot_on : R.drawable.bg_dot_off);
        tvStatus.setText(running ? R.string.running : R.string.stopped);
        tvStatus.setTextColor(getColor(running ? R.color.green : R.color.text_main));
        tvDetail.setText("端口 " + port + " · " + (running ? "可通过 http://本机IP:" + port + " 访问" : "尚未运行"));
        etPort.setText(String.valueOf(port));
    }

    private void showLogDialog() {
        File f = CopyPartyService.logFile(this);
        String content = readLog(f);
        TextView tv = new TextView(this);
        tv.setTextSize(11f);
        tv.setTypeface(android.graphics.Typeface.MONOSPACE);
        tv.setTextColor(0xFF202124);
        tv.setPadding(dp(12), dp(10), dp(12), dp(10));
        tv.setText(content.isEmpty() ? "（暂无日志）" : content);
        ScrollView sv = new ScrollView(this);
        sv.addView(tv);
        new AlertDialog.Builder(this)
                .setTitle("copyparty 日志")
                .setView(sv)
                .setPositiveButton("分享", (d, w) -> shareLog(content))
                .setNegativeButton("关闭", null)
                .show();
    }

    private void shareLog(String content) {
        try {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, "copyparty 日志");
            i.putExtra(Intent.EXTRA_TEXT, content);
            startActivity(Intent.createChooser(i, "分享日志"));
        } catch (Exception e) {
            android.widget.Toast.makeText(this, "分享失败", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private String readLog(File f) {
        try {
            if (!f.exists() || f.length() <= 0) return "";
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
                byte[] buf = new byte[8192]; int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            }
            String s = new String(out.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
            int len = s.length();
            return s.substring(Math.max(0, len - 12000));
        } catch (Exception e) {
            return "读取失败: " + e.getMessage();
        }
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }
}
