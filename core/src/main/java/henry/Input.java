package henry;

/**
 * Boutons logiques du jeu. Chaque plateforme les actionne à sa façon :
 * le clavier sur PC, l'écran tactile sur Android.
 */
public final class Input {

    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final int UP = 2;
    public static final int DOWN = 3;
    public static final int JUMP = 4;
    public static final int START = 5;
    public static final int PAUSE = 6;
    public static final int MUTE = 7;
    public static final int COUNT = 8;

    private final boolean[] down = new boolean[COUNT];
    private final boolean[] prev = new boolean[COUNT];
    private final boolean[] tapped = new boolean[COUNT];
    /** Touchers sur l'écran pendant cette image, en coordonnées du jeu (x, y, x, y…). */
    private final double[] taps = new double[16];
    private int tapCount;

    /** Bouton maintenu ou relâché. */
    public synchronized void set(int button, boolean value) {
        if (value && !down[button]) {
            tapped[button] = true;
        }
        down[button] = value;
    }

    /** Appui bref (un toucher rapide sur l'écran) : compte comme un appui pour l'image suivante. */
    public synchronized void tap(int button) {
        tapped[button] = true;
    }

    /** Toucher à l'écran, en coordonnées du jeu (0..W, 0..H). */
    public synchronized void tapAt(double x, double y) {
        if (tapCount < taps.length / 2) {
            taps[2 * tapCount] = x;
            taps[2 * tapCount + 1] = y;
            tapCount++;
        }
    }

    synchronized boolean anyTap() {
        return tapCount > 0;
    }

    /** Vrai si un toucher de cette image tombe dans le rectangle. */
    synchronized boolean tappedIn(double x, double y, double w, double h) {
        for (int i = 0; i < tapCount; i++) {
            double tx = taps[2 * i];
            double ty = taps[2 * i + 1];
            if (tx >= x && tx <= x + w && ty >= y && ty <= y + h) {
                return true;
            }
        }
        return false;
    }

    public synchronized void releaseAll() {
        for (int i = 0; i < COUNT; i++) {
            down[i] = false;
        }
    }

    public synchronized boolean isDown(int button) {
        return down[button];
    }

    /** À appeler à la fin de chaque image de jeu, pour détecter les nouveaux appuis. */
    synchronized void endFrame() {
        for (int i = 0; i < COUNT; i++) {
            prev[i] = down[i];
            tapped[i] = false;
        }
        tapCount = 0;
    }

    private synchronized boolean held(int b) {
        return down[b];
    }

    private synchronized boolean pressed(int b) {
        return (down[b] && !prev[b]) || tapped[b];
    }

    boolean left() {
        return held(LEFT);
    }

    boolean right() {
        return held(RIGHT);
    }

    boolean up() {
        return held(UP);
    }

    boolean down() {
        return held(DOWN);
    }

    boolean jumpHeld() {
        return held(JUMP);
    }

    boolean jumpPressed() {
        return pressed(JUMP);
    }

    boolean startPressed() {
        return pressed(START) || pressed(JUMP);
    }

    boolean upPressed() {
        return pressed(UP);
    }

    boolean downPressed() {
        return pressed(DOWN);
    }

    boolean leftPressed() {
        return pressed(LEFT);
    }

    boolean rightPressed() {
        return pressed(RIGHT);
    }

    boolean pausePressed() {
        return pressed(PAUSE);
    }

    boolean mutePressed() {
        return pressed(MUTE);
    }
}
