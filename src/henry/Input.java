package henry;

import java.awt.event.KeyEvent;
import java.util.Arrays;

/**
 * État du clavier. Accepte les flèches, ZQSD (AZERTY) et WASD (QWERTY).
 * Saut : Espace, X ou K.
 */
final class Input {

    private final boolean[] down = new boolean[1024];
    private final boolean[] prev = new boolean[1024];

    void set(int code, boolean value) {
        if (code >= 0 && code < down.length) {
            down[code] = value;
        }
    }

    void releaseAll() {
        Arrays.fill(down, false);
    }

    /** À appeler à la fin de chaque image de jeu, pour détecter les nouveaux appuis. */
    void endFrame() {
        System.arraycopy(down, 0, prev, 0, down.length);
    }

    private boolean held(int... codes) {
        for (int c : codes) {
            if (down[c]) {
                return true;
            }
        }
        return false;
    }

    private boolean pressed(int... codes) {
        for (int c : codes) {
            if (down[c] && !prev[c]) {
                return true;
            }
        }
        return false;
    }

    boolean left() {
        return held(KeyEvent.VK_LEFT, KeyEvent.VK_Q, KeyEvent.VK_A);
    }

    boolean right() {
        return held(KeyEvent.VK_RIGHT, KeyEvent.VK_D);
    }

    boolean up() {
        return held(KeyEvent.VK_UP, KeyEvent.VK_Z, KeyEvent.VK_W);
    }

    boolean down() {
        return held(KeyEvent.VK_DOWN, KeyEvent.VK_S);
    }

    boolean jumpHeld() {
        return held(KeyEvent.VK_SPACE, KeyEvent.VK_X, KeyEvent.VK_K);
    }

    boolean jumpPressed() {
        return pressed(KeyEvent.VK_SPACE, KeyEvent.VK_X, KeyEvent.VK_K);
    }

    boolean startPressed() {
        return pressed(KeyEvent.VK_ENTER, KeyEvent.VK_SPACE);
    }

    boolean pausePressed() {
        return pressed(KeyEvent.VK_P, KeyEvent.VK_ESCAPE);
    }

    boolean mutePressed() {
        return pressed(KeyEvent.VK_M);
    }
}
