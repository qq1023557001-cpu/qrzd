package com.oopp.qrzd.vision;

import android.graphics.Bitmap;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.concurrent.CompletableFuture;

public class MlkitOcrEngine {
    private final TextRecognizer zh = TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
    private final TextRecognizer latin = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

    public CompletableFuture<Text> recognize(Bitmap bmp) {
        CompletableFuture<Text> f = new CompletableFuture<>();
        InputImage img = InputImage.fromBitmap(bmp, 0);
        zh.process(img).addOnSuccessListener(r -> {
            if (r != null && !r.getText().isEmpty()) f.complete(r);
            else latin.process(img).addOnSuccessListener(f::complete).addOnFailureListener(f::completeExceptionally);
        }).addOnFailureListener(e -> latin.process(img).addOnSuccessListener(f::complete).addOnFailureListener(f::completeExceptionally));
        return f;
    }
}