package com.oopp.qrzd.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.oopp.qrzd.R;
import com.oopp.qrzd.service.AutoAccessibilityService;
import com.oopp.qrzd.service.CaptureService;

/**
 * 启停、阈值、ROI、帧率、日志级别等配置
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQ_CAPTURE = 1001;

    private MediaProjectionManager mpm;

    // 简单参数输入框
    private EditText etFps, etRoiX, etRoiY, etRoiW, etRoiH, etThresh;
    private CheckBox cbVerbose;

    private Button btnAcc, btnStart, btnStop, btnSave;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);

        mpm = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        etFps   = findViewById(R.id.et_fps);
        etRoiX  = findViewById(R.id.et_roi_x);
        etRoiY  = findViewById(R.id.et_roi_y);
        etRoiW  = findViewById(R.id.et_roi_w);
        etRoiH  = findViewById(R.id.et_roi_h);
        etThresh= findViewById(R.id.et_thresh);
        cbVerbose = findViewById(R.id.cb_verbose);

        btnAcc  = findViewById(R.id.btn_acc);
        btnStart= findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        btnSave = findViewById(R.id.btn_save);

        // 载入已保存参数
        loadPrefs();

        btnAcc.setOnClickListener(v -> openAccessibilitySettings());

        btnSave.setOnClickListener(v -> {
            savePrefs();
            Toast.makeText(this, "参数已保存", Toast.LENGTH_SHORT).show();
        });

        btnStart.setOnClickListener(v -> {
            if (!isAccessibilityServiceEnabled(this, AutoAccessibilityService.class)) {
                Toast.makeText(this, "请先开启无障碍服务", Toast.LENGTH_SHORT).show();
                openAccessibilitySettings();
                return;
            }
            // 请求 MediaProjection 录屏授权
            try {
                startActivityForResult(mpm.createScreenCaptureIntent(), REQ_CAPTURE);
            } catch (Exception e) {
                Toast.makeText(this, "无法请求录屏权限：" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        btnStop.setOnClickListener(v -> {
            stopService(new Intent(this, CaptureService.class));
            Toast.makeText(this, "已停止采集", Toast.LENGTH_SHORT).show();
        });
    }

    // 录屏授权回调：启动前台采集服务
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CAPTURE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                // 先保存参数，服务启动时可读取
                savePrefs();

                Intent svc = new Intent(this, CaptureService.class)
                        .putExtra("resultCode", resultCode)
                        .putExtra("data", data)
                        // 也可以把必要的运行参数通过 Intent 传递给服务：
                        .putExtra("fps", safeInt(etFps, 12))
                        .putExtra("roi_x", safeFloat(etRoiX, 0.70f))
                        .putExtra("roi_y", safeFloat(etRoiY, 0.75f))
                        .putExtra("roi_w", safeFloat(etRoiW, 0.28f))
                        .putExtra("roi_h", safeFloat(etRoiH, 0.22f))
                        .putExtra("thresh", safeFloat(etThresh, 0.84f))
                        .putExtra("verbose", cbVerbose.isChecked());

                // 统一用 ContextCompat.startForegroundService，兼容 24+
                ContextCompat.startForegroundService(this, svc);
                Toast.makeText(this, "采集已启动", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "用户取消了录屏授权", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 打开系统无障碍设置
    private void openAccessibilitySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (Exception e) {
            Toast.makeText(this, "无法打开无障碍设置：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // 检查无障碍服务是否已启用
    public static boolean isAccessibilityServiceEnabled(Context ctx, Class<?> serviceCls) {
        try {
            int enabled = Settings.Secure.getInt(
                    ctx.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED, 0);
            if (enabled != 1) return false;

            String settingValue = Settings.Secure.getString(
                    ctx.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

            if (TextUtils.isEmpty(settingValue)) return false;

            final String target = new ComponentName(ctx, serviceCls).flattenToString();
            for (String s : settingValue.split(":")) {
                if (s.equalsIgnoreCase(target)) return true;
            }
        } catch (Exception ignore) {}
        return false;
    }

    // 读取/保存参数（SharedPreferences）
    private void loadPrefs() {
        var sp = getSharedPreferences("cfg", MODE_PRIVATE);
        etFps.setText(String.valueOf(sp.getInt("fps", 12)));
        etRoiX.setText(String.valueOf(sp.getFloat("roi_x", 0.70f)));
        etRoiY.setText(String.valueOf(sp.getFloat("roi_y", 0.75f)));
        etRoiW.setText(String.valueOf(sp.getFloat("roi_w", 0.28f)));
        etRoiH.setText(String.valueOf(sp.getFloat("roi_h", 0.22f)));
        etThresh.setText(String.valueOf(sp.getFloat("thresh", 0.84f)));
        cbVerbose.setChecked(sp.getBoolean("verbose", false));
    }

    private void savePrefs() {
        var sp = getSharedPreferences("cfg", MODE_PRIVATE).edit();
        sp.putInt("fps", safeInt(etFps, 12));
        sp.putFloat("roi_x", safeFloat(etRoiX, 0.70f));
        sp.putFloat("roi_y", safeFloat(etRoiY, 0.75f));
        sp.putFloat("roi_w", safeFloat(etRoiW, 0.28f));
        sp.putFloat("roi_h", safeFloat(etRoiH, 0.22f));
        sp.putFloat("thresh", safeFloat(etThresh, 0.84f));
        sp.putBoolean("verbose", cbVerbose.isChecked());
        sp.apply();
    }

    private int safeInt(EditText et, int def) {
        try { return Integer.parseInt(et.getText().toString().trim()); }
        catch (Exception e) { return def; }
    }

    private float safeFloat(EditText et, float def) {
        try { return Float.parseFloat(et.getText().toString().trim()); }
        catch (Exception e) { return def; }
    }
}