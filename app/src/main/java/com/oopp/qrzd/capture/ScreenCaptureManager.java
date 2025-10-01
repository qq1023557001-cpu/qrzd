package com.oopp.qrzd.capture;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

/**
 * 屏幕采集
 * MediaProjection + VirtualDisplay + ImageReader
 */
public final class ScreenCaptureManager {
    public interface FrameListener { void onFrame(Bitmap bmp, long tsNanos); }
    public void start(Context ctx, int resultCode, Intent data,
                      int width, int height, int dpi, FrameListener l){

    }
    public void stop(){

    }
}