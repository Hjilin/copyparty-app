package com.copyparty.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity {
    private static final int REQ_PERMS = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestAllPermissions();
    }

    private void requestAllPermissions() {
        String[] perms;
        if (Build.VERSION.SDK_INT >= 33) {
            perms = new String[]{
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.READ_MEDIA_IMAGES",
                "android.permission.READ_MEDIA_VIDEO",
                "android.permission.READ_MEDIA_AUDIO",
                "android.permission.POST_NOTIFICATIONS",
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.ACCESS_COARSE_LOCATION",
                "android.permission.CAMERA",
                "android.permission.RECORD_AUDIO"
            };
        } else {
            perms = new String[]{
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.ACCESS_COARSE_LOCATION",
                "android.permission.CAMERA",
                "android.permission.RECORD_AUDIO"
            };
        }
        java.util.List<String> need = new java.util.ArrayList<>();
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                need.add(p);
            }
        }
        if (!need.isEmpty()) {
            ActivityCompat.requestPermissions(this, need.toArray(new String[0]), REQ_PERMS);
        } else {
            goNext();
        }
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] p, int[] res) {
        super.onRequestPermissionsResult(req, p, res);
        goNext();
    }

    private void goNext() {
        CopyPartyService.start(this);
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(MainActivity.this, WebActivity.class));
                finish();
            }
        }, 1500);
    }
}