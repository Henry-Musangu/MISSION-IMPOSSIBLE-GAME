package henry.quincaillerie;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * L'écran unique de l'application : toute l'interface est la page {@code assets/index.html},
 * affichée ici. Cette classe garde les données dans un fichier privé du téléphone et
 * s'occupe de la sauvegarde, de la restauration et du partage du rapport.
 */
public class MainActivity extends Activity {

    private static final int REQ_EXPORT = 1;
    private static final int REQ_IMPORT = 2;
    private static final String DATA = "donnees.json";
    private static final int MAX_IMPORT = 32 * 1024 * 1024;

    private WebView web;
    private String pendingExport;
    private final Object lock = new Object();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setSupportZoom(false);
        s.setTextZoom(100);

        web.setWebViewClient(new WebViewClient() {
            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // L'application ne quitte jamais sa propre page.
                return url == null || !url.startsWith("file:///android_asset/");
            }
        });
        web.addJavascriptInterface(new Bridge(), "AndroidBridge");
        web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        web.evaluateJavascript("window.onAndroidBack ? window.onAndroidBack() : false",
                new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String handled) {
                        if (!"true".equals(handled)) {
                            finish();
                        }
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri uri = data == null ? null : data.getData();
        if (requestCode == REQ_EXPORT) {
            String json = pendingExport;
            pendingExport = null;
            if (resultCode != RESULT_OK || uri == null || json == null) {
                return;
            }
            boolean ok = false;
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out != null) {
                    out.write(json.getBytes(StandardCharsets.UTF_8));
                    ok = true;
                }
            } catch (Exception e) {
                ok = false;
            }
            js("window.onBackupSaved(" + ok + ")");
        } else if (requestCode == REQ_IMPORT) {
            if (resultCode != RESULT_OK || uri == null) {
                return;
            }
            String text = "";
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in != null) {
                    text = readAll(in, MAX_IMPORT);
                }
            } catch (Exception e) {
                text = "";
            }
            js("window.onBackupImported(" + quote(text) + ")");
        }
    }

    private void js(String code) {
        web.evaluateJavascript(code, null);
    }

    /** Transforme un texte en chaîne JavaScript sûre. */
    private static String quote(String text) {
        return JSONObject.quote(text).replace("\u2028", "\\u2028").replace("\u2029", "\\u2029");
    }

    private static String readAll(InputStream in, int max) throws Exception {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = in.read(chunk)) > 0) {
            buf.write(chunk, 0, n);
            if (buf.size() > max) {
                throw new Exception("Fichier trop gros");
            }
        }
        return new String(buf.toByteArray(), StandardCharsets.UTF_8);
    }

    private String readFile(File f) {
        if (!f.exists()) {
            return "";
        }
        try (InputStream in = new FileInputStream(f)) {
            return readAll(in, Integer.MAX_VALUE - 16);
        } catch (Exception e) {
            return "";
        }
    }

    /** Les fonctions que la page peut appeler (window.AndroidBridge). */
    private final class Bridge {

        @JavascriptInterface
        public String load() {
            synchronized (lock) {
                String text = readFile(new File(getFilesDir(), DATA));
                if (text.isEmpty()) {
                    text = readFile(new File(getFilesDir(), DATA + ".bak"));
                }
                return text;
            }
        }

        /** Écrit d'abord un fichier temporaire, pour ne jamais abîmer les données en cas de coupure. */
        @JavascriptInterface
        public void save(String json) {
            synchronized (lock) {
                File dir = getFilesDir();
                File cur = new File(dir, DATA);
                File tmp = new File(dir, DATA + ".tmp");
                File bak = new File(dir, DATA + ".bak");
                try {
                    try (FileOutputStream out = new FileOutputStream(tmp)) {
                        out.write(json.getBytes(StandardCharsets.UTF_8));
                        out.getFD().sync();
                    }
                    if (cur.exists()) {
                        bak.delete();
                        cur.renameTo(bak);
                    }
                    tmp.renameTo(cur);
                } catch (Exception e) {
                    tmp.delete();
                }
            }
        }

        @JavascriptInterface
        public void exportBackup(final String json, final String name) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pendingExport = json;
                    Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("application/json");
                    i.putExtra(Intent.EXTRA_TITLE, name);
                    try {
                        startActivityForResult(i, REQ_EXPORT);
                    } catch (Exception e) {
                        pendingExport = null;
                        send(json);
                    }
                }
            });
        }

        @JavascriptInterface
        public void importBackup() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    try {
                        startActivityForResult(i, REQ_IMPORT);
                    } catch (Exception e) {
                        js("window.onBackupImported('')");
                    }
                }
            });
        }

        @JavascriptInterface
        public void shareText(final String text) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    send(text);
                }
            });
        }

        @JavascriptInterface
        public void setStatusBar(final String color, final boolean darkIcons) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        getWindow().setStatusBarColor(Color.parseColor(color));
                        if (Build.VERSION.SDK_INT >= 23) {
                            View decor = getWindow().getDecorView();
                            int flags = decor.getSystemUiVisibility();
                            if (darkIcons) {
                                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                            } else {
                                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                            }
                            decor.setSystemUiVisibility(flags);
                        }
                    } catch (Exception e) {
                        // Couleur illisible : on garde la barre telle quelle.
                    }
                }
            });
        }
    }

    private void send(String text) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, text);
        try {
            startActivity(Intent.createChooser(i, "Envoyer"));
        } catch (Exception e) {
            // Aucune application pour envoyer du texte.
        }
    }
}
