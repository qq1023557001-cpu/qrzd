package com.oopp.qrzd.vision;

import android.content.Context;
import android.graphics.Bitmap;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;

public class MlkitOcrEngine {
    private final TextRecognizer zh;
    public MlkitOcrEngine(Context ctx){
        zh = TextRecognition.getClient(
                new ChineseTextRecognizerOptions.Builder().build());
    }
    public Text blockingRecognize(Bitmap bmp) throws Exception {
        InputImage img = InputImage.fromBitmap(bmp, 0);
        Task<Text> t1 = zh.process(img);
        return Tasks.await(t1);
    }
    // 简单懒加载单例
    public static final class MlkitHolder {
        private static MlkitOcrEngine s;
        public static synchronized MlkitOcrEngine eng(Context c){
            if (s==null) {
                s=new MlkitOcrEngine(c.getApplicationContext());
            }
            return s;
        }
    }
}
