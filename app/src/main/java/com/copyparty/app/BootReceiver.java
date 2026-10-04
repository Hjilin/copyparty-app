package com.copyparty.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/** 开机自启：若用户开启开机自启，启动前台服务 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action) &&
            !"android.intent.action.QUICKBOOT_POWERON".equals(action)) return;

        boolean autostart = context.getSharedPreferences("copyparty", Context.MODE_PRIVATE)
                .getBoolean("autostart", false);
        if (!autostart) {
            Log.i(TAG, "开机自启未开启，跳过");
            return;
        }
        Log.i(TAG, "开机自启触发");
        try {
            Intent svc = new Intent(context, CopyPartyService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(svc);
            } else {
                context.startService(svc);
            }
        } catch (Exception e) {
            Log.e(TAG, "开机启动失败", e);
        }
    }
}
