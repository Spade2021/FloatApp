package com.spade2021.floatapp;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

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

    private static final int PICK_IMAGE = 100;
    private static final int CAMERA_PERMISSION = 101;

    private static final int BG = 0xFF111522;
    private static final int PANEL = 0xFF20283A;
    private static final int WHITE = 0xFFF4F5FF;
    private static final int MUTED = 0xFF9CA8C5;

    private WindowManager windowManager;

    private View floatingButton;
    private LinearLayout panel;

    private WebView webView;
    private EditText addressInput;
    private TextView resultText;
    private PreviewView previewView;
    private TextView cameraHeading;
    private Button liveButton;

    private ProcessCameraProvider cameraProvider;
    private ExecutorService cameraExecutor;
    private TextRecognizer recognizer;

    private boolean cameraRequested = false;
    private boolean cameraStarting = false;
    private boolean cameraBound = false;
    private boolean activityResumed = false;

    private final AtomicBoolean processing =
            new AtomicBoolean(false);

    private int dp(float value) {
        return (int) (value *
                getResources().getDisplayMetrics().density);
    }

    private GradientDrawable rounded(
            int color,
            int radius
    ) {
        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));

        return drawable;
    }

    private TextView makeText(
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
        button.setBackground(
                rounded(0xFF303B58, 14)
        );

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

        windowManager = (WindowManager)
                getSystemService(WINDOW_SERVICE);

        checkOverlayPermission();
    }

    private void checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
            );

            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        activityResumed = true;

        if (Settings.canDrawOverlays(this)) {
            if (floatingButton == null) {
                showFloatingButton();
            }

            if (cameraRequested && previewView != null) {
                previewView.post(this::startLiveCamera);
            }
        }
    }

    @Override
    protected void onPause() {
        activityResumed = false;

        stopCameraBinding();

        super.onPause();
    }

    private int overlayWindowType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams
                    .TYPE_APPLICATION_OVERLAY;
        }

        return WindowManager.LayoutParams.TYPE_PHONE;
    }

    private void showFloatingButton() {
        if (floatingButton != null) return;

        TextView dot = makeText("●", 15, Color.BLACK);
        dot.setGravity(Gravity.CENTER);

        GradientDrawable circle =
                new GradientDrawable();

        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(0x66FFFFFF);
        circle.setStroke(dp(1), 0x99FFFFFF);

        dot.setBackground(circle);

        floatingButton = dot;

        WindowManager.LayoutParams params =
                new WindowManager.LayoutParams(
                        dp(42),
                        dp(42),
                        overlayWindowType(),
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT
                );

        params.gravity = Gravity.TOP | Gravity.RIGHT;
        params.x = dp(12);
        params.y = dp(35);

        windowManager.addView(floatingButton, params);

        floatingButton.setOnClickListener(
                view -> showFloatingBrowser()
        );
    }

    private void showFloatingBrowser() {
        if (panel != null) return;

        panel = vertical();

        panel.setPadding(
                dp(12), dp(12), dp(12), dp(12)
        );

        panel.setBackground(rounded(BG, 22));

        TextView heading =
                makeText("FloatApp", 23, WHITE);

        heading.setTypeface(null, Typeface.BOLD);

        TextView subtitle =
                makeText(
                        "SMART FLOATING WORKSPACE",
                        10,
                        MUTED
                );

        subtitle.setPadding(0, 0, 0, dp(10));

        panel.addView(heading);
        panel.addView(subtitle);

        addressInput = new EditText(this);
        addressInput.setSingleLine(true);
        addressInput.setTextColor(WHITE);
        addressInput.setHintTextColor(MUTED);
        addressInput.setHint("Cari atau masukkan alamat website");
        addressInput.setTextSize(13);

        addressInput.setPadding(
                dp(12), 0, dp(12), 0
        );

        addressInput.setBackground(rounded(PANEL, 13));
        addressInput.setImeOptions(EditorInfo.IME_ACTION_GO);

        panel.addView(
                addressInput,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(48)
                )
        );

        LinearLayout buttons = horizontal();

        Button galleryButton = makeButton("Galeri");
        liveButton = makeButton("Live Cam");
        Button copyButton = makeButton("Salin");
        Button closeButton = makeButton("Tutup");

        Button[] buttonList = {
                galleryButton,
                liveButton,
                copyButton,
                closeButton
        };

        for (Button button : buttonList) {
            LinearLayout.LayoutParams buttonParams =
                    new LinearLayout.LayoutParams(
                            0,
                            dp(44),
                            1
                    );

            buttonParams.setMargins(
                    dp(2), dp(5), dp(2), dp(5)
            );

            buttons.addView(button, buttonParams);
        }

        panel.addView(buttons);

        TextView browserHeading =
                makeText("BROWSER", 12, MUTED);

        browserHeading.setTypeface(null, Typeface.BOLD);
        browserHeading.setPadding(dp(5), dp(8), 0, dp(5));

        panel.addView(browserHeading);

        webView = new WebView(this);
        webView.setBackgroundColor(BG);
        webView.setWebViewClient(new WebViewClient());

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        String home =
                "<html><body style='background:#111522;" +
                "color:#f4f5ff;font-family:sans-serif;" +
                "padding:20px'>" +
                "<h2>Selamat datang di FloatApp</h2>" +
                "<p style='color:#9ca8c5'>" +
                "Browser dan pemindai teks dalam satu tempat." +
                "</p>" +
                "<p style='color:#9ca8c5'>" +
                "Masukkan alamat website untuk mulai menjelajah." +
                "</p></body></html>";

        webView.loadDataWithBaseURL(
                null, home, "text/html", "UTF-8", null
        );

        panel.addView(
                webView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        cameraHeading =
                makeText("LIVE CAMERA", 12, WHITE);

        cameraHeading.setTypeface(null, Typeface.BOLD);
        cameraHeading.setPadding(dp(5), dp(8), 0, dp(5));
        cameraHeading.setVisibility(View.GONE);

        panel.addView(cameraHeading);

        previewView = new PreviewView(this);

        previewView.setImplementationMode(
                PreviewView.ImplementationMode.COMPATIBLE
        );

        previewView.setScaleType(
                PreviewView.ScaleType.FILL_CENTER
        );

        previewView.setBackground(rounded(0xFF080B12, 16));
        previewView.setVisibility(View.GONE);

        panel.addView(
                previewView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(175)
                )
        );

        TextView resultHeading =
                makeText("HASIL OCR", 12, MUTED);

        resultHeading.setTypeface(null, Typeface.BOLD);
        resultHeading.setPadding(dp(5), dp(8), 0, dp(5));

        panel.addView(resultHeading);

        resultText = makeText(
                "Hasil pemindaian akan muncul di sini.",
                15,
                WHITE
        );

        resultText.setPadding(
                dp(12), dp(12), dp(12), dp(12)
        );

        resultText.setTextIsSelectable(true);

        ScrollView resultScroll = new ScrollView(this);
        resultScroll.setBackground(rounded(PANEL, 15));
        resultScroll.addView(resultText);

        panel.addView(
                resultScroll,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(135)
                )
        );

        int width = (int) (
                getResources().getDisplayMetrics().widthPixels
                        * 0.96f
        );

        int height = (int) (
                getResources().getDisplayMetrics().heightPixels
                        * 0.88f
        );

        WindowManager.LayoutParams params =
                new WindowManager.LayoutParams(
                        width,
                        height,
                        overlayWindowType(),
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                        PixelFormat.TRANSLUCENT
                );

        params.gravity = Gravity.CENTER;
        params.softInputMode =
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;

        windowManager.addView(panel, params);

        addressInput.setOnEditorActionListener(
                (view, actionId, event) -> {
                    openAddress();
                    return true;
                }
        );

        galleryButton.setOnClickListener(
                view -> selectImage()
        );

        liveButton.setOnClickListener(
                view -> toggleCamera()
        );

        copyButton.setOnClickListener(
                view -> copyResult()
        );

        closeButton.setOnClickListener(
                view -> closePanel()
        );
    }

    private void openAddress() {
        if (addressInput == null || webView == null) return;

        String address = addressInput.getText()
                .toString()
                .trim();

        if (address.isEmpty()) return;

        if (address.startsWith("http://")
                || address.startsWith("https://")) {

            webView.loadUrl(address);

        } else if (address.contains(".")
                && !address.contains(" ")) {

            webView.loadUrl("https://" + address);

        } else {
            webView.loadUrl(
                    "https://duckduckgo.com/?q="
                            + Uri.encode(address)
            );
        }
    }

    private void toggleCamera() {
        if (cameraRequested) {
            stopLiveCamera();

            if (previewView != null) {
                previewView.setVisibility(View.GONE);
            }

            if (cameraHeading != null) {
                cameraHeading.setVisibility(View.GONE);
            }

            if (liveButton != null) {
                liveButton.setText("Live Cam");
            }

        } else {
            cameraRequested = true;

            if (previewView != null) {
                previewView.setVisibility(View.VISIBLE);
            }

            if (cameraHeading != null) {
                cameraHeading.setVisibility(View.VISIBLE);
            }

            startLiveCamera();
        }
    }

    private void startLiveCamera() {
        if (!cameraRequested
                || !activityResumed
                || previewView == null) {
            return;
        }

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
                if (!cameraRequested
                        || !activityResumed
                        || previewView == null) {

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

        try {
            InputImage image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.getImageInfo().getRotationDegrees()
            );

            recognizer.process(image)
                    .addOnSuccessListener(result -> {
                        String recognized = result.getText();                        if (!recognized.trim().isEmpty()) {
                            runOnUiThread(() -> {
                                if (resultText != null) {
                                    resultText.setText(recognized);
                                }
                            });
                        }
                    })
                    .addOnFailureListener(error -> {
                        runOnUiThread(() -> {
                            if (resultText != null) {
                                resultText.setText(
                                        "OCR gagal: "
                                                + error.getMessage()
                                );
                            }
                        });
                    })
                    .addOnCompleteListener(task -> {
                        processing.set(false);
                        imageProxy.close();
                    });

        } catch (Exception e) {
            processing.set(false);
            imageProxy.close();
        }
    }

    private void stopCameraBinding() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }

        cameraBound = false;
        cameraStarting = false;
    }

    private void stopLiveCamera() {
        cameraRequested = false;
        stopCameraBinding();
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

        if (requestCode == CAMERA_PERMISSION) {
            if (grantResults.length > 0
                    && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED) {

                startLiveCamera();

            } else {
                cameraRequested = false;

                if (resultText != null) {
                    resultText.setText(
                            "Izin kamera belum diberikan."
                    );
                }
            }
        }
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");

        startActivityForResult(
                Intent.createChooser(intent, "Pilih gambar"),
                PICK_IMAGE
        );
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
                && resultCode == Activity.RESULT_OK
                && data != null
                && data.getData() != null) {

            Uri uri = data.getData();

            try {
                InputImage image =
                        InputImage.fromFilePath(this, uri);

                if (resultText != null) {
                    resultText.setText("Sedang membaca gambar...");
                }

                recognizer.process(image)
                        .addOnSuccessListener(result -> {
                            if (resultText != null) {
                                String text = result.getText();

                                resultText.setText(
                                        text.isEmpty()
                                                ? "Tidak ada teks ditemukan."
                                                : text
                                );
                            }
                        })
                        .addOnFailureListener(error -> {
                            if (resultText != null) {
                                resultText.setText(
                                        "Gagal membaca gambar: "
                                                + error.getMessage()
                                );
                            }
                        });

            } catch (Exception e) {
                if (resultText != null) {
                    resultText.setText(
                            "Gagal membuka gambar."
                    );
                }
            }
        }
    }

    private void copyResult() {
        if (resultText == null) return;

        String text = resultText.getText().toString();

        ClipboardManager clipboard =
                (ClipboardManager)
                        getSystemService(Context.CLIPBOARD_SERVICE);

        clipboard.setPrimaryClip(
                ClipData.newPlainText("Hasil OCR", text)
        );

        Toast.makeText(
                this,
                "Teks berhasil disalin",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void closePanel() {
        stopLiveCamera();

        if (panel != null) {
            windowManager.removeView(panel);
            panel = null;
        }

        webView = null;
        previewView = null;
        resultText = null;
        addressInput = null;
        cameraHeading = null;
        liveButton = null;
    }

    @Override
    protected void onDestroy() {
        stopLiveCamera();

        if (floatingButton != null) {
            windowManager.removeView(floatingButton);
            floatingButton = null;
        }

        if (panel != null) {
            windowManager.removeView(panel);
            panel = null;
        }

        if (recognizer != null) {
            recognizer.close();
        }

        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }

        super.onDestroy();
    }
}
