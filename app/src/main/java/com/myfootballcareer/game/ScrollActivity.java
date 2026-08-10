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

/**
 * Android 1.0.2 wrapper.
 *
 * Adds a WebView-specific scroll compatibility layer for MFC's fixed fullscreen
 * menu/sheet/modal containers while deliberately leaving the live match controls
 * untouched.
 */
public class ScrollActivity extends Activity {
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
            webView.setVerticalScrollBarEnabled(true);
            webView.setHorizontalScrollBarEnabled(false);
            webView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                webView.setNestedScrollingEnabled(true);
            }
            root.addView(webView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
        } catch (Throwable t) {
            splash.setText("MY FOOTBALL CAREER\n\nNie udało się uruchomić Android System WebView.\nZaktualizuj Chrome / Android System WebView i uruchom aplikację ponownie.\n\n" + t.getClass().getSimpleName());
            setContentView(root);
            root.post(this::enterImmersiveMode);
            return;
        }

        setContentView(root);
        root.post(this::enterImmersiveMode);
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
                // Reinjection is idempotent and also protects against a future internal page refresh.
                view.evaluateJavascript(androidScrollCompatJavascript(), null);
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
            html = injectAndroidScrollCompat(html);
            webView.loadDataWithBaseURL(APP_ORIGIN, html, "text/html", "UTF-8", APP_ORIGIN);
        } catch (Throwable t) {
            showStartupError("Nie udało się wczytać pliku gry: " + t.getClass().getSimpleName() + " — " + t.getMessage());
        }
    }

    private String injectAndroidScrollCompat(String html) {
        String style = "<style id=\"mfcAndroidScrollStyle\">" +
                "html,body,#app{touch-action:pan-y pinch-zoom;}" +
                ".v970-menu,.v970-sheet-card,.v900-more-panel,.modal-card," +
                "[class*=\"dialog\"],[class*=\"sheet-card\"],[class*=\"panel\"]{" +
                "-webkit-overflow-scrolling:touch;overscroll-behavior-y:contain;}" +
                ".live-match-screen,#liveMatchScreen,#virtualStick,.virtual-stick," +
                ".live-actions,.live-action,#liveActions,#matchCanvas{" +
                "touch-action:none!important;overscroll-behavior:none!important;}" +
                "</style>";
        String script = "<script>" + androidScrollCompatJavascript() + "</script>";

        if (!html.contains("mfcAndroidScrollStyle")) {
            int head = html.toLowerCase().lastIndexOf("</head>");
            if (head >= 0) html = html.substring(0, head) + style + html.substring(head);
            else html = style + html;
        }
        if (!html.contains("__mfcAndroidScrollCompatInstalled")) {
            int body = html.toLowerCase().lastIndexOf("</body>");
            if (body >= 0) html = html.substring(0, body) + script + html.substring(body);
            else html = html + script;
        }
        return html;
    }

    /**
     * Native Android WebView compatibility fallback.
     * It manually scrolls the nearest actual scroll container only when the gesture
     * is clearly vertical. Tap/click behaviour remains native. The live match is
     * explicitly excluded so the virtual stick, B/A/C buttons and canvas keep their
     * own pointer logic.
     */
    private String androidScrollCompatJavascript() {
        return "(function(){" +
                "if(window.__mfcAndroidScrollCompatInstalled)return;" +
                "window.__mfcAndroidScrollCompatInstalled=true;" +
                "var g=null;" +
                "var BLOCK='#liveMatchScreen,.live-match-screen,#virtualStick,.virtual-stick,#liveActions,.live-actions,.live-action,#matchCanvas,.rotate-hint';" +
                "function blocked(el){try{return !!(el&&el.closest&&el.closest(BLOCK));}catch(e){return false;}}" +
                "function visible(el){if(!el)return false;var r=el.getBoundingClientRect();var s=getComputedStyle(el);return r.width>0&&r.height>0&&s.display!=='none'&&s.visibility!=='hidden';}" +
                "function scrollable(el){if(!el||el===document.body||el===document.documentElement)return false;" +
                "var s=getComputedStyle(el);var oy=s.overflowY;return visible(el)&&(oy==='auto'||oy==='scroll'||oy==='overlay')&&el.scrollHeight>el.clientHeight+3;}" +
                "function targetFrom(el){var n=el;while(n&&n!==document.body&&n!==document.documentElement){if(scrollable(n))return n;n=n.parentElement;}" +
                "var fixed=[document.querySelector('.v970-menu:not(.hidden)'),document.querySelector('.v970-sheet-card'),document.querySelector('.v900-more-panel'),document.querySelector('.modal:not(.hidden) .modal-card')];" +
                "for(var i=0;i<fixed.length;i++)if(scrollable(fixed[i]))return fixed[i];" +
                "var d=document.scrollingElement||document.documentElement;return d&&d.scrollHeight>d.clientHeight+3?d:null;}" +
                "function start(e){if(!e.touches||e.touches.length!==1||blocked(e.target)){g=null;return;}var t=e.touches[0],sc=targetFrom(e.target);if(!sc){g=null;return;}" +
                "g={id:t.identifier,x:t.clientX,y:t.clientY,top:sc.scrollTop||0,sc:sc,drag:false};}" +
                "function move(e){if(!g||!e.touches||e.touches.length!==1)return;var t=e.touches[0];if(t.identifier!==g.id)return;" +
                "var dx=t.clientX-g.x,dy=t.clientY-g.y;if(!g.drag){if(Math.abs(dy)<8)return;if(Math.abs(dy)<=Math.abs(dx)*1.05){g=null;return;}g.drag=true;}" +
                "var max=Math.max(0,g.sc.scrollHeight-g.sc.clientHeight);var next=Math.max(0,Math.min(max,g.top-dy));" +
                "if(Math.abs((g.sc.scrollTop||0)-next)>.5)g.sc.scrollTop=next;" +
                "if(e.cancelable)e.preventDefault();}" +
                "function end(){g=null;}" +
                "document.addEventListener('touchstart',start,{capture:true,passive:true});" +
                "document.addEventListener('touchmove',move,{capture:true,passive:false});" +
                "document.addEventListener('touchend',end,{capture:true,passive:true});" +
                "document.addEventListener('touchcancel',end,{capture:true,passive:true});" +
                "document.documentElement.classList.add('mfc-android-scroll');" +
                "})();";
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
        if (getWindow().peekDecorView() != null) enterImmersiveMode();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }

    private void enterImmersiveMode() {
        View decor = getWindow().peekDecorView();
        if (decor == null) return;

        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = decor.getWindowInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            decor.setSystemUiVisibility(
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
                        Toast.makeText(ScrollActivity.this,
                                "Zapisano w Pobrane/My Football Career: " + safeName,
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(ScrollActivity.this,
                                "Eksport plików wymaga Androida 10+ w tej wersji aplikacji.",
                                Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(ScrollActivity.this,
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
