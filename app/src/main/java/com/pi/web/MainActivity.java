package com.pi.web;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {
    private static final String TARGET_URL = "http://127.0.0.1:30141";
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 30141;

    private WebView webView;
    private FrameLayout rootLayout;
    private LinearLayout loadingLayout;
    private TextView statusText;
    private ProgressBar progressBar;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isLoaded = false;
    private boolean isChecking = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(Color.parseColor("#1e1e1e"));

        // WebView 配置
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    isLoaded = false;
                    showLoading("连接断开，正在尝试重连...");
                    startProbeLoop();
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (!url.equals("about:blank")) {
                    isLoaded = true;
                    hideLoading();
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient());
        rootLayout.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // Loading & 错误重试布局
        loadingLayout = new LinearLayout(this);
        loadingLayout.setOrientation(LinearLayout.VERTICAL);
        loadingLayout.setGravity(Gravity.CENTER);
        loadingLayout.setBackgroundColor(Color.parseColor("#121212"));

        progressBar = new ProgressBar(this);
        loadingLayout.addView(progressBar);

        statusText = new TextView(this);
        statusText.setText("正在连接 Pi Web 服务 (127.0.0.1:30141)...");
        statusText.setTextColor(Color.WHITE);
        statusText.setTextSize(16);
        statusText.setPadding(0, 32, 0, 0);
        loadingLayout.addView(statusText);

        rootLayout.addView(loadingLayout, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        setContentView(rootLayout);

        startProbeLoop();
    }

    private void showLoading(String msg) {
        runOnUiThread(() -> {
            statusText.setText(msg);
            loadingLayout.setVisibility(View.VISIBLE);
        });
    }

    private void hideLoading() {
        runOnUiThread(() -> loadingLayout.setVisibility(View.GONE));
    }

    private void startProbeLoop() {
        if (isChecking) return;
        isChecking = true;

        new Thread(() -> {
            while (!isLoaded) {
                boolean reachable = checkPort();
                if (reachable) {
                    runOnUiThread(() -> {
                        if (!isLoaded) {
                            statusText.setText("服务已就绪，正在载入页面...");
                            webView.loadUrl(TARGET_URL);
                        }
                    });
                    break;
                } else {
                    runOnUiThread(() -> statusText.setText("Pi Web 尚未启动或正在就绪\n正在每秒自动探测重试..."));
                }
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException ignored) {}
            }
            isChecking = false;
        }).start();
    }

    private boolean checkPort() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(HOST, PORT), 1000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
