package com.oopp.qrzd.service;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.oopp.qrzd.capture.ScreenCaptureManager;
import com.oopp.qrzd.constants.Constants;
import com.oopp.qrzd.vision.MlkitOcrEngine;

import timber.log.Timber;

/**
 * 前台服务：持有 MediaProjection，产出帧
 */
public class CaptureService extends Service {
    private ScreenCaptureManager capture;
    private volatile boolean ocrEnabled = false;
    private final java.util.concurrent.ExecutorService ocrExec =
            java.util.concurrent.Executors.newSingleThreadExecutor();
    private volatile boolean inFlight = false;
    private long lastOcrMs = 0;
    private int targetFps = 12; // 可从 Intent/SharedPreferences 取


    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, ensureNotif());
        if (intent != null && Constants.ACT_SET_OCR_ENABLED.equals(intent.getAction())) {
            ocrEnabled = intent.getBooleanExtra(Constants.EXTRA_ENABLED, false);
            startForeground(1, ensureNotif(/*ocrEnabled*/)); // 刷新通知标题/文本
        }

        int code = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED);
        Intent data = intent.getParcelableExtra("data");
        if (capture == null) capture = new ScreenCaptureManager();

        capture.start(this, code, data, 1280, 720, dpi, (bmp, ts) -> {
            if (!ocrEnabled) return;

            long now = System.currentTimeMillis();
            long interval = 1000L / Math.max(1, targetFps);
            if (inFlight || (now - lastOcrMs) < interval) return;
            inFlight = true;

            // 可选：裁剪 ROI、缩放 0.5x 降负载（演示省略）
            final Bitmap frame = bmp.copy(Bitmap.Config.ARGB_8888, false);

            ocrExec.execute(() -> {
                long t0 = System.currentTimeMillis();
                String text = ""; int count = 0;
                try {
                    MlkitOcrEngine eng = MlkitOcrEngine.MlkitHolder.eng(getApplicationContext());
                    com.google.mlkit.vision.text.Text res = eng.blockingRecognize(frame);
                    text  = res.getText();
                    count = res.getTextBlocks().size();
                } catch (Exception e) {
                    text = "ERR:" + e.getMessage();
                } finally {
                    frame.recycle();
                    inFlight = false;
                    lastOcrMs = System.currentTimeMillis();
                }

                long lat = System.currentTimeMillis() - t0;
                Timber.tag("OCR").d("blocks=" + count + " ms=" + lat + " text=" +
                        (text.length() > 60 ? text.substring(0, 60) + "..." : text));

                // 发应用内广播给悬浮窗/Activity
                Intent br = new Intent(Constants.ACT_OCR_RESULT)
                        .setPackage(getPackageName())
                        .putExtra(Constants.EXTRA_TEXT_SNIPPET, text.length()>80? text.substring(0,80)+"…" : text)
                        .putExtra(Constants.EXTRA_COUNT, count)
                        .putExtra(Constants.EXTRA_LATENCY_MS, (int) lat);
                sendBroadcast(br);
            });
        });

        return START_STICKY;
    }
    private Notification ensureNotif() {
        final String chId = "cap"; final String chName = "Capture";
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(chId, chName, NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false); ch.enableLights(false); ch.enableVibration(false);
            nm.createNotificationChannel(ch);
        }
        return new androidx.core.app.NotificationCompat.Builder(this, chId)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("Screen capture")
                .setContentText(ocrEnabled ? "OCR ON" : "OCR OFF")
                .setOngoing(true)
                .setCategory(androidx.core.app.NotificationCompat.CATEGORY_SERVICE)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MIN)
                .build();
    }


    @Override public void onDestroy() { if (capture!=null) capture.stop(); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}