package henry;

import java.util.Random;

/**
 * Bruitages synthétisés par le code (aucun fichier audio nécessaire).
 * La plateforme fournit un {@link Backend} qui sait jouer les sons ;
 * sans backend (ou sans carte son), le jeu reste simplement muet.
 */
public final class Sound {

    public static final int RING = 0;
    public static final int JUMP = 1;
    public static final int SPRING = 2;
    public static final int LOSE_RINGS = 3;
    public static final int POP = 4;
    public static final int DIE = 5;
    public static final int CHECKPOINT = 6;
    public static final int REV = 7;
    public static final int DASH = 8;
    public static final int GOAL = 9;
    public static final int BOSS_HIT = 10;
    public static final int ONE_UP = 11;
    public static final int BOOM = 12;
    public static final int TICK = 13;
    public static final int ROLL = 14;
    public static final int COUNT = 15;

    /** Fréquence d'échantillonnage des sons générés (PCM 16 bits mono, little-endian). */
    public static final int RATE = 22050;

    /** Joue un son déjà préparé. */
    public interface Backend {
        void play(int id);
    }

    private static final int SQUARE = 0;
    private static final int TRIANGLE = 1;
    private static final int NOISE = 2;

    private static volatile Backend backend;
    private static volatile boolean enabled = true;

    private Sound() {
    }

    public static void setBackend(Backend b) {
        backend = b;
    }

    static void play(int id) {
        Backend b = backend;
        if (b != null && enabled) {
            try {
                b.play(id);
            } catch (RuntimeException ignored) {
                // un son raté ne doit jamais faire planter le jeu
            }
        }
    }

    public static void toggle() {
        enabled = !enabled;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /** Échantillons PCM 16 bits mono little-endian du son {@code id}. */
    public static byte[] synthesize(int id) {
        return synth(definition(id));
    }

    /** Chaque note : {fréquence début, fréquence fin, durée (s), forme d'onde, volume}. */
    private static double[][] definition(int id) {
        switch (id) {
            case RING:
                return new double[][] {{1319, 1319, 0.06, SQUARE, 0.18}, {1976, 1976, 0.16, SQUARE, 0.15}};
            case JUMP:
                return new double[][] {{280, 900, 0.16, SQUARE, 0.15}};
            case SPRING:
                return new double[][] {{180, 1100, 0.30, TRIANGLE, 0.35}};
            case LOSE_RINGS:
                return new double[][] {{1700, 500, 0.45, SQUARE, 0.15}};
            case POP:
                return new double[][] {{0, 0, 0.22, NOISE, 0.30}, {400, 120, 0.12, SQUARE, 0.12}};
            case DIE:
                return new double[][] {{900, 600, 0.12, SQUARE, 0.2}, {600, 80, 0.55, SQUARE, 0.2}};
            case CHECKPOINT:
                return new double[][] {{660, 660, 0.08, SQUARE, 0.15}, {880, 880, 0.08, SQUARE, 0.15},
                    {1320, 1320, 0.18, SQUARE, 0.15}};
            case REV:
                return new double[][] {{350, 1300, 0.14, SQUARE, 0.12}};
            case DASH:
                return new double[][] {{1300, 250, 0.25, SQUARE, 0.15}};
            case GOAL:
                return new double[][] {{523, 523, 0.10, SQUARE, 0.15}, {659, 659, 0.10, SQUARE, 0.15},
                    {784, 784, 0.10, SQUARE, 0.15}, {1047, 1047, 0.35, SQUARE, 0.15}};
            case BOSS_HIT:
                return new double[][] {{220, 60, 0.25, SQUARE, 0.3}, {0, 0, 0.1, NOISE, 0.2}};
            case ONE_UP:
                return new double[][] {{784, 784, 0.1, SQUARE, 0.15}, {659, 659, 0.1, SQUARE, 0.15},
                    {784, 784, 0.1, SQUARE, 0.15}, {1047, 1047, 0.1, SQUARE, 0.15}, {1319, 1319, 0.3, SQUARE, 0.15}};
            case BOOM:
                return new double[][] {{0, 0, 0.6, NOISE, 0.35}};
            case TICK:
                return new double[][] {{1200, 1200, 0.025, SQUARE, 0.08}};
            case ROLL:
            default:
                return new double[][] {{600, 300, 0.12, TRIANGLE, 0.2}};
        }
    }

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
