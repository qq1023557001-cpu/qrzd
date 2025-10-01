package com.oopp.qrzd.service.component;

import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class DragMoveListener implements View.OnTouchListener {
    private final WindowManager wm; private final WindowManager.LayoutParams lp;
    private int startX, startY; private float touchX, touchY;

    public DragMoveListener(WindowManager wm, WindowManager.LayoutParams lp) {
        this.wm = wm; this.lp = lp;
    }
    @Override public boolean onTouch(View v, MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = lp.x; startY = lp.y; touchX = e.getRawX(); touchY = e.getRawY(); return true;
            case MotionEvent.ACTION_MOVE:
                lp.x = startX + (int)(e.getRawX() - touchX);
                lp.y = startY + (int)(e.getRawY() - touchY);
                wm.updateViewLayout(v, lp); return true;
        }
        return false;
    }
}