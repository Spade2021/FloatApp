
package com.spade2021.floatapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

    private Bitmap previousFrame;

    private int dp(float value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        windowManager = (WindowManager)
                getSystemService(WINDOW_SERVICE);

        projectionManager = (MediaProjectionManager)
                getSystemService(Context.MEDIA_PROJECTION_SERVICE);

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

    private int getWindowType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }

        return WindowManager.LayoutParams.TYPE_PHONE;
    }

    private GradientDrawable rounded(
            int color,
            int radius
    ) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private TextView makeText(
            String value,
            int size,
            int color
    ) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        return text;
    }

    private Button makeButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(12);
        button.setAllCaps(false);
        return button;
    }

    private void showFloatingButton() {
        if (floatingButton != null) return;

        View dot = new View(this);
        dot.setBackground(rounded(Color.WHITE, 50));
        dot.setElevation(dp(5));

        floatingButton = dot;

        WindowManager.LayoutParams params =
                new WindowManager.LayoutParams(
                        dp(20),
                        dp(20),
                        getWindowType(),
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT
                );

        params.gravity = Gravity.BOTTOM | Gravity.LEFT;
        params.x = dp(12);
        params.y = dp(18);

        dot.setOnClickListener(v -> {
            if (floatingPanel == null) {
                showFloatingPanel();
            } else {
                closeFloatingPanel();
            }
        });

        try {
            windowManager.addView(dot, params);
        } catch (Exception e) {
            Toast.makeText(this,
                    "Tombol mengambang gagal dibuat",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showFloatingPanel() {
        if (floatingPanel != null) return;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(14), dp(14), dp(14));
        root.setBackground(rounded(
                Color.rgb(24, 26, 32), 20
        ));
        root.setElevation(dp(12));

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);

        TextView title = makeText(
                "FLOAT APP",
                19,
                Color.WHITE
        );
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        header.addView(title,
                new LinearLayout.LayoutParams(0, dp(42), 1));

        Button close = makeButton("✕");
        header.addView(close,
                new LinearLayout.LayoutParams(dp(48), dp(42)));

        root.addView(header);

        // Browser address
        EditText address = new EditText(this);
        address.setSingleLine(true);
        address.setTextColor(Color.WHITE);
        address.setHintTextColor(Color.GRAY);
        address.setHint("Cari di Google atau masukkan URL");
        address.setTextSize(13);
        address.setPadding(dp(12), 0, dp(12), 0);
        address.setBackground(rounded(
                Color.rgb(43, 46, 55), 12
        ));

        LinearLayout.LayoutParams addressParams =
                new LinearLayout.LayoutParams(
                        -1, dp(46)
                );
        addressParams.topMargin = dp(10);
        root.addView(address, addressParams);

        // Search button
        Button search = makeButton("Cari / Buka");
        search.setBackground(rounded(
                Color.rgb(65, 105, 225), 12
        ));
        search.setTextColor(Color.WHITE);

        LinearLayout.LayoutParams searchParams =
                new LinearLayout.LayoutParams(
                        -1, dp(44)
                );
        searchParams.topMargin = dp(7);
        root.addView(search, searchParams);

        // Web browser
        webView = new WebView(this);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient());
        webView.setBackgroundColor(Color.WHITE);
        webView.loadUrl("https://www.google.com");

        LinearLayout.LayoutParams webParams =
                new LinearLayout.LayoutParams(
                        -1, 0, 1
                );
        webParams.topMargin = dp(10);
        webParams.bottomMargin = dp(10);

        root.addView(webView, webParams);

        // Live Screen title
        TextView liveTitle = makeText(
                "LIVE SCREEN",
                14,
                Color.WHITE
        );
        liveTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(liveTitle);

        screenStatus = makeText(
                "Belum dimulai",
                11,
                Color.LTGRAY
        );

        LinearLayout.LayoutParams statusParams =
                new LinearLayout.LayoutParams(
                        -1, dp(28)
                );
        statusParams.topMargin = dp(3);
        root.addView(screenStatus, statusParams);

        // Screen preview
        screenPreview = new ImageView(this);
        screenPreview.setBackground(rounded(
                Color.BLACK, 12
        ));
        screenPreview.setScaleType(
                ImageView.ScaleType.FIT_CENTER
        );

        root.addView(screenPreview,
                new LinearLayout.LayoutParams(
                        -1, dp(115)
                ));

        // Live buttons
        LinearLayout liveButtons = new LinearLayout(this);
        liveButtons.setOrientation(LinearLayout.HORIZONTAL);

        Button startLive = makeButton("Mulai Live");
        Button stopLive = makeButton("Stop");

        startLive.setTextColor(Color.WHITE);
        startLive.setBackground(rounded(
                Color.rgb(35, 145, 95), 12
        ));

        stopLive.setTextColor(Color.WHITE);
        stopLive.setBackground(rounded(
                Color.rgb(170, 55, 65), 12
        ));

        LinearLayout.LayoutParams liveButtonParams =
                new LinearLayout.LayoutParams(
                        0, dp(44), 1
                );
        liveButtonParams.setMargins(
                dp(3), dp(9), dp(3), 0
        );

        liveButtons.addView(startLive, liveButtonParams);
        liveButtons.addView(stopLive, liveButtonParams);

        root.addView(liveButtons);

        // Browser action
        search.setOnClickListener(v -> {
            String input = address.getText()
                    .toString().trim();

            if (input.isEmpty()) return;

            String url;

            if (input.startsWith("https://")
                    || input.startsWith("http://")) {
                url = input;
            } else if (input.contains(".")
                    && !input.contains(" ")) {
                url = "https://" + input;
            } else {
                url = "https://www.google.com/search?q="
                        + Uri.encode(input);
            }

            webView.loadUrl(url);
        });

        address.setOnEditorActionListener((v, actionId, event) -> {
            search.performClick();
            return true;
        });

        close.setOnClickListener(v -> closeFloatingPanel());

        startLive.setOnClickListener(v -> requestLiveScreen());
        stopLive.setOnClickListener(v -> stopLiveScreen());

        floatingPanel = root;

        int panelWidth = Math.min(
                dp(380),
                getResources().getDisplayMetrics().widthPixels - dp(32)
        );

        int panelHeight = Math.min(
                dp(620),
                getResources().getDisplayMetrics().heightPixels - dp(80)
        );

        WindowManager.LayoutParams panelParams =
                new WindowManager.LayoutParams(
                        panelWidth,
                        panelHeight,
                        getWindowType(),
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                        PixelFormat.TRANSLUCENT
                );

        panelParams.gravity = Gravity.CENTER;

        try {
            windowManager.addView(floatingPanel, panelParams);
        } catch (Exception e) {
            floatingPanel = null;
            Toast.makeText(this,
                    "Panel gagal dibuka",
                    Toast.LENGTH_SHORT).show();
        }
            }
                              
    private void requestLiveScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this,
                    "Izin overlay diperlukan",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent permissionIntent =
                    projectionManager.createScreenCaptureIntent();

            startActivityForResult(
                    permissionIntent,
                    REQUEST_SCREEN
            );

        } catch (Exception e) {
            if (screenStatus != null) {
                screenStatus.setText("Gagal meminta izin layar");
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

        if (requestCode == REQUEST_OVERLAY) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                    || Settings.canDrawOverlays(this)) {
                showFloatingButton();
            } else {
                Toast.makeText(this,
                        "Izin overlay belum diberikan",
                        Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == REQUEST_SCREEN) {
            if (resultCode == RESULT_OK && data != null) {
                startScreenService(resultCode, data);
            } else if (screenStatus != null) {
                screenStatus.setText("Izin Live Screen ditolak");
            }
        }
    }

    private void startScreenService(
            int resultCode,
            Intent resultData
    ) {
        try {
            ScreenCaptureService.listener =
                    new ScreenCaptureService.FrameListener() {

                @Override
                public void onFrame(Bitmap bitmap) {
                    runOnUiThread(() -> {
                        if (screenPreview == null
                                || floatingPanel == null) {
                            bitmap.recycle();
                            return;
                        }

                        if (previousFrame != null
                                && previousFrame != bitmap) {
                            previousFrame.recycle();
                        }

                        previousFrame = bitmap;
                        screenPreview.setImageBitmap(bitmap);

                        if (screenStatus != null) {
                            screenStatus.setText(
                                    "Live Screen aktif"
                            );
                        }
                    });
                }

                @Override
                public void onStopped(String message) {
                    runOnUiThread(() -> {
                        if (screenStatus != null) {
                            screenStatus.setText(message);
                        }
                    });
                }
            };

            Intent serviceIntent =
                    new Intent(this, ScreenCaptureService.class);

            serviceIntent.putExtra(
                    ScreenCaptureService.EXTRA_RESULT_CODE,
                    resultCode
            );

            serviceIntent.putExtra(
                    ScreenCaptureService.EXTRA_RESULT_DATA,
                    resultData
            );

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

            if (screenStatus != null) {
                screenStatus.setText(
                        "Menghubungkan Live Screen..."
                );
            }

        } catch (Exception e) {
            ScreenCaptureService.listener = null;

            if (screenStatus != null) {
                screenStatus.setText(
                        "Gagal memulai Live Screen"
                );
            }
        }
    }

    private void stopLiveScreen() {
        ScreenCaptureService.listener = null;

        try {
            stopService(new Intent(
                    this,
                    ScreenCaptureService.class
            ));
        } catch (Exception ignored) {
        }

        if (screenPreview != null) {
            screenPreview.setImageDrawable(null);
        }

        if (previousFrame != null) {
            previousFrame.recycle();
            previousFrame = null;
        }

        if (screenStatus != null) {
            screenStatus.setText("Live Screen dihentikan");
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
