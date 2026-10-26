package henry;

import java.util.HashMap;
import java.util.Map;

/** Sauvegarde de la progression (meilleur score, zones débloquées, réglages). */
public interface Storage {

    int getInt(String key, int defaultValue);

    void putInt(String key, int value);

    /** Sauvegarde en mémoire seulement (perdue à la fermeture), pour les tests. */
    final class Memory implements Storage {
        private final Map<String, Integer> values = new HashMap<>();

        @Override
        public int getInt(String key, int defaultValue) {
            Integer v = values.get(key);
            return v == null ? defaultValue : v;
        }

        @Override
        public void putInt(String key, int value) {
            values.put(key, value);
        }
    }
}
