package com.spade2021.floatapp;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.provider.Settings;
import android.net.Uri;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;

public class MainActivity extends Activity {

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
        finish();
    }

    private void showFloatingButton() {
        WindowManager wm =
            (WindowManager) getSystemService(WINDOW_SERVICE);

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
        params.x = 50;
        params.y = 200;

        wm.addView(floatButton, params);

        floatButton.setOnClickListener(v -> {
            showFloatingBrowser(wm);
        });
    }

    private void showFloatingBrowser(WindowManager wm) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundColor(0xFFFFFFFF);

        EditText address = new EditText(this);
        address.setSingleLine(true);
        address.setText("https://www.google.com");

        Button close = new Button(this);
        close.setText("TUTUP");

        WebView web = new WebView(this);
        web.setWebViewClient(new WebViewClient());
        web.getSettings().setJavaScriptEnabled(true);
        web.loadUrl("https://www.google.com");

        panel.addView(address);
        panel.addView(close);
        panel.addView(web, new LinearLayout.LayoutParams(
            600, 700
        ));

        WindowManager.LayoutParams params =
            new WindowManager.LayoutParams(
                650,
                850,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            );

        params.gravity = Gravity.CENTER;

        wm.addView(panel, params);

        address.setOnEditorActionListener((v, actionId, event) -> {
            String url = address.getText().toString().trim();

            if (!url.startsWith("http")) {
                url = "https://" + url;
            }

            web.loadUrl(url);
            return true;
        });

        close.setOnClickListener(v -> {
            web.destroy();
            wm.removeView(panel);
        });
    }
}
