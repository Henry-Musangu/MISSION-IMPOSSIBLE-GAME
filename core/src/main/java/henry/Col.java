package henry;

/** Petites fonctions sur les couleurs ARGB stockées dans un {@code int}. */
final class Col {

    static final int BLACK = 0xff000000;
    static final int WHITE = 0xffffffff;
    static final int RED = 0xffff0000;
    static final int YELLOW = 0xffffff00;
    static final int GRAY = 0xff808080;
    static final int LIGHT_GRAY = 0xffc0c0c0;
    static final int ORANGE = 0xffffc800;

    private Col() {
    }

    static int rgb(int rgb) {
        return 0xff000000 | rgb;
    }

    static int argb(int alpha, int rgb) {
        return (clamp(alpha) << 24) | (rgb & 0xffffff);
    }

    static int rgba(int r, int g, int b, int a) {
        return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    static int withAlpha(int color, int alpha) {
        return argb(alpha, color);
    }

    /** Même formule que {@code java.awt.Color.brighter()}. */
    static int brighter(int c) {
        int r = (c >> 16) & 0xff;
        int g = (c >> 8) & 0xff;
        int b = c & 0xff;
        int i = (int) (1.0 / 0.3);
        if (r == 0 && g == 0 && b == 0) {
            return rgba(i, i, i, c >>> 24);
        }
        if (r > 0 && r < i) {
            r = i;
        }
        if (g > 0 && g < i) {
            g = i;
        }
        if (b > 0 && b < i) {
            b = i;
        }
        return rgba(Math.min((int) (r / 0.7), 255), Math.min((int) (g / 0.7), 255),
                Math.min((int) (b / 0.7), 255), c >>> 24);
    }

    /** Même formule que {@code java.awt.Color.darker()}. */
    static int darker(int c) {
        return rgba((int) (((c >> 16) & 0xff) * 0.7), (int) (((c >> 8) & 0xff) * 0.7),
                (int) ((c & 0xff) * 0.7), c >>> 24);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
