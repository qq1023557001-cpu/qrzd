package com.oopp.qrzd.service.component;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

public class DragMoveListener implements View.OnTouchListener {

    public interface Callback {
        void onClick(View v);
        void onLongPress(View v);
    }

    private final WindowManager wm;
    private final WindowManager.LayoutParams lp;
    private final int touchSlop;
    private final long longPressTimeout;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Callback cb;

    private int startX, startY;
    private float downX, downY;
    private boolean moved;
    private boolean longPressed;

    public DragMoveListener(Context ctx, WindowManager wm, WindowManager.LayoutParams lp, Callback cb) {
        this.wm = wm;
        this.lp = lp;
        this.cb = cb;
        this.touchSlop = ViewConfiguration.get(ctx).getScaledTouchSlop();
        this.longPressTimeout = ViewConfiguration.getLongPressTimeout(); // ~500ms
    }

    private final Runnable longPressRunnable = new Runnable() {
        @Override public void run() {
            longPressed = true;
            // 转交给使用方；若想走 View 的 onLongClick 也可 v.performLongClick()
            if (targetView != null && cb != null) cb.onLongPress(targetView);
        }
    };
    private View targetView;

    @Override
    public boolean onTouch(View v, MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                targetView = v;
                startX = lp.x; startY = lp.y;
                downX = e.getRawX(); downY = e.getRawY();
                moved = false; longPressed = false;
                handler.postDelayed(longPressRunnable, longPressTimeout);
                return true; // 消费事件（我们自己处理点击/长按/拖动）

            case MotionEvent.ACTION_MOVE:
                int dx = Math.round(e.getRawX() - downX);
                int dy = Math.round(e.getRawY() - downY);
                if (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop) {
                    moved = true;
                    handler.removeCallbacks(longPressRunnable); // 拖动则不再判定长按
                    lp.x = startX + dx;
                    lp.y = startY + dy;
                    wm.updateViewLayout(v, lp);
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(longPressRunnable);
                if (!moved && !longPressed && cb != null) {
                    // 视为“单击”
                    v.performClick(); // 无障碍友好
                    cb.onClick(v);
                }
                targetView = null;
                return true;
        }
        return false;
    }
}