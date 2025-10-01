package com.oopp.qrzd.vision;

import android.graphics.Bitmap;

/**
 * ML Kit 文本识别（中/日/韩/拉丁可选）
 */
public interface OcrEngine {
    List<OcrWord> recognize(Bitmap bmp, Rect roi); // 输出文字与 bbox
}
