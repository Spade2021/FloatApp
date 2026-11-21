
package com.spade2021.floatapp;

import android.Manifest;
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
import android.content.pm.PackageManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;

import androidx.activity.ComponentActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends ComponentActivity {

    private WindowManager wm;
    private LinearLayout panel;
    private WebView web;
    private TextView resultText;
    private PreviewView previewView;
    private Button liveButton;

    private ProcessCameraProvider cameraProvider;
    private ExecutorService cameraExecutor;
    private TextRecognizer recognizer;

    private final AtomicBoolean processing =
        new AtomicBoolean(false);

    private static final int PICK_IMAGE = 100;
    private static final int CAMERA_PERMISSION = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cameraExecutor = Executors.newSingleThreadExecutor();

        recognizer = TextRecognition.getClient(
            TextRecognizerOptions.DEFAULT_OPTIONS
        );

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
        wm = (WindowManager)
            getSystemService(WINDOW_SERVICE);

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

        floatButton.setOnClickListener(
            v -> showFloatingBrowser()
        );
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

        Button galleryButton = new Button(this);
        galleryButton.setText("GALERI");

        liveButton = new Button(this);
        liveButton.setText("LIVE CAM");

        Button copyButton = new Button(this);
        copyButton.setText("SALIN");

        Button closeButton = new Button(this);
        closeButton.setText("TUTUP");

        Button[] buttonList = {
            galleryButton,
            liveButton,
            copyButton,
            closeButton
        };

        for (Button button : buttonList) {
            buttons.addView(button,
                new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1
                )
            );
        }

        web = new WebView(this);
        web.setWebViewClient(new WebViewClient());

        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        TextView googleTitle = new TextView(this);
        googleTitle.setText("GOOGLE BROWSER");
        googleTitle.setTextColor(0xFF202124);
        googleTitle.setTextSize(12);
        googleTitle.setPadding(6, 5, 6, 5);

        previewView = new PreviewView(this);
        previewView.setImplementationMode(
            PreviewView.ImplementationMode.COMPATIBLE
        );
        previewView.setVisibility(View.GONE);

        TextView cameraTitle = new TextView(this);
        cameraTitle.setText("LIVE CAMERA");
        cameraTitle.setTextColor(0xFF202124);
        cameraTitle.setTextSize(12);
        cameraTitle.setPadding(6, 5, 6, 5);
        cameraTitle.setVisibility(View.GONE);

        resultText = new TextView(this);
        resultText.setText("Hasil scan akan muncul di sini.");
        resultText.setTextColor(0xFF000000);
        resultText.setTextSize(13);
        resultText.setPadding(8, 6, 8, 6);

        ScrollView resultScroll = new ScrollView(this);
        resultScroll.addView(resultText);

        panel.addView(address);
        panel.addView(buttons);

        panel.addView(googleTitle);

        panel.addView(web,
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
            )
        );

        panel.addView(cameraTitle);

        panel.addView(previewView,
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
            )
        );

        panel.addView(resultScroll,
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                85
            )
        );

        int width = (int) (
            getResources().getDisplayMetrics().widthPixels
            * 0.94f
        );

        int height = (int) (
            getResources().getDisplayMetrics().heightPixels
            * 0.85f
        );

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

        address.setOnEditorActionListener(
            (v, actionId, event) -> {

                String url = address.getText()
                    .toString().trim();

                if (!url.startsWith("http://")
                    && !url.startsWith("https://")) {
                    url = "https://" + url;
                }

                web.loadUrl(url);
                return true;
            }
        );

        galleryButton.setOnClickListener(
            v -> selectImage()
        );

        liveButton.setOnClickListener(v -> {

            if (previewView.getVisibility()
                == View.VISIBLE) {

                stopLiveCamera();

                previewView.setVisibility(View.GONE);
                cameraTitle.setVisibility(View.GONE);

                liveButton.setText("LIVE CAM");

            } else {
                startLiveCamera();
            }
        });

        copyButton.setOnClickListener(v -> {

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

        closeButton.setOnClickListener(v -> {

            stopLiveCamera();

            if (web != null) {
                web.destroy();
            }

            wm.removeView(panel);

            panel = null;
            web = null;
            previewView = null;
            resultText = null;
            liveButton = null;
        });
    }

    private void startLiveCamera() {

        if (previewView == null) return;

        if (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                new String[]{
                    Manifest.permission.CAMERA
                },
                CAMERA_PERMISSION
            );

            return;
        }

        previewView.setVisibility(View.VISIBLE);

        resultText.setText(
            "Menyiapkan kamera..."
        );

        ListenableFuture<ProcessCameraProvider> future =
            ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {

            try {
                cameraProvider = future.get();

                Preview preview =
                    new Preview.Builder().build();

                preview.setSurfaceProvider(
                    previewView.getSurfaceProvider()
                );

                ImageAnalysis analysis =
                    new ImageAnalysis.Builder()
                        .setBackpressureStrategy(
                            ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                        )
                        .build();

                analysis.setAnalyzer(
                    cameraExecutor,
                    this::scanCameraFrame
                );

                cameraProvider.unbindAll();

                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                );

                runOnUiThread(() -> {

                    if (previewView != null) {
                        previewView.setVisibility(
                            View.VISIBLE
                        );
                    }

                    if (liveButton != null) {
                        liveButton.setText("STOP CAM");
                    }

                    if (resultText != null) {
                        resultText.setText(
                            "Kamera aktif. Arahkan ke tulisan."
                        );
                    }
                });

            } catch (Exception e) {

                runOnUiThread(() -> {
                    if (resultText != null) {
                        resultText.setText(
                            "Kamera gagal: " + e.getMessage()
                        );
                    }
                });
            }

        }, ContextCompat.getMainExecutor(this));
    }

    private void scanCameraFrame(ImageProxy imageProxy) {

        if (processing.getAndSet(true)) {
            imageProxy.close();
            return;
        }

        android.media.Image mediaImage =
            imageProxy.getImage();

        if (mediaImage == null) {
            processing.set(false);
            imageProxy.close();
            return;
        }

        InputImage image =
            InputImage.fromMediaImage(
                mediaImage,
                imageProxy.getImageInfo()
                    .getRotationDegrees()
            );

        recognizer.process(image)
            .addOnSuccessListener(result -> {

                String text = result.getText();

                if (resultText != null && !text.isEmpty()) {
                    runOnUiThread(() -> {
                        if (resultText != null) {
                            resultText.setText(text);
                        }
                    });
                }
            })
            .addOnFailureListener(e -> {

                runOnUiThread(() -> {
                    if (resultText != null) {
                        resultText.setText(
                            "Gagal membaca: " + e.getMessage()
                        );
                    }
                });
            })
            .addOnCompleteListener(task -> {

                processing.set(false);
                imageProxy.close();
            });
    }

    private void stopLiveCamera() {

        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider = null;
        }

        if (liveButton != null) {
            liveButton.setText("LIVE CAM");
        }
    }

    private void selectImage() {

        Intent intent = new Intent(
            Intent.ACTION_GET_CONTENT
        );

        intent.setType("image/*");

        startActivityForResult(
            intent,
            PICK_IMAGE
        );
    }

    @Override
    public void onRequestPermissionsResult(
        int requestCode,
        String[] permissions,
        int[] grantResults
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        );

        if (requestCode == CAMERA_PERMISSION
            && grantResults.length > 0
            && grantResults[0]
                == PackageManager.PERMISSION_GRANTED) {

            startLiveCamera();

        } else if (requestCode == CAMERA_PERMISSION) {

            if (resultText != null) {
                resultText.setText(
                    "Izin kamera belum diberikan."
                );
            }
        }
    }

    @Override
    protected void onActivityResult(
        int requestCode,
        int resultCode,
        Intent data
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        );

        if (requestCode == PICK_IMAGE
            && resultCode == RESULT_OK
            && data != null
            && data.getData() != null) {

            Uri imageUri = data.getData();

            if (resultText != null) {
                resultText.setText(
                    "Sedang membaca gambar..."
                );
            }

            try {
                InputImage image =
                    InputImage.fromFilePath(
                        this,
                        imageUri
                    );

                recognizer.process(image)
                    .addOnSuccessListener(result -> {

                        if (resultText != null) {

                            String text = result.getText();

                            if (text.isEmpty()) {
                                resultText.setText(
                                    "Tidak ada tulisan ditemukan."
                                );
                            } else {
                                resultText.setText(text);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {

                        if (resultText != null) {
                            resultText.setText(
                                "Gagal membaca gambar: "
                                + e.getMessage()
                            );
                        }
                    });

            } catch (Exception e) {

                if (resultText != null) {
                    resultText.setText(
                        "Gagal membuka gambar: "
                        + e.getMessage()
                    );
                }
            }
        }
    }

    @Override
    protected void onDestroy() {

        stopLiveCamera();

        if (recognizer != null) {
            recognizer.close();
        }

        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }

        super.onDestroy();
    }
    }
            
