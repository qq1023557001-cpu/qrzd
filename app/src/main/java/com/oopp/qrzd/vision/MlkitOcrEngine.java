package com.oopp.qrzd.vision;

import android.content.Context;
import android.graphics.Bitmap;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.concurrent.CompletableFuture;

public class MlkitOcrEngine {
    private final TextRecognizer zh, latin;
    public MlkitOcrEngine(Context ctx){
        zh = TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
        latin = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    }
    public com.google.mlkit.vision.text.Text blockingRecognize(Bitmap bmp) throws Exception {
        com.google.mlkit.vision.common.InputImage img = InputImage.fromBitmap(bmp, 0);
        // 中文优先，没识别到再拉丁
        com.google.android.gms.tasks.Task<Text> t1 = zh.process(img);
        Text r1 = com.google.android.gms.tasks.Tasks.await(t1);
        if (r1 != null && r1.getText() != null && r1.getText().length() > 0) return r1;
        return com.google.android.gms.tasks.Tasks.await(latin.process(img));
    }
    // 简单懒加载单例
    public static final class MlkitHolder {
        private static MlkitOcrEngine s;
        public static synchronized MlkitOcrEngine eng(Context c){ if (s==null) s=new MlkitOcrEngine(c.getApplicationContext()); return s; }
    }
}
