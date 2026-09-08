package com.monkeytech.hiswrapper;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;

public class MainActivity extends Activity {
    private static final String HIS_LOGIN = "https://his-uat.easehospital.com/web/login";
    private static final String FLOATING_URL = "https://santib521.github.io/idcard/index.html";
    private static final int CAMERA_REQUEST = 10;
    private static final int FILE_REQUEST = 11;

    private WebView hisWebView;
    private WebView cardWebView;
    private Dialog cardDialog;
    private PermissionRequest pendingCameraRequest;
    private ValueCallback<Uri[]> pendingFileCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        hisWebView = new WebView(this);
        root.addView(hisWebView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        Button floatingButton = new Button(this);
        floatingButton.setText("อ่านบัตร");
        floatingButton.setTextColor(Color.WHITE);
        floatingButton.setTextSize(16);
        floatingButton.setAllCaps(false);
        floatingButton.setBackgroundColor(Color.rgb(25, 118, 111));
        floatingButton.setElevation(dp(10));
        floatingButton.setOnClickListener(v -> openCardReader());

        FrameLayout.LayoutParams buttonParams = new FrameLayout.LayoutParams(dp(118), dp(56));
        buttonParams.gravity = Gravity.END | Gravity.BOTTOM;
        buttonParams.setMargins(dp(12), dp(12), dp(16), dp(20));
        root.addView(floatingButton, buttonParams);
        setContentView(root);

        configureHisWebView();
        if (savedInstanceState == null) hisWebView.loadUrl(HIS_LOGIN);
        else hisWebView.restoreState(savedInstanceState);
    }

    private void configureHisWebView() {
        configureCommonSettings(hisWebView, false);
        hisWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String host = uri.getHost();
                if (host != null && (host.equals("easehospital.com") || host.endsWith(".easehospital.com"))) {
                    return false;
                }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception ignored) { }
                return true;
            }
        });
        hisWebView.setWebChromeClient(createChromeClient(false));
    }

    private void openCardReader() {
        if (cardDialog != null && cardDialog.isShowing()) return;

        cardDialog = new Dialog(this);
        cardDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout panel = new FrameLayout(this);
        panel.setBackgroundColor(Color.WHITE);

        cardWebView = new WebView(this);
        configureCommonSettings(cardWebView, true);
        cardWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // หน้าเดิมเริ่มแบบ Mini; ในหน้าต่าง Native ให้เปิด Full โดยไม่แก้ OCR/Card Reader
                view.evaluateJavascript("if(window.expandWidget){expandWidget();}", null);
            }
        });
        cardWebView.setWebChromeClient(createChromeClient(true));
        panel.addView(cardWebView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        Button close = new Button(this);
        close.setText("×");
        close.setTextSize(24);
        close.setTextColor(Color.WHITE);
        close.setBackgroundColor(Color.rgb(25, 92, 87));
        close.setOnClickListener(v -> cardDialog.dismiss());
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(dp(48), dp(48));
        closeParams.gravity = Gravity.TOP | Gravity.END;
        closeParams.setMargins(0, dp(8), dp(8), 0);
        panel.addView(close, closeParams);

        cardDialog.setContentView(panel);
        cardDialog.setOnDismissListener(d -> destroyCardWebView());
        Window window = cardDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        cardDialog.show();
        window = cardDialog.getWindow();
        if (window != null) window.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        cardWebView.loadUrl(FLOATING_URL);
    }

    private void configureCommonSettings(WebView webView, boolean localAgentPage) {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setSupportZoom(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        if (localAgentPage) s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        webView.setBackgroundColor(Color.WHITE);
    }

    private WebChromeClient createChromeClient(boolean permitCamera) {
        return new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                if (!permitCamera) {
                    request.deny();
                    return;
                }
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                } else {
                    pendingCameraRequest = request;
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);
                }
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (pendingFileCallback != null) pendingFileCallback.onReceiveValue(null);
                pendingFileCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), FILE_REQUEST);
                    return true;
                } catch (Exception e) {
                    pendingFileCallback = null;
                    return false;
                }
            }
        };
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == CAMERA_REQUEST && pendingCameraRequest != null) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                pendingCameraRequest.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
            } else pendingCameraRequest.deny();
            pendingCameraRequest = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_REQUEST && pendingFileCallback != null) {
            Uri[] result = resultCode == RESULT_OK
                    ? WebChromeClient.FileChooserParams.parseResult(resultCode, data) : null;
            pendingFileCallback.onReceiveValue(result);
            pendingFileCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (cardDialog != null && cardDialog.isShowing()) {
            cardDialog.dismiss();
        } else if (hisWebView.canGoBack()) {
            hisWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        hisWebView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        destroyCardWebView();
        if (hisWebView != null) {
            hisWebView.stopLoading();
            hisWebView.destroy();
        }
        super.onDestroy();
    }

    private void destroyCardWebView() {
        if (cardWebView != null) {
            cardWebView.stopLoading();
            cardWebView.loadUrl("about:blank");
            cardWebView.destroy();
            cardWebView = null;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
