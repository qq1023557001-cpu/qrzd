package com.oopp.qrzd.service;

import android.accessibilityservice.AccessibilityService;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.oopp.qrzd.service.component.DragMoveListener;
import com.oopp.qrzd.service.component.Gesture;

/**
 * 无障碍服务：派发点击/滑动/长按/曲线拖拽
 */
public class AutoAccessibilityService extends AccessibilityService {
    private WindowManager wm;
    private View overlayView;
    private WindowManager.LayoutParams lp;

    @Override public void onServiceConnected() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        createOverlay();
    }

    private void createOverlay() {
        if (overlayView != null) return;
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_bubble, null);

        int type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
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

        // 拖动小球
        overlayView.setOnTouchListener(new DragMoveListener(wm, lp));

        // 点击触发一次“开始/领取”等动作（示例）
        overlayView.findViewById(R.id.btnGo).setOnClickListener(v -> {
            Gesture.execTap(this, /*x*/ lastTargetX, /*y*/ lastTargetY);
        });

        wm.addView(overlayView, lp);
    }

    @Override
    public void onUnbind(Intent intent) {
        if (overlayView != null) { wm.removeView(overlayView); overlayView = null; }
        super.onUnbind(intent);
    }


}
