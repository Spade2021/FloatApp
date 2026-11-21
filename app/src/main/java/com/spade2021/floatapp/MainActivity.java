
package com.spade2021.floatapp;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.view.inputmethod.EditorInfo;
import android.view.KeyEvent;
import android.widget.FrameLayout;
import android.widget.Toast;

public class MainActivity extends Activity {

    private WindowManager windowManager;
    private View floatingButton;
    private View panelView;
    private WindowManager.LayoutParams buttonParams;
    private WindowManager.LayoutParams panelParams;

    private EditText searchInput;
    private WebView webView;

    private boolean panelVisible = false;
    private boolean buttonAdded = false;

    private final int BG = Color.rgb(22, 24, 30);
    private final int PANEL = Color.rgb(31, 34, 42);
    private final int BLUE = Color.rgb(65, 125, 255);
    private final int WHITE = Color.WHITE;
    private final int MUTED = Color.rgb(165, 170, 185);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        TextView message = new TextView(this);
        message.setText("FloatApp\n\nAktifkan izin tampil di atas aplikasi lain agar tombol mengambang bisa digunakan.");
        message.setTextColor(Color.WHITE);
        message.setTextSize(16);
        message.setGravity(Gravity.CENTER);
        message.setPadding(24, 24, 24, 24);
        message.setBackgroundColor(BG);
        setContentView(message);

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

        if (Settings.canDrawOverlays(this) && !buttonAdded) {
            showFloatingButton();
        }
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private TextView makeText(String value, int size, int color) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setGravity(Gravity.CENTER_VERTICAL);
        return text;
    }

    private void showFloatingButton() {
        if (buttonAdded) return;

        TextView button = new TextView(this);
        button.setText("F");
        button.setTextSize(18);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(Color.rgb(25, 30, 45));
        button.setGravity(Gravity.CENTER);
        button.setBackground(rounded(WHITE, 50));
        floatingButton = button;

        buttonParams = new WindowManager.LayoutParams(
            dp(42),
            dp(42),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );

        buttonParams.gravity = Gravity.BOTTOM | Gravity.LEFT;
        buttonParams.x = dp(12);
        buttonParams.y = dp(110);

        button.setOnClickListener(v -> togglePanel());

        try {
            windowManager.addView(floatingButton, buttonParams);
            buttonAdded = true;
        } catch (Exception e) {
            Toast.makeText(this, "Tidak bisa menampilkan tombol mengambang", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void togglePanel() {
        if (panelVisible) {
            hidePanel();
        } else {
            showPanel();
        }
    }

    private void showPanel() {
        if (panelVisible) return;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(14), dp(16), dp(16));
        root.setBackground(rounded(BG, 20));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = makeText("FloatApp", 19, WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        TextView close = makeText("✕", 20, WHITE);
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(v -> hidePanel());

        header.addView(title, new LinearLayout.LayoutParams(0, dp(42), 1));
        header.addView(close, new LinearLayout.LayoutParams(dp(42), dp(42)));
        root.addView(header);

        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setTextColor(WHITE);
        searchInput.setHintTextColor(MUTED);
        searchInput.setHint("Cari di Google atau masukkan alamat");
        searchInput.setTextSize(14);
        searchInput.setPadding(dp(12), 0, dp(12), 0);
        searchInput.setBackground(rounded(PANEL, 12));
        searchInput.setImeOptions(EditorInfo.IME_ACTION_GO);

        LinearLayout.LayoutParams inputParams =
            new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            );
        inputParams.topMargin = dp(12);
        root.addView(searchInput, inputParams);

        Button searchButton = new Button(this);
        searchButton.setText("Cari / Buka");
        searchButton.setTextColor(WHITE);
        searchButton.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(BLUE)
        );

        LinearLayout.LayoutParams searchParams =
            new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(46)
            );
        searchParams.topMargin = dp(8);
        root.addView(searchButton, searchParams);

        FrameLayout browserContainer = new FrameLayout(this);
        browserContainer.setBackground(rounded(PANEL, 12));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        browserContainer.addView(
            webView,
            new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        );

        LinearLayout.LayoutParams browserParams =
            new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1
            );
        browserParams.topMargin = dp(12);
        root.addView(browserContainer, browserParams);

        searchButton.setOnClickListener(v -> performSearch());

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                performSearch();
                return true;
            }
            return false;
        });

        panelView = root;

        panelParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            dp(520),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        );

        panelParams.gravity = Gravity.CENTER;
        panelParams.width = dp(340);

        try {
            windowManager.addView(panelView, panelParams);
            panelVisible = true;
        } catch (Exception e) {
            panelView = null;
            Toast.makeText(this, "Panel gagal dibuka", Toast.LENGTH_SHORT).show();
        }
    }

    private void performSearch() {
        if (searchInput == null || webView == null) return;

        String query = searchInput.getText().toString().trim();

        if (query.isEmpty()) return;

        String url;

        if (query.startsWith("https://") || query.startsWith("http://")) {
            url = query;
        } else if (query.contains(".") && !query.contains(" ")) {
            url = "https://" + query;
        } else {
            try {
                url = "https://www.google.com/search?q=" +
                    Uri.encode(query);
            } catch (Exception e) {
                url = "https://www.google.com";
            }
        }

        webView.loadUrl(url);

        try {
            InputMethodManager imm =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
        } catch (Exception ignored) {
        }
    }

    private void hidePanel() {
        if (panelView != null) {
            try {
                windowManager.removeView(panelView);
            } catch (Exception ignored) {
            }
        }

        if (webView != null) {
            webView.destroy();
            webView = null;
        }

        panelView = null;
        searchInput = null;
        panelVisible = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        hidePanel();

        if (floatingButton != null && buttonAdded) {
            try {
                windowManager.removeView(floatingButton);
            } catch (Exception ignored) {
            }
        }

        buttonAdded = false;
    }
    }
                
