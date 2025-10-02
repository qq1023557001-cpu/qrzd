package com.oopp.qrzd.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.Toast;

import com.oopp.qrzd.app.MainActivity;
import com.oopp.qrzd.service.component.DragMoveListener;

/**
 * 无障碍服务：派发点击/滑动/长按/曲线拖拽
 */
public class AutoAccessibilityService extends AccessibilityService {

    private WindowManager wm;
    private View overlay;
    private WindowManager.LayoutParams lp;
    private boolean running = false; // 示例：单击切换开/停

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) { /* 可暂不处理 */ }

    @Override
    public void onInterrupt() { /* 可在此暂停自动化 */ }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        createOverlay();
        toast("AutoAccessibilityService connected");
    }

    @Override
    public boolean onUnbind(android.content.Intent intent) {
        removeOverlay();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        removeOverlay();
        super.onDestroy();
    }

    private void createOverlay() {
        if (overlay != null || wm == null) return;

        // 1) 先准备 View
        ImageView bubble = new ImageView(this);
        bubble.setImageResource(android.R.drawable.presence_online);
        int size = dp(56);
        bubble.setLayoutParams(new WindowManager.LayoutParams(size, size));
        overlay = bubble;

        // 2) **先创建 lp**（注意：必须在绑定 DragMoveListener 之前）
        int type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY; // API 28 OK
        lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        lp.x = 0; lp.y = 0;

        // 3) 绑定你实现的 DragMoveListener（可拖动 & 点击不丢失）
        // 确保可点可长按
        overlay.setClickable(true);
        overlay.setLongClickable(true);
        overlay.setOnTouchListener(new DragMoveListener(
                this, wm, lp,
                new DragMoveListener.Callback() {
                    @Override public void onClick(View v) {
                        running = !running;
                        toast(running ? "▶ 已开始" : "⏸ 已暂停");

                        // 演示：点屏幕中心，并带可视化“闪点”
                        int cx = getResources().getDisplayMetrics().widthPixels / 2;
                        int cy = getResources().getDisplayMetrics().heightPixels / 2;
                        tapWithFeedback(cx, cy);
                    }

                    @Override public void onLongPress(View v) {
                        // 从 Service 拉起 Activity 必须加 NEW_TASK
                        android.content.Intent i =
                                new android.content.Intent(getApplicationContext(), MainActivity.class);
                        i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                    }
                }
        ));

        // 4) addView 到窗口
        wm.addView(overlay, lp);
        toast("overlay added");
    }

    private void removeOverlay() {
        if (wm != null && overlay != null) {
            wm.removeView(overlay);
            overlay = null;
        }
    }

    // ===== 手势：点击（带回调 + 可视化“闪点”）=====
    private void tapWithFeedback(int x, int y) {
        Path p = new Path(); p.moveTo(x, y);
        GestureDescription g = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(p, 0, 60))
                .build();

        flashDot(x, y);

        dispatchGesture(g, new GestureResultCallback() {
            @Override public void onCompleted(GestureDescription gestureDescription) {
                toast("tap OK @(" + x + "," + y + ")");
            }
            @Override public void onCancelled(GestureDescription gestureDescription) {
                toast("tap CANCELLED");
            }
        }, null);
    }

    // 在点击位置显示 300ms 的小点，方便你“看见”注入位置
    private void flashDot(int x, int y) {
        View dot = new View(this);
        dot.setBackgroundColor(0xFFFF4081); // 粉色
        int d = dp(16);
        WindowManager.LayoutParams dlp = new WindowManager.LayoutParams(
                d, d,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        dlp.gravity = Gravity.START | Gravity.TOP;
        dlp.x = x - d / 2;
        dlp.y = y - d / 2;

        wm.addView(dot, dlp);
        dot.postDelayed(() -> {
            try { wm.removeView(dot); } catch (Throwable ignore) {}
        }, 300);
    }

    // ===== 工具 =====
    private int dp(int dp) {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        return Math.round(dp * dm.density);
    }
    private void toast(String s) {
        try {
            Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}