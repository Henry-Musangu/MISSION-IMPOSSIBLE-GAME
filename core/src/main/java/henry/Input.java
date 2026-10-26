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

    boolean pausePressed() {
        return pressed(PAUSE);
    }

    boolean mutePressed() {
        return pressed(MUTE);
    }
}
