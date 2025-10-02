package com.oopp.qrzd.service;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import com.google.mlkit.vision.text.Text;
import com.oopp.qrzd.capture.ScreenCaptureManager;
import com.oopp.qrzd.constants.Constants;
import com.oopp.qrzd.vision.MlkitOcrEngine;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import timber.log.Timber;

/**
 * 前台服务：持有 MediaProjection，产出帧
 */
public class CaptureService extends Service {
    private ScreenCaptureManager capture;
    private volatile boolean ocrEnabled = false;
    private final ExecutorService ocrExec = Executors.newSingleThreadExecutor();
    private volatile boolean inFlight = false;
    private long lastOcrMs = 0;
    private int targetFps = 4;              // 先降到 4，更稳
    private long lastHash = 0;              // 帧差过滤

    private long lastFullHash = 0;
    private static final int HASH_SIZE = 24;            // aHash 缩图尺寸（越大越敏感）
    private static final int HD_BIG_CHANGE = 48;        // 判定“大变化”的阈值（可调 32~64）
    private static final long FORCE_OCR_GAP_MS = 1200;  // 超过此间隔强制做一次 OCR


    @Override
    public void onCreate() {
        super.onCreate();
        // 预热一次（避免首帧加载模型导致的超长耗时）
        ocrExec.execute(() -> {
            try {
                Bitmap tiny = Bitmap.createBitmap(64,64, Bitmap.Config.ARGB_8888);
                tiny.eraseColor(0xFFFFFFFF);
                MlkitOcrEngine.MlkitHolder.eng(getApplicationContext()).blockingRecognize(tiny);
                tiny.recycle();
                Timber.tag("OCR").i("warmed up");
            } catch (Exception ignore) {}
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 先上前台，避免被杀
        startForeground(1, ensureNotif());
        String action = intent != null ? intent.getAction() : null;
        Log.i("CAP", "onStartCommand action=" + action + " ocrEnabled=" + ocrEnabled);

        // 仅切换 OCR 开关：不碰 MediaProjection
        if (Constants.ACT_SET_OCR_ENABLED.equals(action)) {
            ocrEnabled = intent.getBooleanExtra(Constants.EXTRA_ENABLED, false);
            // 重置基线，避免旧场景干扰
            lastFullHash = 0;
            lastOcrMs = 0;
            inFlight = false;
            startForeground(1, ensureNotif());
            return START_STICKY;
        }


        // 停止采集
        if (Constants.ACT_STOP_CAPTURE.equals(action)) {
            stopCapture();
            stopSelf();
            return START_NOT_STICKY;
        }

        // 开始采集：必须携带授权
        if (Constants.ACT_START_CAPTURE.equals(action)) {
            int code = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED);
            Intent data = intent.getParcelableExtra("data");
            if (code != Activity.RESULT_OK || data == null) {
                Timber.tag("CAP").w("Start without MediaProjection permission. Ignored.");
                return START_STICKY;
            }
            if (capture == null) capture = new ScreenCaptureManager();

            // 真实分辨率与 dpi
            DisplayMetrics dm = new android.util.DisplayMetrics();
            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            wm.getDefaultDisplay().getRealMetrics(dm);
            int width = dm.widthPixels, height = dm.heightPixels, dpi = dm.densityDpi;

            capture.start(this, code, data, width, height, dpi, (bmp, ts) -> {
                if (!ocrEnabled) {
                    Timber.tag("OCR").d("skip: ocrEnabled=false");
                    bmp.recycle();
                    return;
                }

                // === 先做整帧哈希，捕捉大变化 ===
                long now = System.currentTimeMillis();
                long fullHash = aHash(bmp, HASH_SIZE);
                int hd = Long.bitCount(fullHash ^ lastFullHash);
                boolean bigChange = hd >= HD_BIG_CHANGE;
                boolean timeStale = (now - lastOcrMs) >= FORCE_OCR_GAP_MS;
                lastFullHash = fullHash; // 每帧刷新基线

                long interval = 1000L / Math.max(1, targetFps);

                // === inFlight 仍然最先拦截（防止并发）
                if (inFlight) {
                    bmp.recycle();
                    return;
                }

                // === 若不是大变化、也没到强制间隔，才按节流跳过
                if (!bigChange && !timeStale && (now - lastOcrMs) < interval) {
                    bmp.recycle(); // ★ 修复你之前这里没 recycle 的泄漏
                    return;
                }

                // 走到这里：要么大变化、要么超时兜底、要么正常节奏到点
                inFlight = true;

                // ScreenCaptureManager 返回的就是独立 ARGB_8888，可直接用，无需再 copy
                final Bitmap frame = bmp;

                ocrExec.execute(() -> {

                    try {

                        long t0 = System.currentTimeMillis();
                        MlkitOcrEngine eng = MlkitOcrEngine.MlkitHolder.eng(getApplicationContext());
                        Text res = eng.blockingRecognize(frame);
                        long lat = System.currentTimeMillis() - t0;

                        String text = res.getText();
                        int count = res.getTextBlocks().size();

                        Timber.tag("OCR").d("blocks=%d ms=%d hd=%d text=%s",
                                count, lat, hd, text);

                        Intent br = new Intent(Constants.ACT_OCR_RESULT)
                                .setPackage(getPackageName())
                                .putExtra(Constants.EXTRA_TEXT_SNIPPET, text.length()>80? text.substring(0,80)+"…" : text)
                                .putExtra(Constants.EXTRA_COUNT, count)
                                .putExtra(Constants.EXTRA_LATENCY_MS, (int) lat);
                        sendBroadcast(br);


                    } catch (Exception e) {
                        Timber.tag("OCR").e("err: %s", e.getMessage());
                    } finally {
                        frame.recycle();
                        inFlight = false;
                        lastOcrMs = System.currentTimeMillis();
                    }

                });
            });
            return START_STICKY;
        }

        // 其他/系统重启情况：什么也不做，仅保持前台
        return START_STICKY;
    }

    private void stopCapture() {
        try { if (capture != null) capture.stop(); }
        catch (Throwable ignore) {}
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

    // === 工具：aHash 帧差（极轻量）===
    private long aHash(Bitmap bmp, int size) {
        Bitmap s = Bitmap.createScaledBitmap(bmp, size, size, true);
        long sum = 0;
        int[] px = new int[size * size];
        s.getPixels(px, 0, size, 0, 0, size, size);
        float[] lum = new float[px.length];
        for (int i = 0; i < px.length; i++) {
            int c = px[i];
            int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
            float l = 0.299f * r + 0.587f * g + 0.114f * b; // 亮度
            lum[i] = l; sum += l;
        }
        float avg = sum / lum.length;
        long bits = 0;
        for (float v : lum) bits = (bits << 1) | (v >= avg ? 1 : 0);
        s.recycle();
        return bits;
    }
}