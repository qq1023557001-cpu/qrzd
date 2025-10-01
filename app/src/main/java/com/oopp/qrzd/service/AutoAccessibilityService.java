package com.oopp.qrzd.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.oopp.qrzd.service.component.DragMoveListener;

/**
 * 无障碍服务：派发点击/滑动/长按/曲线拖拽
 */
public class AutoAccessibilityService extends AccessibilityService {
    private WindowManager wm;
    private View overlay;
    private WindowManager.LayoutParams lp;

    @Override public void onServiceConnected() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        createOverlay();
    }

    private void createOverlay() {
        if (overlay != null) return;
        ImageView bubble = new ImageView(this);
        bubble.setImageResource(android.R.drawable.presence_online); // TODO: 换成你的悬浮图
        overlay = bubble;

        lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, // 首选
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.START | Gravity.CENTER_VERTICAL;

        overlay.setOnTouchListener(new DragMoveListener(wm, lp));
        overlay.setOnClickListener(v -> tap(screenCenterX(), screenCenterY())); // 示例：点屏幕中点
        wm.addView(overlay, lp);
    }

    private int screenCenterX(){ return getResources().getDisplayMetrics().widthPixels/2; }
    private int screenCenterY(){ return getResources().getDisplayMetrics().heightPixels/2; }

    public void tap(int x, int y) {
        Path p = new Path(); p.moveTo(x, y);
        GestureDescription.StrokeDescription s = new GestureDescription.StrokeDescription(p, 0, 60);
        dispatchGesture(new GestureDescription.Builder().addStroke(s).build(), null, null);
    }

    @Override public void onDestroy() {
        if (overlay != null) { wm.removeView(overlay); overlay = null; }
        super.onDestroy();
    }
}