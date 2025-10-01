package com.oopp.qrzd.service.component;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;

public final class Gesture {
    public static void execTap(AccessibilityService svc, int x, int y) {
        Path p = new Path(); p.moveTo(x, y);
        GestureDescription.StrokeDescription sd =
                new GestureDescription.StrokeDescription(p, 0, 60);
        svc.dispatchGesture(new GestureDescription.Builder().addStroke(sd).build(), null, null);
    }
}

