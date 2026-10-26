package henry.desktop;

import henry.Storage;
import java.util.prefs.Preferences;

/** Sauvegarde de la progression sur PC (préférences Java de l'utilisateur). */
final class PrefsStorage implements Storage {

    private final Preferences prefs = Preferences.userRoot().node("henry-le-herisson");

    @Override
    public int getInt(String key, int defaultValue) {
        try {
            return prefs.getInt(key, defaultValue);
        } catch (RuntimeException e) {
            return defaultValue;
        }
    }

    @Override
    public void putInt(String key, int value) {
        try {
            prefs.putInt(key, value);
            prefs.flush();
        } catch (Exception e) {
            // sauvegarde impossible : tant pis, le jeu continue
        }
    }
}
