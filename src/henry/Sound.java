package henry;

import java.awt.GraphicsEnvironment;
import java.util.Random;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

/**
 * Bruitages synthétisés au démarrage (aucun fichier audio nécessaire).
 * Si la machine n'a pas de carte son, le jeu reste simplement muet.
 */
final class Sound {

    static final int RING = 0;
    static final int JUMP = 1;
    static final int SPRING = 2;
    static final int LOSE_RINGS = 3;
    static final int POP = 4;
    static final int DIE = 5;
    static final int CHECKPOINT = 6;
    static final int REV = 7;
    static final int DASH = 8;
    static final int GOAL = 9;
    static final int BOSS_HIT = 10;
    static final int ONE_UP = 11;
    static final int BOOM = 12;
    static final int TICK = 13;
    static final int ROLL = 14;
    private static final int COUNT = 15;

    private static final float RATE = 22050f;
    private static final int SQUARE = 0;
    private static final int TRIANGLE = 1;
    private static final int NOISE = 2;

    private static Clip[] clips;
    private static boolean enabled = true;

    private Sound() {
    }

    static void init() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        try {
            double[][][] defs = new double[COUNT][][];
            defs[RING] = new double[][] {{1319, 1319, 0.06, SQUARE, 0.18}, {1976, 1976, 0.16, SQUARE, 0.15}};
            defs[JUMP] = new double[][] {{280, 900, 0.16, SQUARE, 0.15}};
            defs[SPRING] = new double[][] {{180, 1100, 0.30, TRIANGLE, 0.35}};
            defs[LOSE_RINGS] = new double[][] {{1700, 500, 0.45, SQUARE, 0.15}};
            defs[POP] = new double[][] {{0, 0, 0.22, NOISE, 0.30}, {400, 120, 0.12, SQUARE, 0.12}};
            defs[DIE] = new double[][] {{900, 600, 0.12, SQUARE, 0.2}, {600, 80, 0.55, SQUARE, 0.2}};
            defs[CHECKPOINT] = new double[][] {{660, 660, 0.08, SQUARE, 0.15}, {880, 880, 0.08, SQUARE, 0.15},
                {1320, 1320, 0.18, SQUARE, 0.15}};
            defs[REV] = new double[][] {{350, 1300, 0.14, SQUARE, 0.12}};
            defs[DASH] = new double[][] {{1300, 250, 0.25, SQUARE, 0.15}};
            defs[GOAL] = new double[][] {{523, 523, 0.10, SQUARE, 0.15}, {659, 659, 0.10, SQUARE, 0.15},
                {784, 784, 0.10, SQUARE, 0.15}, {1047, 1047, 0.35, SQUARE, 0.15}};
            defs[BOSS_HIT] = new double[][] {{220, 60, 0.25, SQUARE, 0.3}, {0, 0, 0.1, NOISE, 0.2}};
            defs[ONE_UP] = new double[][] {{784, 784, 0.1, SQUARE, 0.15}, {659, 659, 0.1, SQUARE, 0.15},
                {784, 784, 0.1, SQUARE, 0.15}, {1047, 1047, 0.1, SQUARE, 0.15}, {1319, 1319, 0.3, SQUARE, 0.15}};
            defs[BOOM] = new double[][] {{0, 0, 0.6, NOISE, 0.35}};
            defs[TICK] = new double[][] {{1200, 1200, 0.025, SQUARE, 0.08}};
            defs[ROLL] = new double[][] {{600, 300, 0.12, TRIANGLE, 0.2}};

            AudioFormat format = new AudioFormat(RATE, 16, 1, true, false);
            Clip[] built = new Clip[COUNT];
            for (int i = 0; i < COUNT; i++) {
                byte[] data = synth(defs[i]);
                Clip clip = AudioSystem.getClip();
                clip.open(format, data, 0, data.length);
                built[i] = clip;
            }
            clips = built;
        } catch (Throwable t) {
            clips = null;
        }
    }

    static void play(int id) {
        Clip[] c = clips;
        if (c == null || !enabled) {
            return;
        }
        Clip clip = c[id];
        clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }

    static void toggle() {
        enabled = !enabled;
    }

    static boolean isEnabled() {
        return enabled;
    }

    /** Chaque note : {fréquence début, fréquence fin, durée (s), forme d'onde, volume}. */
    private static byte[] synth(double[][] notes) {
        int total = 0;
        for (double[] n : notes) {
            total += (int) (n[2] * RATE);
        }
        byte[] out = new byte[total * 2];
        Random rnd = new Random(42);
        int pos = 0;
        double phase = 0;
        for (double[] n : notes) {
            int len = (int) (n[2] * RATE);
            int wave = (int) n[3];
            double noiseValue = 0;
            for (int i = 0; i < len; i++) {
                double t = i / (double) len;
                double freq = n[0] + (n[1] - n[0]) * t;
                phase += freq / RATE;
                double s;
                if (wave == NOISE) {
                    if (i % 3 == 0) {
                        noiseValue = rnd.nextDouble() * 2 - 1;
                    }
                    s = noiseValue;
                } else if (wave == TRIANGLE) {
                    double p = phase - Math.floor(phase);
                    s = 4 * Math.abs(p - 0.5) - 1;
                } else {
                    s = (phase - Math.floor(phase)) < 0.5 ? 1 : -1;
                }
                double env = t > 0.6 ? (1 - t) / 0.4 : 1;
                int v = (int) (s * env * n[4] * 32767);
                out[pos++] = (byte) v;
                out[pos++] = (byte) (v >> 8);
            }
        }
        return out;
    }
}
