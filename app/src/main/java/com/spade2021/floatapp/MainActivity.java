
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;

public class MainActivity extends Activity {

    private WindowManager wm;
    private LinearLayout panel;
    private WebView web;

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
        panel.setBackgroundColor(0xFFFFFFFF);

        EditText address = new EditText(this);
        address.setSingleLine(true);
        address.setText("https://www.google.com");
        address.setHint("Masukkan alamat website");
        address.setImeOptions(EditorInfo.IME_ACTION_GO);

        Button close = new Button(this);
        close.setText("TUTUP");

        web = new WebView(this);
        web.setWebViewClient(new WebViewClient());

        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        panel.addView(address);
        panel.addView(close);

        LinearLayout.LayoutParams webParams =
            new LinearLayout.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                0,
                1
            );

        panel.addView(web, webParams);

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

        close.setOnClickListener(v -> {
            web.destroy();
            wm.removeView(panel);
            panel = null;
            web = null;
        });
    }
}
