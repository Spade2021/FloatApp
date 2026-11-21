
package com.spade2021.floatapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;

public class MainActivity extends Activity {

    private static final int REQUEST_OVERLAY = 100;
    private static final int REQUEST_SCREEN = 200;

    private WindowManager windowManager;
    private View floatingButton;
    private View floatingPanel;

    private WebView webView;
    private ImageView screenPreview;
    private TextView screenStatus;

    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;

    private Handler mainHandler;
    private long lastFrameTime = 0;

    private int screenWidth;
    private int screenHeight;
    private int screenDensity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        projectionManager = (MediaProjectionManager)
                getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mainHandler = new Handler(Looper.getMainLooper());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(this)) {

            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
            );

            startActivityForResult(intent, REQUEST_OVERLAY);

        } else {
            showFloatingButton();
        }
    }

    private int dp(float value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    private int getWindowType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        return WindowManager.LayoutParams.TYPE_PHONE;
    }

    private void showFloatingButton() {
        if (floatingButton != null) return;

        TextView dot = new TextView(this);
        dot.setBackground(createWhiteCircle());
        dot.setText("");

        floatingButton = dot;

        WindowManager.LayoutParams params =
                new WindowManager.LayoutParams(
                        dp(18),
                        dp(18),
                        getWindowType(),
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT
                );

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = 0;

        dot.setOnClickListener(v -> showFloatingPanel());

        try {
            windowManager.addView(floatingButton, params);
        } catch (Exception e) {
            Toast.makeText(this, "Gagal membuat tombol", Toast.LENGTH_SHORT).show();
        }
    }

    private android.graphics.drawable.GradientDrawable createWhiteCircle() {
        android.graphics.drawable.GradientDrawable shape =
                new android.graphics.drawable.GradientDrawable();

        shape.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        shape.setColor(Color.WHITE);

        return shape;
    }

    private void showFloatingPanel() {
        if (floatingPanel != null) {
            return;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(12), dp(12), dp(12));
        root.setBackgroundColor(Color.rgb(25, 25, 25));

        TextView title = new TextView(this);
        title.setText("FLOAT APP");
        title.setTextColor(Color.WHITE);
        title.setTextSize(17);
        title.setPadding(0, 0, 0, dp(10));
        root.addView(title);

        EditText address = new EditText(this);
        address.setSingleLine(true);
        address.setTextColor(Color.WHITE);
        address.setHintTextColor(Color.LTGRAY);
        address.setHint("Masukkan alamat atau kata pencarian");
        address.setTextSize(14);
        root.addView(address);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);

        Button openButton = new Button(this);
        openButton.setText("BUKA");

        Button liveButton = new Button(this);
        liveButton.setText("LIVE SCREEN");

        Button closeButton = new Button(this);
        closeButton.setText("TUTUP");

        buttons.addView(openButton,
                new LinearLayout.LayoutParams(0, dp(48), 1));

        buttons.addView(liveButton,
                new LinearLayout.LayoutParams(0, dp(48), 1));

        buttons.addView(closeButton,
                new LinearLayout.LayoutParams(0, dp(48), 1));

        root.addView(buttons);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);
        webView.getSettings().setJavaScriptEnabled(false);
        webView.getSettings().setDomStorageEnabled(false);
        webView.setWebViewClient(new WebViewClient());
        webView.loadData(
                "<html><body style='font-family:sans-serif;padding:20px'>"
                        + "<h2>Float Browser</h2>"
                        + "<p>Masukkan alamat website atau kata pencarian di atas.</p>"
                        + "</body></html>",
                "text/html",
                "UTF-8"
        );

        root.addView(webView,
                new LinearLayout.LayoutParams(
                        -1, dp(260)
                ));

        screenStatus = new TextView(this);
        screenStatus.setText("Live Screen belum dimulai");
        screenStatus.setTextColor(Color.WHITE);
        screenStatus.setPadding(0, dp(10), 0, dp(5));
        root.addView(screenStatus);

        screenPreview = new ImageView(this);
        screenPreview.setBackgroundColor(Color.BLACK);
        screenPreview.setScaleType(ImageView.ScaleType.FIT_CENTER);

        root.addView(screenPreview,
                new LinearLayout.LayoutParams(-1, dp(190)));

        Button stopButton = new Button(this);
        stopButton.setText("STOP LIVE SCREEN");
        root.addView(stopButton);

        openButton.setOnClickListener(v -> {
            String input = address.getText().toString().trim();
            if (input.isEmpty()) return;

            String url;

            if (input.startsWith("https://") || input.startsWith("http://")) {
                url = input;
            } else if (input.contains(".") && !input.contains(" ")) {
                url = "https://" + input;
            } else {
                url = "https://duckduckgo.com/?q="
                        + Uri.encode(input);
            }

            webView.loadUrl(url);
        });

        liveButton.setOnClickListener(v -> requestLiveScreen());

        stopButton.setOnClickListener(v -> stopLiveScreen());

        closeButton.setOnClickListener(v -> closeFloatingPanel());

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);

        floatingPanel = scroll;

        WindowManager.LayoutParams panelParams =
                new WindowManager.LayoutParams(
                        dp(340),
                        dp(600),
                        getWindowType(),
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT
                );

        panelParams.gravity = Gravity.TOP | Gravity.LEFT;
        panelParams.x = dp(20);
        panelParams.y = dp(35);

        try {
            windowManager.addView(floatingPanel, panelParams);
        } catch (Exception e) {
            floatingPanel = null;
            Toast.makeText(this, "Panel gagal dibuka", Toast.LENGTH_SHORT).show();
        }
            }
                                
    private void requestLiveScreen() {
        if (mediaProjection != null) {
            screenStatus.setText("Live Screen sedang berjalan");
            return;
        }

        Intent intent = projectionManager.createScreenCaptureIntent();
        startActivityForResult(intent, REQUEST_SCREEN);
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_OVERLAY) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                    || Settings.canDrawOverlays(this)) {
                showFloatingButton();
            } else {
                Toast.makeText(this,
                        "Izin tombol mengambang diperlukan",
                        Toast.LENGTH_LONG).show();
            }
        }

        if (requestCode == REQUEST_SCREEN) {
            if (resultCode == RESULT_OK && data != null) {
                startLiveScreen(resultCode, data);
            } else if (screenStatus != null) {
                screenStatus.setText("Izin Live Screen ditolak");
            }
        }
    }

    private void startLiveScreen(int resultCode, Intent data) {
        try {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getRealMetrics(metrics);

            screenWidth = metrics.widthPixels;
            screenHeight = metrics.heightPixels;
            screenDensity = metrics.densityDpi;

            imageReader = ImageReader.newInstance(
                    screenWidth,
                    screenHeight,
                    PixelFormat.RGBA_8888,
                    2
            );

            mediaProjection = projectionManager.getMediaProjection(
                    resultCode,
                    data
            );

            if (mediaProjection == null) {
                screenStatus.setText("Live Screen gagal dimulai");
                return;
            }

            mediaProjection.registerCallback(
                    new MediaProjection.Callback() {
                        @Override
                        public void onStop() {
                            releaseCaptureResources();
                            mediaProjection = null;

                            if (screenStatus != null) {
                                screenStatus.setText("Live Screen berhenti");
                            }
                        }
                    },
                    mainHandler
            );

            imageReader.setOnImageAvailableListener(
                    reader -> displayLatestFrame(reader),
                    mainHandler
            );

            virtualDisplay = mediaProjection.createVirtualDisplay(
                    "FloatAppLiveScreen",
                    screenWidth,
                    screenHeight,
                    screenDensity,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.getSurface(),
                    null,
                    mainHandler
            );

            if (screenStatus != null) {
                screenStatus.setText("Live Screen sedang berjalan");
            }

        } catch (Exception e) {
            stopLiveScreen();

            if (screenStatus != null) {
                screenStatus.setText("Gagal memulai Live Screen");
            }
        }
    }

    private void displayLatestFrame(ImageReader reader) {
        Image image = null;

        try {
            image = reader.acquireLatestImage();
            if (image == null) return;

            long now = SystemClock.uptimeMillis();

            // Batasi pembaruan preview agar lebih ringan.
            if (now - lastFrameTime < 250) {
                return;
            }

            lastFrameTime = now;

            Image.Plane[] planes = image.getPlanes();
            if (planes.length == 0) return;

            Image.Plane plane = planes[0];

            ByteBuffer buffer = plane.getBuffer();
            int pixelStride = plane.getPixelStride();
            int rowStride = plane.getRowStride();

            int rowPadding = rowStride
                    - pixelStride * screenWidth;

            int paddedWidth = screenWidth
                    + rowPadding / pixelStride;

            Bitmap paddedBitmap = Bitmap.createBitmap(
                    paddedWidth,
                    screenHeight,
                    Bitmap.Config.ARGB_8888
            );

            buffer.rewind();
            paddedBitmap.copyPixelsFromBuffer(buffer);

            Bitmap finalBitmap = Bitmap.createBitmap(
                    paddedBitmap,
                    0,
                    0,
                    screenWidth,
                    screenHeight
            );

            paddedBitmap.recycle();

            if (screenPreview != null) {
                screenPreview.setImageBitmap(finalBitmap);
            } else {
                finalBitmap.recycle();
            }

        } catch (Exception e) {
            // Frame yang gagal dibaca akan dilewati.
        } finally {
            if (image != null) {
                image.close();
            }
        }
    }

    private void stopLiveScreen() {
        MediaProjection oldProjection = mediaProjection;
        mediaProjection = null;

        releaseCaptureResources();

        if (oldProjection != null) {
            try {
                oldProjection.stop();
            } catch (Exception ignored) {
            }
        }

        if (screenStatus != null) {
            screenStatus.setText("Live Screen dihentikan");
        }

        if (screenPreview != null) {
            screenPreview.setImageDrawable(null);
        }
    }

    private void releaseCaptureResources() {
        if (virtualDisplay != null) {
            try {
                virtualDisplay.release();
            } catch (Exception ignored) {
            }
            virtualDisplay = null;
        }

        if (imageReader != null) {
            try {
                imageReader.setOnImageAvailableListener(null, null);
                imageReader.close();
            } catch (Exception ignored) {
            }
            imageReader = null;
        }
    }

    private void closeFloatingPanel() {
        stopLiveScreen();

        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }

        if (floatingPanel != null) {
            try {
                windowManager.removeView(floatingPanel);
            } catch (Exception ignored) {
            }
            floatingPanel = null;
        }

        screenPreview = null;
        screenStatus = null;
    }

    @Override
    protected void onDestroy() {
        closeFloatingPanel();

        if (floatingButton != null) {
            try {
                windowManager.removeView(floatingButton);
            } catch (Exception ignored) {
            }
            floatingButton = null;
        }

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (floatingPanel != null) {
            closeFloatingPanel();
        } else {
            super.onBackPressed();
        }
    }
}
