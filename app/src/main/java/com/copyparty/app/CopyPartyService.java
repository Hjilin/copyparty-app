package com.copyparty.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CopyPartyService extends Service {
    private static final String TAG = "CopyPartySvc";
    private static final String CHANNEL_ID = "copyparty";
    private static final int NOTIF_ID = 1001;

    public static volatile Process process = null;
    private static volatile boolean running = false;
    private static int currentPort = 5301;
    private static final List<String> opLog = new ArrayList<>();
    private static final int MAX_LOG = 500;

    @Override
    public void onCreate() { super.onCreate(); createChannel(); }

    public static File logFile(Context ctx) {
        return new File(ctx.getFilesDir(), "copyparty.log");
    }

    private void createChannel() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "CopyParty", NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(ch);
            NotificationChannel ch2 = new NotificationChannel("events", "事件通知", NotificationManager.IMPORTANCE_DEFAULT);
            nm.createNotificationChannel(ch2);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroundInternal();
        if (intent != null && "com.copyparty.app.action.STOP".equals(intent.getAction())) {
            stopCopyParty();
            return START_NOT_STICKY;
        }
        startCopyParty();
        return START_STICKY;
    }

    private void startForegroundInternal() {
        try {
            Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("CopyParty 网盘")
                    .setContentText(isRunning() ? "运行中 · 端口 " + currentPort : "正在启动...")
                    .setSmallIcon(android.R.drawable.ic_menu_manage)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build();
            if (android.os.Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIF_ID, n, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } else {
                startForeground(NOTIF_ID, n);
            }
        } catch (Exception e) { Log.e(TAG, "fg fail", e); }
    }

    private void startCopyParty() {
        if (isRunning()) return;
        File dir = AssetExtractor.runDir(this);
        File py = new File(dir, "bin/python3");
        File pyz = new File(dir, "web/copyparty.pyz");
        if (!py.exists() || !pyz.exists()) {
            appendLog("正在解压运行包...");
            boolean ok = AssetExtractor.extract(this);
            if (!ok) { appendLog("解压失败"); return; }
            appendLog("解压完成，启动中...");
        }
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("/system/bin/linker64");
            cmd.add(py.getAbsolutePath());
            cmd.add(pyz.getAbsolutePath());
            cmd.add("-p"); cmd.add(String.valueOf(currentPort));
            cmd.add("-q"); cmd.add("--cors"); cmd.add("*");
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(dir);
            pb.environment().put("LD_LIBRARY_PATH", dir.getAbsolutePath() + "/lib");
            pb.environment().put("PYTHONHOME", dir.getAbsolutePath());
            pb.environment().put("PYTHONUTF8", "1");
            pb.environment().put("PYTHONUNBUFFERED", "1");
            pb.environment().put("HOME", dir.getAbsolutePath());
            process = pb.start();
            running = true;
            appendLog("=== copyparty 启动 ===");
            startLogReader(process.getInputStream());
            startLogReader(process.getErrorStream());
            new Thread(() -> { try { process.waitFor(); } catch (Exception e) {} running = false; }).start();
        } catch (Exception e) { appendLog("启动失败: " + e.getMessage()); }
    }

    private void startLogReader(InputStream is) {
        new Thread(() -> {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = br.readLine()) != null) {
                    appendLog(line);
                    detectEvent(line);
                }
            } catch (Exception e) {}
        }).start();
    }

    private void detectEvent(String line) {
        String l = line.toLowerCase();
        String title = null;
        if (l.contains("upload") || l.contains("put ") || l.contains("post ")) title = "文件上传";
        else if (l.contains("login") || l.contains("auth")) title = "登录事件";
        else if (l.contains("delete")) title = "文件删除";
        if (title != null) sendEventNotif(title, line.trim());
    }

    private void sendEventNotif(String title, String msg) {
        try {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            Notification n = new NotificationCompat.Builder(this, "events")
                    .setContentTitle(title)
                    .setContentText(msg.length() > 80 ? msg.substring(0, 80) : msg)
                    .setSmallIcon(android.R.drawable.stat_notify_sync)
                    .setAutoCancel(true)
                    .build();
            nm.notify((int) System.currentTimeMillis(), n);
        } catch (Exception e) {}
    }

    private void stopCopyParty() {
        running = false;
        if (process != null) { process.destroy(); process = null; }
        appendLog("=== 已停止 ===");
    }

    public static boolean isRunning() { return running; }
    public static synchronized List<String> getOpLog() { return new ArrayList<>(opLog); }

    private void appendLog(String s) {
        String line = "[" + now() + "] " + s;
        synchronized (opLog) {
            opLog.add(line);
            if (opLog.size() > MAX_LOG) opLog.remove(0);
        }
        try {
            FileOutputStream fos = new FileOutputStream(logFile(this), true);
            fos.write((line + "\n").getBytes("UTF-8"));
            fos.close();
        } catch (Exception e) {}
        Log.i(TAG, s);
    }

    private String now() { return new SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()); }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
