package com.myfootballcareer.game;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST = 7001;
    private static final String APP_ORIGIN = "https://mfc.local/";

    private WebView webView;
    private TextView splash;
    private ValueCallback<Uri[]> filePathCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setStatusBarColor(Color.rgb(7, 20, 28));
        getWindow().setNavigationBarColor(Color.rgb(7, 20, 28));
        enterImmersiveMode();

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(7, 20, 28));

        splash = new TextView(this);
        splash.setText("MY FOOTBALL CAREER\n\nUruchamianie gry…");
        splash.setTextColor(Color.rgb(224, 177, 63));
        splash.setTextSize(20f);
        splash.setGravity(Gravity.CENTER);
        splash.setBackgroundColor(Color.rgb(7, 20, 28));
        root.addView(splash, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        try {
            webView = new WebView(this);
            webView.setBackgroundColor(Color.rgb(7, 20, 28));
            webView.setVisibility(View.INVISIBLE);
            root.addView(webView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
        } catch (Throwable t) {
            splash.setText("MY FOOTBALL CAREER\n\nNie udało się uruchomić Android System WebView.\nZaktualizuj Chrome / Android System WebView i uruchom aplikację ponownie.\n\n" + t.getClass().getSimpleName());
            setContentView(root);
            return;
        }

        setContentView(root);
        configureWebView();
        loadBundledGame();
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);

        webView.addJavascriptInterface(new AndroidBridge(), "AndroidMFC");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                String host = uri.getHost();
                if (("https".equals(scheme) && "mfc.local".equals(host)) ||
                        "file".equals(scheme) || "about".equals(scheme) ||
                        "data".equals(scheme) || "blob".equals(scheme)) {
                    return false;
                }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception ignored) {
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                webView.setVisibility(View.VISIBLE);
                splash.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request != null && request.isForMainFrame()) {
                    String desc = error == null ? "nieznany błąd" : String.valueOf(error.getDescription());
                    showStartupError("Błąd WebView: " + desc);
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = callback;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                        "application/json",
                        "application/octet-stream",
                        "application/x-ool-save",
                        "application/x-ool-bundle",
                        "text/plain"
                });
                startActivityForResult(Intent.createChooser(intent, "Importuj save My Football Career"), FILE_CHOOSER_REQUEST);
                return true;
            }
        });
    }

    private void loadBundledGame() {
        try {
            String html;
            try {
                html = readAsset("index.html.html");
            } catch (Exception first) {
                html = readAsset("index.html");
            }
            if (html == null || html.length() < 1000) {
                throw new IllegalStateException("Plik gry jest pusty lub niekompletny");
            }
            // HTTPS base origin daje WebView stabilny origin dla localStorage zamiast file://.
            webView.loadDataWithBaseURL(APP_ORIGIN, html, "text/html", "UTF-8", APP_ORIGIN);
        } catch (Throwable t) {
            showStartupError("Nie udało się wczytać pliku gry: " + t.getClass().getSimpleName() + " — " + t.getMessage());
        }
    }

    private String readAsset(String name) throws Exception {
        try (InputStream in = getAssets().open(name);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[32 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            return out.toString("UTF-8");
        }
    }

    private void showStartupError(String message) {
        if (webView != null) webView.setVisibility(View.GONE);
        splash.setVisibility(View.VISIBLE);
        splash.setText("MY FOOTBALL CAREER\n\nNie udało się uruchomić gry.\n\n" + message + "\n\nSpróbuj zaktualizować Chrome / Android System WebView.");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && filePathCallback != null) {
            Uri[] result = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                result = new Uri[]{data.getData()};
            }
            filePathCallback.onReceiveValue(result);
            filePathCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.getVisibility() == View.VISIBLE && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        enterImmersiveMode();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }

    private void enterImmersiveMode() {
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    private class AndroidBridge {
        @JavascriptInterface
        public void saveText(String text, String filename, String mimeType) {
            runOnUiThread(() -> {
                try {
                    String safeName = filename == null || filename.trim().isEmpty()
                            ? "MFC_save.oolsave"
                            : filename.replaceAll("[\\\\/:*?\"<>|]", "_");
                    String mime = mimeType == null || mimeType.isEmpty()
                            ? "application/octet-stream"
                            : mimeType;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ContentValues values = new ContentValues();
                        values.put(MediaStore.Downloads.DISPLAY_NAME, safeName);
                        values.put(MediaStore.Downloads.MIME_TYPE, mime);
                        values.put(MediaStore.Downloads.RELATIVE_PATH,
                                Environment.DIRECTORY_DOWNLOADS + "/My Football Career");
                        Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                        if (uri == null) throw new IllegalStateException("Nie można utworzyć pliku");
                        try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                            if (os == null) throw new IllegalStateException("Brak strumienia zapisu");
                            os.write(text.getBytes(StandardCharsets.UTF_8));
                        }
                        Toast.makeText(MainActivity.this,
                                "Zapisano w Pobrane/My Football Career: " + safeName,
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.this,
                                "Eksport plików wymaga Androida 10+ w tej wersji aplikacji.",
                                Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this,
                            "Eksport nie powiódł się: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            });
        }

        @JavascriptInterface
        public void setOrientation(String mode) {
            runOnUiThread(() -> {
                if (mode == null) return;
                if (mode.startsWith("landscape"))
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                else if (mode.startsWith("portrait"))
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                else
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            });
        }
    }
}
