
package com.spade2021.floatapp;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.EditorInfo;
import android.view.KeyEvent;
import android.widget.*;
import android.webkit.*;
import android.text.TextWatcher;
import android.text.Editable;

public class MainActivity extends Activity {

    private WindowManager wm;
    private View floatingButton, panelView;
    private EditText searchInput;
    private WebView webView;

    private boolean buttonAdded = false;
    private boolean panelVisible = false;
    private boolean useDuckDuckGo = false;

    private SharedPreferences prefs;

    private final int BG = Color.rgb(20, 22, 29);
    private final int PANEL = Color.rgb(34, 37, 46);
    private final int BLUE = Color.rgb(65, 125, 255);
    private final int WHITE = Color.WHITE;
    private final int MUTED = Color.rgb(165, 170, 185);

    private int dp(float value) {
        return (int) (value * getResources()
            .getDisplayMetrics().density);
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        return shape;
    }

    private void saveValue(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("FloatAppData", MODE_PRIVATE);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        TextView message = new TextView(this);
        message.setText("FloatApp\n\nAktifkan izin tampil di atas aplikasi lain.");
        message.setTextColor(WHITE);
        message.setTextSize(16);
        message.setGravity(Gravity.CENTER);
        message.setPadding(dp(24), dp(24), dp(24), dp(24));
        message.setBackgroundColor(BG);
        setContentView(message);

        checkPermission();
    }

    private void checkPermission() {
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

    private void showFloatingButton() {
        if (buttonAdded) return;

        TextView button = new TextView(this);
        button.setBackground(rounded(WHITE, 50));
        floatingButton = button;

        WindowManager.LayoutParams params =
            new WindowManager.LayoutParams(
                dp(42), dp(42),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            );

        params.gravity = Gravity.BOTTOM | Gravity.LEFT;
        params.x = dp(12);
        params.y = dp(110);

        button.setOnClickListener(v -> togglePanel());

        try {
            wm.addView(floatingButton, params);
            buttonAdded = true;
        } catch (Exception e) {
            Toast.makeText(this, "Tombol gagal ditampilkan",
                Toast.LENGTH_SHORT).show();
        }
    }

    private void togglePanel() {
        if (panelVisible) hidePanel();
        else showPanel();
    }

    private void showPanel() {
        if (panelVisible) return;

        useDuckDuckGo = prefs.getBoolean("duckduckgo", false);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(14), dp(16), dp(16));
        root.setBackground(rounded(BG, 20));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("FloatApp");
        title.setTextSize(19);
        title.setTextColor(WHITE);
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView close = new TextView(this);
        close.setText("✕");
        close.setTextSize(20);
        close.setTextColor(WHITE);
        close.setGravity(Gravity.CENTER);

        header.addView(title, new LinearLayout.LayoutParams(0, dp(42), 1));
        header.addView(close, new LinearLayout.LayoutParams(dp(42), dp(42)));
        close.setOnClickListener(v -> hidePanel());
        root.addView(header);

        LinearLayout engineButtons = new LinearLayout(this);
        engineButtons.setOrientation(LinearLayout.HORIZONTAL);

        Button googleButton = new Button(this);
        googleButton.setText("Google");
        googleButton.setTextColor(WHITE);

        Button duckButton = new Button(this);
        duckButton.setText("DuckDuckGo");
        duckButton.setTextColor(WHITE);

        LinearLayout.LayoutParams engineParams =
            new LinearLayout.LayoutParams(0, dp(46), 1);
        engineParams.topMargin = dp(12);
        engineParams.rightMargin = dp(4);
        engineButtons.addView(googleButton, engineParams);

        LinearLayout.LayoutParams duckParams =
            new LinearLayout.LayoutParams(0, dp(46), 1);
        duckParams.topMargin = dp(12);
        duckParams.leftMargin = dp(4);
        engineButtons.addView(duckButton, duckParams);

        root.addView(engineButtons);

        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setTextColor(WHITE);
        searchInput.setHintTextColor(MUTED);
        searchInput.setHint("Cari atau masukkan alamat website");
        searchInput.setTextSize(14);
        searchInput.setPadding(dp(12), 0, dp(12), 0);
        searchInput.setBackground(rounded(PANEL, 12));
        searchInput.setImeOptions(EditorInfo.IME_ACTION_GO);

        String savedQuery = prefs.getString("last_query", "");
        searchInput.setText(savedQuery);

        LinearLayout.LayoutParams inputParams =
            new LinearLayout.LayoutParams(-1, dp(48));
        inputParams.topMargin = dp(10);
        root.addView(searchInput, inputParams);

        searchInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {}

            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                saveValue("last_query", s.toString());
            }

            public void afterTextChanged(Editable e) {}
        });

        Button searchButton = new Button(this);
        searchButton.setText("Cari / Buka");
        searchButton.setTextColor(WHITE);
        searchButton.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(BLUE)
        );

        LinearLayout.LayoutParams buttonParams =
            new LinearLayout.LayoutParams(-1, dp(46));
        buttonParams.topMargin = dp(8);
        root.addView(searchButton, buttonParams);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (url != null && url.startsWith("http")) {
                    saveValue("last_url", url);
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient());

        LinearLayout.LayoutParams webParams =
            new LinearLayout.LayoutParams(-1, 0, 1);
        webParams.topMargin = dp(12);
        root.addView(webView, webParams);

        updateEngineButtons(googleButton, duckButton);

        googleButton.setOnClickListener(v -> {
            useDuckDuckGo = false;
            prefs.edit().putBoolean("duckduckgo", false).apply();
            updateEngineButtons(googleButton, duckButton);
            webView.loadUrl("https://www.google.com");
        });

        duckButton.setOnClickListener(v -> {
            useDuckDuckGo = true;
            prefs.edit().putBoolean("duckduckgo", true).apply();
            updateEngineButtons(googleButton, duckButton);
            webView.loadUrl("https://duckduckgo.com");
        });

        searchButton.setOnClickListener(v -> performSearch());

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null &&
                 event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                performSearch();
                return true;
            }
            return false;
        });

        panelView = root;

        WindowManager.LayoutParams params =
            new WindowManager.LayoutParams(
                dp(340), dp(520),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            );

        params.gravity = Gravity.CENTER;

        try {
            wm.addView(panelView, params);
            panelVisible = true;

            String lastUrl = prefs.getString("last_url", "");

            if (!lastUrl.isEmpty()) {
                webView.loadUrl(lastUrl);
            } else if (useDuckDuckGo) {
                webView.loadUrl("https://duckduckgo.com");
            } else {
                webView.loadUrl("https://www.google.com");
            }

        } catch (Exception e) {
            panelView = null;
            Toast.makeText(this, "Panel gagal dibuka",
                Toast.LENGTH_SHORT).show();
        }
    }

    private void updateEngineButtons(Button google, Button duck) {
        google.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(
                useDuckDuckGo ? PANEL : BLUE
            )
        );

        duck.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(
                useDuckDuckGo ? BLUE : PANEL
            )
        );
    }

    private void performSearch() {
        if (searchInput == null || webView == null) return;

        String query = searchInput.getText().toString().trim();
        if (query.isEmpty()) return;

        String url;

        if (query.startsWith("https://") ||
            query.startsWith("http://")) {
            url = query;
        } else if (query.contains(".") && !query.contains(" ")) {
            url = "https://" + query;
        } else if (useDuckDuckGo) {
            url = "https://duckduckgo.com/?q=" + Uri.encode(query);
        } else {
            url = "https://www.google.com/search?q=" + Uri.encode(query);
        }

        saveValue("last_query", query);
        saveValue("last_url", url);

        webView.loadUrl(url);

        InputMethodManager imm =
            (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE
            );

        if (imm != null) {
            imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
        }
    }

    private void hidePanel() {
        if (panelView != null) {
            try {
                wm.removeView(panelView);
            } catch (Exception ignored) {}
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
        hidePanel();

        if (floatingButton != null && buttonAdded) {
            try {
                wm.removeView(floatingButton);
            } catch (Exception ignored) {}
        }

        buttonAdded = false;
        super.onDestroy();
    }
}
