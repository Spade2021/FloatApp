
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
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.EditorInfo;
import android.view.KeyEvent;
import android.widget.*;
import android.webkit.*;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {

    private WindowManager wm;
    private android.view.View floatingButton, panelView;
    private EditText searchInput;
    private WebView webView;

    private boolean buttonAdded = false;
    private boolean panelVisible = false;
    private boolean useDuckDuckGo = false;
    private boolean darkMode = true;

    private SharedPreferences prefs;

    private int BG, PANEL, BLUE, WHITE, MUTED, BUTTON_TEXT;

    private final Set<String> blockedHosts = new HashSet<>(
        Arrays.asList(
            "doubleclick.net",
            "googlesyndication.com",
            "googleadservices.com",
            "adservice.google.com",
            "adnxs.com",
            "taboola.com",
            "outbrain.com",
            "scorecardresearch.com",
            "adcolony.com",
            "popads.net",
            "popcash.net"
        )
    );

    private int dp(float value) {
        return (int) (value * getResources()
            .getDisplayMetrics().density);
    }

    private void applyTheme() {
        if (darkMode) {
            BG = Color.rgb(20, 22, 29);
            PANEL = Color.rgb(40, 43, 52);
            BLUE = Color.rgb(65, 125, 255);
            WHITE = Color.WHITE;
            MUTED = Color.rgb(165, 170, 185);
            BUTTON_TEXT = Color.WHITE;
        } else {
            BG = Color.rgb(245, 245, 245);
            PANEL = Color.rgb(225, 225, 225);
            BLUE = Color.rgb(65, 125, 255);
            WHITE = Color.rgb(25, 25, 25);
            MUTED = Color.rgb(100, 100, 100);
            BUTTON_TEXT = Color.WHITE;
        }
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

    private boolean isAdHost(String host) {
        if (host == null) return false;

        String domain = host.toLowerCase();

        for (String blocked : blockedHosts) {
            if (domain.equals(blocked) ||
                domain.endsWith("." + blocked)) {
                return true;
            }
        }
        return false;
    }

    private WebResourceResponse emptyResponse() {
        return new WebResourceResponse(
            "text/plain",
            "UTF-8",
            200,
            "OK",
            null,
            new ByteArrayInputStream(new byte[0])
        );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("FloatAppData", MODE_PRIVATE);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        darkMode = prefs.getBoolean("dark_mode", true);
        applyTheme();

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
        button.setBackground(rounded(
            darkMode ? Color.WHITE : Color.BLACK, 50
        ));
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
        params.y = dp(65);

        button.setOnClickListener(v -> togglePanel());

        try {
            wm.addView(floatingButton, params);
            buttonAdded = true;
        } catch (Exception e) {
            Toast.makeText(this, "Tombol gagal ditampilkan",
                Toast.LENGTH_SHORT).show();
        }
    }

    private void updateFloatingButton() {
        if (floatingButton != null) {
            floatingButton.setBackground(rounded(
                darkMode ? Color.WHITE : Color.BLACK, 50
            ));
        }
    }

    private void togglePanel() {
        if (panelVisible) hidePanel();
        else showPanel();
    }

    private void changeTheme() {
        darkMode = !darkMode;
        prefs.edit().putBoolean("dark_mode", darkMode).apply();
        applyTheme();
        hidePanel();
        updateFloatingButton();
        showPanel();
    }

    private void showPanel() {
        if (panelVisible) return;

        useDuckDuckGo = prefs.getBoolean("duckduckgo", false);
        applyTheme();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(8), dp(12), dp(8));
        root.setBackground(rounded(BG, 16));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("FloatApp");
        title.setTextSize(18);
        title.setTextColor(WHITE);
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        Button themeButton = new Button(this);
        themeButton.setText(darkMode ? "Putih" : "Hitam");
        themeButton.setTextSize(11);
        themeButton.setTextColor(WHITE);
        themeButton.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(BLUE)
        );

        TextView close = new TextView(this);
        close.setText("✕");
        close.setTextSize(20);
        close.setTextColor(WHITE);
        close.setGravity(Gravity.CENTER);

        header.addView(title, new LinearLayout.LayoutParams(0, dp(38), 1));
        header.addView(themeButton, new LinearLayout.LayoutParams(dp(85), dp(38)));
        header.addView(close, new LinearLayout.LayoutParams(dp(38), dp(38)));

        root.addView(header);

        themeButton.setOnClickListener(v -> changeTheme());
        close.setOnClickListener(v -> hidePanel());

        LinearLayout engines = new LinearLayout(this);
        engines.setOrientation(LinearLayout.HORIZONTAL);

        Button google = new Button(this);
        google.setText("Google");
        google.setTextColor(WHITE);

        Button duck = new Button(this);
        duck.setText("DuckDuckGo");
        duck.setTextColor(WHITE);

        LinearLayout.LayoutParams p1 =
            new LinearLayout.LayoutParams(0, dp(42), 1);
        p1.rightMargin = dp(4);
        engines.addView(google, p1);

        LinearLayout.LayoutParams p2 =
            new LinearLayout.LayoutParams(0, dp(42), 1);
        p2.leftMargin = dp(4);
        engines.addView(duck, p2);

        root.addView(engines);

        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setTextColor(WHITE);
        searchInput.setHintTextColor(MUTED);
        searchInput.setHint("Cari atau masukkan alamat website");
        searchInput.setTextSize(16);
        searchInput.setPadding(dp(12), 0, dp(12), 0);
        searchInput.setBackground(rounded(PANEL, 10));
        searchInput.setImeOptions(EditorInfo.IME_ACTION_GO);
        searchInput.setText(prefs.getString("last_query", ""));

        LinearLayout.LayoutParams inputParams =
            new LinearLayout.LayoutParams(-1, dp(48));
        inputParams.topMargin = dp(5);
        root.addView(searchInput, inputParams);

        Button search = new Button(this);
        search.setText("Cari / Buka");
        search.setTextColor(WHITE);
        search.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(BLUE)
        );

        LinearLayout.LayoutParams searchParams =
            new LinearLayout.LayoutParams(-1, dp(42));
        searchParams.topMargin = dp(4);
        root.addView(search, searchParams);

        TextView info = new TextView(this);
        info.setText("Filter iklan aktif");
        info.setTextColor(MUTED);
        info.setTextSize(10);
        info.setGravity(Gravity.CENTER);
        root.addView(info, new LinearLayout.LayoutParams(-1, dp(18)));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(
                WebView view, WebResourceRequest request) {

                if (isAdHost(request.getUrl().getHost())) {
                    return emptyResponse();
                }

                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onReceivedError(
                WebView view,
                WebResourceRequest request,
                WebResourceError error) {

                super.onReceivedError(view, request, error);

                if (request.isForMainFrame()) {
                    runOnUiThread(() ->
                        Toast.makeText(
                            MainActivity.this,
                            "Halaman gagal dimuat: " +
                                error.getDescription(),
                            Toast.LENGTH_LONG
                        ).show()
                    );
                }
            }

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

        webParams.topMargin = dp(2);
        root.addView(webView, webParams);

        updateEngineButtons(google, duck);

        google.setOnClickListener(v -> {
            useDuckDuckGo = false;
            prefs.edit().putBoolean("duckduckgo", false).apply();
            updateEngineButtons(google, duck);
            webView.loadUrl("https://www.google.com");
        });

        duck.setOnClickListener(v -> {
            useDuckDuckGo = true;
            prefs.edit().putBoolean("duckduckgo", true).apply();
            updateEngineButtons(google, duck);
            webView.loadUrl("https://duckduckgo.com");
        });

        search.setOnClickListener(v -> performSearch());

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
                dp(370), dp(650),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            );

        params.gravity = Gravity.CENTER;
        params.y = -dp(35);

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
            (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

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
            webView.stopLoading();
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
            
