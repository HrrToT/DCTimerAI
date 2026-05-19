package com.dctimer.activity;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.dctimer.R;
import com.dctimer.util.SmartCubeLogoProvider;
import com.dctimer.view.SmartCubeLogoCropView;

public class SmartCubeLogoCropActivity extends AppCompatActivity {
    public static final String EXTRA_INPUT_URI = "input_uri";
    public static final String EXTRA_OUTPUT_URI = "output_uri";

    private SmartCubeLogoCropView cropView;
    private Bitmap cropSourceBitmap;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_cube_logo_crop);

        cropView = findViewById(R.id.smart_cube_logo_crop_view);
        Button cancelButton = findViewById(R.id.btn_cancel);
        Button applyButton = findViewById(R.id.btn_done);

        String inputUri = getIntent().getStringExtra(EXTRA_INPUT_URI);
        if (TextUtils.isEmpty(inputUri)) {
            Toast.makeText(this, R.string.smart_cube_logo_upload_failed, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        try {
            cropSourceBitmap = SmartCubeLogoProvider.loadCropSourceBitmap(this, inputUri);
            if (cropSourceBitmap == null) {
                throw new IllegalStateException("crop source bitmap is null");
            }
            cropView.setBitmap(cropSourceBitmap);
        } catch (Exception e) {
            Toast.makeText(this, R.string.smart_cube_logo_upload_failed, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyCropResult();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (cropSourceBitmap != null && !cropSourceBitmap.isRecycled()) {
            cropSourceBitmap.recycle();
            cropSourceBitmap = null;
        }
        super.onDestroy();
    }

    private void applyCropResult() {
        try {
            Bitmap croppedBitmap = cropView.exportCroppedLogo();
            if (croppedBitmap == null) {
                throw new IllegalStateException("cropped bitmap is null");
            }
            String outputUri = SmartCubeLogoProvider.saveCustomLogoBitmap(this, croppedBitmap);
            croppedBitmap.recycle();
            android.content.Intent data = new android.content.Intent();
            data.putExtra(EXTRA_OUTPUT_URI, outputUri);
            setResult(RESULT_OK, data);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, R.string.smart_cube_logo_upload_failed, Toast.LENGTH_SHORT).show();
        }
    }
}
