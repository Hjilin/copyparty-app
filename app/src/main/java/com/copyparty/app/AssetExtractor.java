package com.copyparty.app;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** 首次启动把 assets 里的 copyparty 运行包解压到 filesDir/copyparty */
public class AssetExtractor {
    private static final String TAG = "AssetExtractor";
    private static final String ASSET_NAME = "copyparty-runtime.zip";
    private static final String RUN_DIR = "copyparty";
    private static final String VERSION_MARK = "cp_ready_v1";

    /** 运行根目录 */
    public static File runDir(Context ctx) {
        return new File(ctx.getFilesDir(), RUN_DIR);
    }

    /** 已解压且版本一致？ */
    public static boolean isReady(Context ctx) {
        File mark = new File(ctx.getFilesDir(), VERSION_MARK);
        return mark.exists() && new File(runDir(ctx), "web/copyparty.pyz").exists();
    }

    /** 解压（阻塞，应在后台线程调用）。返回是否成功 */
    public static boolean extract(Context ctx) {
        File dir = runDir(ctx);
        try {
            if (!dir.exists()) dir.mkdirs();
            InputStream is = ctx.getAssets().open(ASSET_NAME);
            ZipInputStream zis = new ZipInputStream(is);
            ZipEntry entry;
            byte[] buf = new byte[65536];
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                // 防 zip-slip
                File target = new File(dir, name);
                if (!target.getCanonicalPath().startsWith(dir.getCanonicalPath())) {
                    Log.w(TAG, "跳过非法路径: " + name);
                    continue;
                }
                if (entry.isDirectory()) {
                    target.mkdirs();
                } else {
                    File parent = target.getParentFile();
                    if (parent != null) parent.mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(target)) {
                        int n;
                        while ((n = zis.read(buf)) > 0) fos.write(buf, 0, n);
                    }
                }
                zis.closeEntry();
            }
            zis.close();
            is.close();

            // 关键：恢复 ELF 可执行权限（zip 解压丢失权限位）
            File py = new File(dir, "bin/python3");
            if (py.exists()) py.setExecutable(true, false);
            // 所有 bin/lib 下 .so 加读权限
            File libDir = new File(dir, "lib");
            File[] libs = libDir.listFiles();
            if (libs != null) {
                for (File l : libs) {
                    if (l.isFile()) {
                        l.setReadable(true, false);
                        if (l.getName().endsWith(".so")) l.setExecutable(true, false);
                    }
                }
            }

            // 写版本标记
            File mark = new File(ctx.getFilesDir(), VERSION_MARK);
            try (FileOutputStream fos = new FileOutputStream(mark)) {
                fos.write(VERSION_MARK.getBytes());
            }
            Log.i(TAG, "copyparty 运行包解压完成: " + dir.getAbsolutePath());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "解压失败", e);
            return false;
        }
    }
}
