
package com.spade2021.floatapp;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.provider.Settings;
import android.net.Uri;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.view.ViewGroup;
import android.view.View;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class MainActivity extends Activity {

    private WindowManager wm;
    private LinearLayout panel;
    private WebView web;
    private TextView resultText;

    private static final int PICK_IMAGE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())
            );
            startActivity(intent);
            return;
        }

        showFloatingButton();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Settings.canDrawOverlays(this) && wm == null) {
            showFloatingButton();
        }
    }

    private void showFloatingButton() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        Button floatButton = new Button(this);
        floatButton.setText("FLOAT");

        WindowManager.LayoutParams params =
            new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 30;
        params.y = 200;

        wm.addView(floatButton, params);

        floatButton.setOnClickListener(v -> showFloatingBrowser());
    }

    private void showFloatingBrowser() {
        if (panel != null) return;

        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(8, 8, 8, 8);
        panel.setBackgroundColor(0xFFFFFFFF);

        EditText address = new EditText(this);
        address.setSingleLine(true);
        address.setText("https://www.google.com");
        address.setHint("Masukkan alamat website");
        address.setImeOptions(EditorInfo.IME_ACTION_GO);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);

        Button scan = new Button(this);
        scan.setText("SCAN SOAL");

        Button copy = new Button(this);
        copy.setText("SALIN");

        Button close = new Button(this);
        close.setText("TUTUP");

        buttons.addView(scan, new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1
        ));

        buttons.addView(copy, new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1
        ));

        buttons.addView(close, new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1
        ));

        resultText = new TextView(this);
        resultText.setText("Hasil pemindaian akan muncul di sini.");
        resultText.setTextColor(0xFF000000);
        resultText.setTextSize(14);
        resultText.setPadding(8, 8, 8, 8);

        ScrollView resultScroll = new ScrollView(this);
        resultScroll.addView(resultText);

        web = new WebView(this);
        web.setWebViewClient(new WebViewClient());

        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        panel.addView(address);
        panel.addView(buttons);

        panel.addView(resultScroll,
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                150
            )
        );

        panel.addView(web, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0, 1
        ));

        int width = (int) (getResources()
            .getDisplayMetrics().widthPixels * 0.90f);

        int height = (int) (getResources()
            .getDisplayMetrics().heightPixels * 0.75f);

        WindowManager.LayoutParams params =
            new WindowManager.LayoutParams(
                width,
                height,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            );

        params.gravity = Gravity.CENTER;

        wm.addView(panel, params);

        web.loadUrl("https://www.google.com");

        address.setOnEditorActionListener((v, actionId, event) -> {
            String url = address.getText().toString().trim();

            if (!url.startsWith("http://")
                    && !url.startsWith("https://")) {
                url = "https://" + url;
            }

            web.loadUrl(url);
            return true;
        });

        scan.setOnClickListener(v -> selectImage());

        copy.setOnClickListener(v -> {
            ClipboardManager clipboard =
                (ClipboardManager) getSystemService(
                    Context.CLIPBOARD_SERVICE
                );

            clipboard.setPrimaryClip(
                ClipData.newPlainText(
                    "Hasil OCR",
                    resultText.getText().toString()
                )
            );
        });

        close.setOnClickListener(v -> {
            web.destroy();
            wm.removeView(panel);
            panel = null;
            web = null;
            resultText = null;
        });
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(
        int requestCode,
        int resultCode,
        Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {

            Uri imageUri = data.getData();

            if (resultText != null) {
                resultText.setText("Sedang membaca tulisan...");
            }

            try {
                InputImage image =
                    InputImage.fromFilePath(this, imageUri);

                TextRecognizer recognizer =
                    TextRecognition.getClient(
                        TextRecognizerOptions.DEFAULT_OPTIONS
                    );

                recognizer.process(image)
                    .addOnSuccessListener(result -> {
                        if (resultText != null) {
                            String text = result.getText();

                            if (text.isEmpty()) {
                                resultText.setText(
                                    "Tidak ada tulisan yang ditemukan."
                                );
                            } else {
                                resultText.setText(text);
                            }
                        }
                        recognizer.close();
                    })
                    .addOnFailureListener(e -> {
                        if (resultText != null) {
                            resultText.setText(
                                "Gagal membaca gambar: " +
                                e.getMessage()
                            );
                        }
                        recognizer.close();
                    });

            } catch (Exception e) {
                if (resultText != null) {
                    resultText.setText(
                        "Gagal membuka gambar: " + e.getMessage()
                    );
                }
            }
        }
    }
}
