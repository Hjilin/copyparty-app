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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/** 前台服务：用 linker 启动 copyparty python，日志写到 filesDir/copyparty.log */
public class CopyPartyService extends Service {
    private static final String TAG = "CopyPartySvc";
    private static final String CHANNEL_ID = "copyparty";
    private static final int NOTIF_ID = 1001;

    public static volatile Process process = null;
    private static volatile boolean running = false;
    private static Thread logReader;
    private static int currentPort = 5301;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 常驻通知
        startForegroundInternal();
        if (intent != null && "com.copyparty.app.action.STOP".equals(intent.getAction())) {
            stopCopyParty();
            return START_NOT_STICKY;
        }
        // 启动（若未运行）
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
        } catch (Exception e) {
            Log.e(TAG, "startForeground 失败", e);
            appendLog("!!! 通知前台启动失败: " + e.getMessage() + "（请检查通知权限是否开启）");
        }
    }

    private void startCopyParty() {
        if (isRunning()) return;
        File dir = AssetExtractor.runDir(this);
        File py = new File(dir, "bin/python3");
        File pyz = new File(dir, "web/copyparty.pyz");
        if (!py.exists() || !pyz.exists()) {
            Log.e(TAG, "copyparty 未解压，无法启动: " + dir.getAbsolutePath());
            appendLog("=== 启动失败 @ " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()) + " ===");
            appendLog("运行包未解压或不完整: " + dir.getAbsolutePath());
            appendLog("python3 存在=" + py.exists() + ", copyparty.pyz 存在=" + pyz.exists());
            appendLog("请回到主界面重新点启动（会自动解压）");
            return;
        }
        int port = getSharedPreferences("copyparty", MODE_PRIVATE).getInt("port", 5301);
        currentPort = port;

        try {
            // ELF 检测：用 linker64 启动
            String linker = new File("/system/bin/linker64").exists() ? "/system/bin/linker64" : "/system/bin/linker";
            List<String> cmdList = new ArrayList<>();
            cmdList.add(linker);
            cmdList.add(py.getAbsolutePath());
            cmdList.add(pyz.getAbsolutePath());
            cmdList.add("-p");
            cmdList.add(String.valueOf(port));
            cmdList.add("-q");
            // 环境变量：LD_LIBRARY_PATH 指向 lib，PYTHONHOME 指向运行目录
            List<String> envList = new ArrayList<>();
            envList.add("LD_LIBRARY_PATH=" + new File(dir, "lib").getAbsolutePath());
            envList.add("PYTHONHOME=" + dir.getAbsolutePath());
            envList.add("PYTHONUTF8=1");
            envList.add("HOME=" + dir.getAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder(cmdList);
            pb.environment().putAll(System.getenv());
            for (String e : envList) {
                String[] kv = e.split("=", 2);
                if (kv.length == 2) pb.environment().put(kv[0], kv[1]);
            }
            pb.environment().remove("LD_PRELOAD");
            pb.directory(dir);
            pb.redirectErrorStream(true);

            process = pb.start();
            appendLog("!!! process started, pid=" + process.hashCode());
            running = true;

            // 日志读取线程
            final File logFile = new File(getFilesDir(), "copyparty.log");
            final InputStream is = process.getInputStream();
            logReader = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(is));
                     FileOutputStream fos = new FileOutputStream(logFile, true)) {
                    fos.write(("=== copyparty started @ " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()) + " ===\n").getBytes());
                    String line;
                    while ((line = br.readLine()) != null) {
                        fos.write((line + "\n").getBytes());
                        fos.flush();
                    }
                } catch (Exception ignored) {}
                running = false;
                int exitCode = -999;
                try { exitCode = process.exitValue(); } catch (Exception ignored) {}
                try (FileOutputStream fos = new FileOutputStream(logFile, true)) {
                    fos.write(("=== copyparty exited, exitCode=" + exitCode + " ===\n").getBytes());
                } catch (Exception ignored) {}
            }, "cp-log");
            logReader.setDaemon(true);
            logReader.start();

            // 等待端口就绪
            for (int i = 0; i < 40; i++) {
                if (portOpen(port)) break;
                try { Thread.sleep(300); } catch (Exception ignored) {}
            }
            Log.i(TAG, "copyparty 已启动, port=" + port);
        } catch (Exception e) {
            Log.e(TAG, "启动 copyparty 失败", e);
            running = false;
        }
    }

    public static void stopCopyParty() {
        if (process != null) {
            try { process.destroyForcibly(); } catch (Exception ignored) {}
            try { process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS); } catch (Exception ignored) {}
            process = null;
        }
        running = false;
    }

    public static boolean isRunning() {
        if (process == null) return false;
        try {
            if (process.isAlive()) return true;
        } catch (Exception ignored) {}
        running = false;
        return false;
    }

    private static boolean portOpen(int port) {
        try {
            Socket s = new Socket();
            s.connect(new InetSocketAddress("127.0.0.1", port), 600);
            s.close();
            return true;
        } catch (Exception e) { return false; }
    }

    public static File logFile(Context ctx) {
        return new File(ctx.getFilesDir(), "copyparty.log");
    }

    private void appendLog(String line) {
        try (FileOutputStream fos = new FileOutputStream(logFile(this), true)) {
            fos.write((line + "\n").getBytes());
        } catch (Exception ignored) {}
    }

    private void createChannel() {
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "CopyParty", NotificationManager.IMPORTANCE_LOW);
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(ch);
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
