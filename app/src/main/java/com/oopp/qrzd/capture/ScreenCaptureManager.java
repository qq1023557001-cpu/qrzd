package com.oopp.qrzd.capture;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.Looper;

import java.nio.ByteBuffer;

/**
 * 屏幕采集
 * MediaProjection + VirtualDisplay + ImageReader
 */
public class ScreenCaptureManager {
    public interface FrameListener { void onFrame(Bitmap bmp, long tsNanos); }
    private MediaProjection projection;
    private ImageReader reader;

    public void start(Context ctx, int resultCode, Intent data, int w, int h, int dpi, FrameListener l) {
        MediaProjectionManager m = (MediaProjectionManager) ctx.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        projection = m.getMediaProjection(resultCode, data);
        reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2);
        projection.createVirtualDisplay("cap", w, h, dpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader.getSurface(), null, null);
        reader.setOnImageAvailableListener(r -> {
            Image img = r.acquireLatestImage();
            if (img == null) return;
            try {
                Image.Plane p = img.getPlanes()[0];
                int rowStride = p.getRowStride();
                int pixelStride = p.getPixelStride(); // 4
                ByteBuffer buf = p.getBuffer();
                Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                bmp.copyPixelsFromBuffer(buf); // 简化处理；如有 stride 差异需手动行拷贝
                l.onFrame(bmp, img.getTimestamp());
            } finally { img.close(); }
        }, new Handler(Looper.getMainLooper()));
    }
    public void stop() { try { if (reader!=null) reader.close(); } finally { if (projection!=null) projection.stop(); } }
}