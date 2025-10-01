package com.oopp.qrzd.vision;

import android.graphics.Bitmap;

/**
 * OpenCV 模板匹配（图标/按钮）
 */
public interface TemplateMatcher {
    Optional<Match> match(Bitmap frame, Bitmap templ, Rect roi, double thr);
}