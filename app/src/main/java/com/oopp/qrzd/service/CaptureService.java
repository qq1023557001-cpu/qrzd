package com.oopp.qrzd.service;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.oopp.qrzd.capture.ScreenCaptureManager;

/**
 * 前台服务：持有 MediaProjection，产出帧
 */
public class CaptureService extends Service {
    private ScreenCaptureManager capture;

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, ensureNotif());
        int code = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED);
        Intent data = intent.getParcelableExtra("data");
        if (capture == null) capture = new ScreenCaptureManager();
        capture.start(this, code, data,
                1280, 720, getResources().getDisplayMetrics().densityDpi,
                (bmp, ts) -> { /* TODO: 传给 OCR/检测流水线；不落盘 */ });
        return START_STICKY;
    }

    private Notification ensureNotif() {
        String chId = "cap";
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(chId, "Capture", NotificationManager.IMPORTANCE_MIN));
        }
        return new Notification.Builder(this, chId).setContentTitle("Screen capture running").setSmallIcon(android.R.drawable.ic_btn_speak_now).build();
    }

    @Override public void onDestroy() { if (capture!=null) capture.stop(); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}