package com.oopp.qrzd.input;

/**
 * dispatchGesture 封装（支持连段/曲线）
 */
public interface GestureExecutor {
    void tap(int x, int y);
    void swipe(int x1,int y1,int x2,int y2,long durMs);
    void dragPath(List<Point> pts, long totalMs); // 摇杆/技能拖拽
}
