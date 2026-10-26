package henry.android;

import android.content.Context;
import android.content.SharedPreferences;
import henry.Storage;

/** Sauvegarde de la progression sur le téléphone. */
final class AndroidStorage implements Storage {

    private final SharedPreferences prefs;

    AndroidStorage(Context context) {
        prefs = context.getSharedPreferences("henry-le-herisson", Context.MODE_PRIVATE);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        return prefs.getInt(key, defaultValue);
    }

    @Override
    public void putInt(String key, int value) {
        prefs.edit().putInt(key, value).apply();
    }
}
