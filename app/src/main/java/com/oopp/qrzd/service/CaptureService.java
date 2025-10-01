package com.oopp.qrzd.service;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

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
        final String channelId = "cap";
        final String channelName = "Capture";
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Android 8.0+ 必须先注册渠道；建议 IMPORTANCE_LOW（MIN 有些机型会把前台服务干掉）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    channelId, channelName, NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            ch.enableLights(false);
            ch.enableVibration(false);
            nm.createNotificationChannel(ch);
        }

        NotificationCompat.Builder b = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)   // TODO: 换成自己的前台小图标
                .setContentTitle("Screen capture running")
                .setContentText("Capturing frames for OCR")
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                // 兼容项：在 26 以下用优先级，26+ 由渠道控制
                .setPriority(NotificationCompat.PRIORITY_MIN);

        // Android 12+（API 31）前台行为提示（Compat 会自动在低版本忽略）
        b.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);

        return b.build();
    }

    @Override public void onDestroy() { if (capture!=null) capture.stop(); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}