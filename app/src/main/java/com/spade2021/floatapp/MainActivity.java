
package com.spade2021.floatapp;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.widget.EditText;
import android.widget.Button;
import android.widget.LinearLayout;
import android.view.View;
import android.view.inputmethod.EditorInfo;

public class MainActivity extends Activity {

    private WebView webView;
    private EditText addressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        addressBar = new EditText(this);
        addressBar.setSingleLine(true);
        addressBar.setHint("Masukkan alamat website");
        addressBar.setText("https://www.google.com");
        addressBar.setInputType(17);
        addressBar.setImeOptions(EditorInfo.IME_ACTION_GO);

        Button openButton = new Button(this);
        openButton.setText("BUKA WEBSITE");

        webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient());

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);

        layout.addView(addressBar);
        layout.addView(openButton);
        layout.addView(webView,
            new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0, 1
            )
        );

        setContentView(layout);

        openButton.setOnClickListener(v -> openWebsite());

        addressBar.setOnEditorActionListener((v, actionId, event) -> {
            openWebsite();
            return true;
        });

        webView.loadUrl("https://www.google.com");
    }

    private void openWebsite() {
        String address = addressBar.getText().toString().trim();

        if (!address.startsWith("http://")
                && !address.startsWith("https://")) {
            address = "https://" + address;
        }

        webView.loadUrl(address);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
