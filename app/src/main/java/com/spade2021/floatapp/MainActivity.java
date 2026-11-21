
package com.spade2021.floatapp;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.provider.Settings;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.widget.FrameLayout;

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
    private View floatButton;
    private LinearLayout panel;
    private WebView web;
    private TextView resultText;
    private PreviewView previewView;
    private Button liveButton;

    private ProcessCameraProvider cameraProvider;
    private ExecutorService cameraExecutor;
    private TextRecognizer recognizer;

    private boolean cameraRequested = false;
    private boolean cameraStarting = false;
    private boolean cameraBound = false;

    private final AtomicBoolean processing =
        new AtomicBoolean(false);

    private static final int PICK_IMAGE = 100;
    private static final int CAMERA_PERMISSION = 101;

    private final int BG = 0xFF111522;
    private final int PANEL = 0xFF20283A;
    private final int ACCENT = 0xFF7585FF;
    private final int WHITE = 0xFFF4F5FF;
    private final int MUTED = 0xFF9CA8C5;

    private int dp(float value) {
        return (int) (value *
            getResources().getDisplayMetrics().density);
    }

    private GradientDrawable shape(
        int color,
        int radius
    ) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private TextView text(
        String value,
        int size,
        int color
    ) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private Button makeButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(11);
        button.setAllCaps(false);
        button.setTextColor(WHITE);
        button.setBackground(shape(0xFF303B58, 14));
        return button;
    }

    private LinearLayout vertical() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout horizontal() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

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

        if (Settings.canDrawOverlays(this)) {
            if (wm == null) {
                showFloatingButton();
            }

            if (cameraRequested && previewView != null) {
                previewView.post(this::startLiveCamera);
            }
        }
    }

    private void showFloatingButton() {

        wm = (WindowManager)
            getSystemService(WINDOW_SERVICE);

        TextView dot = new TextView(this);
        dot.setText("●");
        dot.setTextSize(15);
        dot.setTextColor(Color.BLACK);
        dot.setGravity(Gravity.CENTER);

        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(0x66FFFFFF);
        circle.setStroke(dp(1), 0x99FFFFFF);

        dot.setBackground(circle);
        floatButton = dot;

        WindowManager.LayoutParams params =
            new WindowManager.LayoutParams(
                dp(42),
                dp(42),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            );

        params.gravity = Gravity.TOP | Gravity.RIGHT;
        params.x = dp(12);
        params.y = dp(35);

        wm.addView(floatButton, params);

        floatButton.setOnClickListener(
            v -> showFloatingBrowser()
        );
    }

    private void showFloatingBrowser() {

        if (panel != null) return;

        panel = vertical();
        panel.setPadding(dp(10), dp(10), dp(10), dp(10));
        panel.setBackground(shape(BG, 22));

        TextView heading = text("FloatApp", 23, WHITE);
        heading.setTypeface(null, Typeface.BOLD);

        TextView subtitle = text(
            "SMART FLOATING WORKSPACE",
            10,
            MUTED
        );

        subtitle.setPadding(0, 0, 0, dp(10));

        LinearLayout header = vertical();
        header.addView(heading);
        header.addView(subtitle);

        EditText address = new EditText(this);
        address.setSingleLine(true);
        address.setTextColor(WHITE);
        address.setHintTextColor(MUTED);
        address.setHint("Cari atau masukkan alamat website");
        address.setTextSize(13);
        address.setPadding(dp(12), 0, dp(12), 0);
        address.setBackground(shape(PANEL, 13));
        address.setImeOptions(EditorInfo.IME_ACTION_GO);

        LinearLayout buttons = horizontal();

        Button galleryButton = makeButton("Galeri");
        liveButton = makeButton("Live Cam");
        Button copyButton = makeButton("Salin");
        Button closeButton = makeButton("Tutup");

        Button[] buttonList = {
            galleryButton, liveButton, copyButton, closeButton
        };

        for (Button button : buttonList) {
            LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(
                    0,
                    dp(44),
                    1
                );
            p.setMargins(dp(2), dp(5), dp(2), dp(5));
            buttons.addView(button, p);
        }

        web = new WebView(this);
        web.setBackgroundColor(BG);
        web.setWebViewClient(new WebViewClient());

        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        String home =
            "<html><body style='background:#111522;color:#f4f5ff;" +
            "font-family:sans-serif;padding:20px'>" +
            "<h2>Selamat datang di FloatApp</h2>" +
            "<p style='color:#9ca8c5'>Browser dan alat pemindai teks " +
            "dalam satu tempat.</p>" +
            "<p style='color:#9ca8c5'>Masukkan alamat website di atas " +
            "untuk mulai menjelajah.</p>" +
            "</body></html>";

        web.loadDataWithBaseURL(
            null, home, "text/html", "UTF-8", null
        );

        TextView cameraHeading = text(
            "LIVE CAMERA",
            12,
            WHITE
        );
        cameraHeading.setTypeface(null, Typeface.BOLD);
        cameraHeading.setPadding(dp(5), dp(8), 0, dp(5));
        cameraHeading.setVisibility(View.GONE);

        previewView = new PreviewView(this);
        previewView.setImplementationMode(
            PreviewView.ImplementationMode.COMPATIBLE
        );
        previewView.setScaleType(
            PreviewView.ScaleType.FILL_CENTER
        );
        previewView.setBackground(shape(0xFF080B12, 16));
        previewView.setVisibility(View.GONE);

        resultText = text(
            "Hasil pemindaian akan muncul di sini.",
            15,
            WHITE
        );
        resultText.setPadding(dp(12), dp(12), dp(12), dp(12));
        resultText.setTextIsSelectable(true);

        ScrollView resultScroll = new ScrollView(this);
        resultScroll.setBackground(shape(PANEL, 15));
        resultScroll.addView(resultText);

        TextView resultHeading = text(
            "HASIL OCR",
            12,
            MUTED
        );
        resultHeading.setTypeface(null, Typeface.BOLD);
        resultHeading.setPadding(dp(5), dp(8), 0, dp(5));

        panel.addView(header);
        panel.addView(address);
        panel.addView(buttons);

        TextView browserHeading = text(
            "BROWSER",
            12,
            MUTED
        );
        browserHeading.setPadding(dp(5), dp(8), 0, dp(5));
        panel.addView(browserHeading);

        LinearLayout.LayoutParams webParams =
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                0.8f
            );

        panel.addView(web, webParams);
        panel.addView(cameraHeading);

        LinearLayout.LayoutParams cameraParams =
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1.3f
            );

        panel.addView(previewView, cameraParams);
        panel.addView(resultHeading);

        panel.addView(resultScroll,
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(155)
            )
        );

        int width = (int) (
            getResources().getDisplayMetrics().widthPixels * 0.96f
        );

        int height = (int) (
            getResources().getDisplayMetrics().heightPixels * 0.88f
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

        address.setOnEditorActionListener(
            (v, actionId, event) -> {
                String url = address.getText().toString().trim();

                if (!url.startsWith("http://")
                    && !url.startsWith("https://")) {
                    url = "https://" + url;
                }

                web.loadUrl(url);
                return true;
            }
        );

        galleryButton.setOnClickListener(v -> selectImage());

        liveButton.setOnClickListener(v -> {

            if (cameraRequested) {
                cameraRequested = false;
                stopLiveCamera();

                previewView.setVisibility(View.GONE);
                cameraHeading.setVisibility(View.GONE);

                liveButton.setText("Live Cam");

            } else {
                cameraRequested = true;

                previewView.setVisibility(View.VISIBLE);
                cameraHeading.setVisibility(View.VISIBLE);

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

            cameraRequested = false;
            stopLiveCamera();

            if (web != null) {
                web.destroy();
            }

            if (panel != null) {
                wm.removeView(panel);
            }

            panel = null;
            web = null;
            previewView = null;
            resultText = null;
            liveButton = null;
        });
    }

    private void startLiveCamera() {

        if (previewView == null || !cameraRequested) return;

        if (cameraStarting || cameraBound) return;

        if (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION
            );
            return;
        }

        cameraStarting = true;

        if (resultText != null) {
            resultText.setText("Menyiapkan kamera...");
        }

        ListenableFuture<ProcessCameraProvider> future =
            ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {

            try {
                if (!cameraRequested || previewView == null) {
                    cameraStarting = false;
                    return;
                }

                cameraProvider = future.get();

                Preview preview = new Preview.Builder().build();

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

                cameraBound = true;
                cameraStarting = false;

                runOnUiThread(() -> {
                    if (resultText != null) {
                        resultText.setText(
                            "Kamera aktif. Arahkan ke tulisan."
                        );
                    }

                    if (liveButton != null) {
                        liveButton.setText("Stop Cam");
                    }
                });

            } catch (Exception e) {

                cameraStarting = false;
                cameraBound = false;

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

        android.media.Image mediaImage = imageProxy.getImage();

        if (mediaImage == null) {
            processing.set(false);
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.getImageInfo().getRotationDegrees()
        );

        recognizer.process(image)
            .addOnSuccessListener(result -> {

                String recognized = result.getText();

                if (!recognized.isEmpty()) {
                    runOnUiThread(() -> {
                        if (resultText != null && cameraRequested) {
                            resultText.setText(recognized);
                        }
                    });
                }
            })
            .addOnFailureListener(e -> {
                runOnUiThread(() -> {
                    if (resultText != null && cameraRequested) {
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

        cameraStarting = false;
        cameraBound = false;

        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider = null;
        }

        if (liveButton != null) {
            liveButton.setText("Live Cam");
        }
    }

    private void selectImage() {

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");

        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    public void onRequestPermissionsResult(
        int requestCode,
        String[] permissions,
        int[] grantResults
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        );

        if (requestCode == CAMERA_PERMISSION
            && grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            startLiveCamera();

        } else if (requestCode == CAMERA_PERMISSION) {

            if (resultText != null) {
                resultText.setText("Izin kamera belum diberikan.");
            }
        }
    }

    @Override
    protected void onActivityResult(
        int requestCode,
        int resultCode,
        Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE
            && resultCode == Activity.RESULT_OK
            && data != null
            && data.getData() != null) {

            Uri imageUri = data.getData();

            if (resultText != null) {
                resultText.setText("Sedang membaca gambar...");
            }

            try {
                InputImage image = InputImage.fromFilePath(
                    this, imageUri
                );

                recognizer.process(image)
                    .addOnSuccessListener(result -> {

                        if (resultText != null) {
                            String recognized = result.getText();

                            resultText.setText(
                                recognized.isEmpty()
                                    ? "Tidak ada tulisan ditemukan."
                                    : recognized
                            );
                        }
                    })
                    .addOnFailureListener(e -> {

                        if (resultText != n
