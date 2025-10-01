package com.oopp.qrzd.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
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

import com.oopp.qrzd.service.component.DragMoveListener;

/**
 * 无障碍服务：派发点击/滑动/长按/曲线拖拽
 */
public class AutoAccessibilityService extends AccessibilityService {

    private WindowManager wm;
    private View overlay;
    private WindowManager.LayoutParams lp;

    // ====== 必须方法 #1：接收无障碍事件（此处最小实现，先不做逻辑） ======
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 可选：监听窗口切换、前台应用变化等
        // int type = event.getEventType();
        // CharSequence pkg = event.getPackageName();
    }

    // ====== 必须方法 #2：被系统打断（如来电、切后台）时回调 ======
    @Override
    public void onInterrupt() {
        // 可在此暂停你的自动化流程
    }

    // ====== 服务连接成功：初始化悬浮窗 ======
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        createOverlay();
    }

    // ====== 可选：解绑/销毁时清理悬浮窗 ======
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

    // ====== 创建悬浮窗（优先 TYPE_ACCESSIBILITY_OVERLAY） ======
    private void createOverlay() {
        if (overlay != null || wm == null) return;

        ImageView bubble = new ImageView(this);
        // 先用系统图标占位，后续换成你的资源
        bubble.setImageResource(android.R.drawable.presence_online);

        // 设置一个合适的尺寸（例如 56dp）
        int size = dp(56);
        bubble.setLayoutParams(new WindowManager.LayoutParams(size, size));

        // 简易拖动
        bubble.setOnTouchListener(new View.OnTouchListener() {
            int startX, startY; float touchX, touchY;
            @Override public boolean onTouch(View v, MotionEvent e) {
                if (lp == null) return false;
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = lp.x; startY = lp.y;
                        touchX = e.getRawX(); touchY = e.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        lp.x = startX + Math.round(e.getRawX() - touchX);
                        lp.y = startY + Math.round(e.getRawY() - touchY);
                        wm.updateViewLayout(overlay, lp);
                        return true;
                }
                return false;
            }
        });

        // 点击示例：点屏幕中点；之后你可换成“点击识别到的坐标”
        bubble.setOnClickListener(v -> {
            int x = getScreenWidth() / 2;
            int y = getScreenHeight() / 2;
            tap(x, y);
        });

        overlay = bubble;

        int type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY; // API 28 可用
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

        wm.addView(overlay, lp);
    }

    private void removeOverlay() {
        if (wm != null && overlay != null) {
            wm.removeView(overlay);
            overlay = null;
        }
    }

    // ====== 手势封装：点击 ======
    public void tap(int x, int y) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return; // API 24+
        Path p = new Path();
        p.moveTo(x, y);
        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(p, 0, 60);
        dispatchGesture(new GestureDescription.Builder().addStroke(stroke).build(),
                null, null);
    }

    // 你也可以加 swipe/dragPath 等封装

    // ====== 工具方法 ======
    private int dp(int dp) {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        return Math.round(dp * dm.density);
    }
    private int getScreenWidth() { return getResources().getDisplayMetrics().widthPixels; }
    private int getScreenHeight() { return getResources().getDisplayMetrics().heightPixels; }
}