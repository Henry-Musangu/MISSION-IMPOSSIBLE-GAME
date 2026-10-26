package henry;

import java.util.Random;

/**
 * Musiques du jeu, composées ici en notation texte et synthétisées en PCM
 * (style console 16 bits : ondes carrées, triangle et bruit pour la batterie).
 *
 * Notation d'une piste : un symbole par double-croche, séparés par des espaces.
 * « C5 » joue un do de l'octave 5, « - » prolonge la note précédente, « . » est un silence.
 * Accords : une lettre par mesure (ex. « Am »), joués en arpèges.
 * Batterie : « k » grosse caisse, « s » caisse claire, « h » charleston, « . » rien.
 */
public final class Music {

    public static final int TITLE = 0;
    public static final int PRAIRIE = 1;
    public static final int CANYON = 2;
    public static final int FACTORY = 3;
    public static final int BOSS = 4;
    public static final int COUNT = 5;

    private Music() {
    }

    /** Une chanson : tempo, accords par mesure, mélodie, basse et batterie. */
    private static final class Song {
        final double bpm;
        final String chords;
        final String lead;
        final String bass;
        final String drums;
        final int leadWave;
        final double swing;

        Song(double bpm, String chords, String lead, String bass, String drums, int leadWave, double swing) {
            this.bpm = bpm;
            this.chords = chords;
            this.lead = lead;
            this.bass = bass;
            this.drums = drums;
            this.leadWave = leadWave;
            this.swing = swing;
        }
    }

    private static final int SQUARE25 = 0;
    private static final int SQUARE50 = 1;
    private static final int TRIANGLE = 2;
    private static final int SQUARE12 = 3;

    private static Song song(int track) {
        switch (track) {
            case TITLE:
                return new Song(132, "G C D G G C D G",
                        "G4 - B4 - D5 - G5 - - - F#5 - G5 - A5 - | B5 - - - A5 - G5 - E5 - D5 - - - - - | "
                        + "F#5 - - - E5 - D5 - A4 - B4 - C5 - D5 - | D5 - - - - - - - . . B4 - D5 - G5 - | "
                        + "G5 - - - B5 - - - A5 - G5 - E5 - G5 - | C6 - - - B5 - A5 - G5 - - - E5 - - - | "
                        + "D5 - F#5 - A5 - D6 - - - C6 - B5 - A5 - | G5 - - - - - - - - - - - . . . .",
                        "G2 . G3 . G2 . G3 . G2 . G3 . D3 . B2 . | C3 . C4 . C3 . C4 . C3 . C4 . E3 . G3 . | "
                        + "D3 . D4 . D3 . D4 . D3 . D4 . A2 . F#3 . | G2 . G3 . G2 . G3 . G2 . D3 . G3 . B2 . | "
                        + "G2 . G3 . G2 . G3 . G2 . G3 . D3 . B2 . | C3 . C4 . C3 . C4 . C3 . C4 . E3 . G3 . | "
                        + "D3 . D4 . D3 . D4 . D3 . D4 . A2 . F#3 . | G2 . G3 . G2 . D3 . G2 - - - . . . .",
                        "k . h . s . h . k . h k s . h h", SQUARE25, 0);
            case PRAIRIE:
                return new Song(150, "C Am F G C Am Dm G",
                        "E5 - G5 - C6 - - - B5 - G5 - E5 - - - | A5 - - - G5 - E5 - C5 - D5 - E5 - - - | "
                        + "F5 - A5 - C6 - A5 - F5 - - - E5 - D5 - | D5 - - - G5 - - - B4 - D5 - G5 - - - | "
                        + "E5 - G5 - C6 - - - D6 - C6 - B5 - G5 - | A5 - - - C6 - B5 - A5 - G5 - E5 - - - | "
                        + "F5 - E5 - D5 - F5 - A5 - - - G5 - F5 - | G5 - - - B5 - - - D6 - - - . . . .",
                        "C3 . C4 . C3 . C4 . C3 . C4 . G2 . B2 . | A2 . A3 . A2 . A3 . A2 . A3 . E3 . G3 . | "
                        + "F2 . F3 . F2 . F3 . F2 . F3 . C3 . A2 . | G2 . G3 . G2 . G3 . G2 . G3 . D3 . B2 . | "
                        + "C3 . C4 . C3 . C4 . C3 . C4 . G2 . B2 . | A2 . A3 . A2 . A3 . A2 . A3 . E3 . G3 . | "
                        + "D3 . D4 . D3 . D4 . D3 . D4 . A2 . F3 . | G2 . G3 . G2 . G3 . G2 . B2 . D3 . F3 .",
                        "k . h . s . h k k . h . s . h h", SQUARE25, 0);
            case CANYON:
                return new Song(118, "Am F C G Am F E E",
                        "A4 - - - C5 - E5 - - - D5 - C5 - - - | A4 - - - - - - - . . F4 - A4 - C5 - | "
                        + "G5 - - - E5 - - - C5 - - - D5 - E5 - | D5 - - - - - - - B4 - - - G4 - - - | "
                        + "A4 - - - C5 - E5 - - - A5 - G5 - - - | F5 - - - E5 - C5 - - - A4 - C5 - D5 - | "
                        + "E5 - - - G#5 - - - B5 - - - G#5 - E5 - | E5 - - - - - - - D5 - - - B4 - - -",
                        "A2 - . A2 . . A2 . E3 . . E3 G3 . E3 . | F2 - . F2 . . F2 . C3 . . C3 E3 . C3 . | "
                        + "C3 - . C3 . . C3 . G3 . . G3 E3 . G3 . | G2 - . G2 . . G2 . D3 . . D3 B2 . D3 . | "
                        + "A2 - . A2 . . A2 . E3 . . E3 G3 . E3 . | F2 - . F2 . . F2 . C3 . . C3 E3 . C3 . | "
                        + "E2 - . E2 . . E2 . B2 . . B2 G#2 . B2 . | E2 - . E2 . . E2 . B2 . E3 . G#2 . B2 .",
                        "k . h h s . h . k k h . s . h .", TRIANGLE, 0.12);
            case FACTORY:
                return new Song(164, "Em Em C D Em Em C B",
                        "E5 . E5 . G5 . E5 . B5 - - - A5 - G5 - | F#5 - - - E5 - - - D5 - E5 - F#5 - - - | "
                        + "G5 . G5 . E5 . G5 . C6 - - - B5 - A5 - | A5 - - - F#5 - - - D5 - F#5 - A5 - - - | "
                        + "E5 . E5 . G5 . E5 . B5 - - - A5 - G5 - | F#5 - - - G5 - - - A5 - B5 - - - - - | "
                        + "C6 - B5 - A5 - G5 - E5 - - - G5 - - - | F#5 - - - D#5 - - - B4 - - - . . . .",
                        "E2 E2 E3 E2 E2 E3 E2 E2 E2 E2 E3 E2 E2 E3 D3 B2 | E2 E2 E3 E2 E2 E3 E2 E2 E2 E2 E3 E2 E2 E3 G2 A2 | "
                        + "C3 C3 C4 C3 C3 C4 C3 C3 C3 C3 C4 C3 C3 C4 B2 G2 | D3 D3 D4 D3 D3 D4 D3 D3 D3 D3 D4 D3 D3 D4 C3 A2 | "
                        + "E2 E2 E3 E2 E2 E3 E2 E2 E2 E2 E3 E2 E2 E3 D3 B2 | E2 E2 E3 E2 E2 E3 E2 E2 E2 E2 E3 E2 E2 E3 G2 A2 | "
                        + "C3 C3 C4 C3 C3 C4 C3 C3 C3 C3 C4 C3 C3 C4 B2 G2 | B2 B2 B3 B2 B2 B3 B2 B2 B2 B2 B3 B2 F#2 . B2 .",
                        "k h s h k k s h k h s h k k s s", SQUARE12, 0);
            default:
                return new Song(176, "Dm Dm Bb A Dm Dm Bb A",
                        "D5 - - - F5 - - - A5 - - - G#5 - A5 - | D6 - - - C6 - A5 - - - F5 - G5 - - - | "
                        + "F5 - - - D5 - - - Bb4 - D5 - F5 - - - | E5 - - - C#5 - - - A4 - - - E5 - - - | "
                        + "D5 - - - F5 - - - A5 - - - G#5 - A5 - | D6 - - - E6 - F6 - - - E6 - D6 - - - | "
                        + "Bb5 - - - A5 - G5 - F5 - - - D5 - - - | E5 - - - G5 - - - A5 - - - C#6 - - -",
                        "D2 D2 D3 D2 D#2 D2 D3 D2 D2 D2 D3 D2 D#2 D2 C3 A2 | D2 D2 D3 D2 D#2 D2 D3 D2 D2 D2 D3 D2 D#2 D2 C3 A2 | "
                        + "Bb1 Bb1 Bb2 Bb1 B1 Bb1 Bb2 Bb1 Bb1 Bb1 Bb2 Bb1 B1 Bb1 F2 D2 | A1 A1 A2 A1 Bb1 A1 A2 A1 A1 A1 A2 A1 Bb1 A1 E2 C#2 | "
                        + "D2 D2 D3 D2 D#2 D2 D3 D2 D2 D2 D3 D2 D#2 D2 C3 A2 | D2 D2 D3 D2 D#2 D2 D3 D2 D2 D2 D3 D2 D#2 D2 C3 A2 | "
                        + "Bb1 Bb1 Bb2 Bb1 B1 Bb1 Bb2 Bb1 Bb1 Bb1 Bb2 Bb1 B1 Bb1 F2 D2 | A1 A1 A2 A1 Bb1 A1 A2 A1 A1 A1 A2 A1 Bb1 A1 E2 C#2",
                        "k h s h k h s k k h s h k s s s", SQUARE25, 0);
        }
    }

    /** PCM 16 bits mono little-endian de la musique, prête à jouer en boucle. */
    public static byte[] render(int track) {
        Song s = song(track);
        String[] lead = tokens(s.lead);
        String[] bass = tokens(s.bass);
        String[] chords = s.chords.trim().split("\\s+");
        int steps = lead.length;
        double stepSec = 60.0 / s.bpm / 4;
        int rate = Sound.RATE;
        int total = (int) Math.round(steps * stepSec * rate);
        float[] mix = new float[total];

        voice(mix, lead, stepSec, s.leadWave, 0.20, true, s.swing);
        voice(mix, bass, stepSec, TRIANGLE, 0.30, false, s.swing);
        arpeggio(mix, chords, steps, stepSec, s.swing);
        drums(mix, s.drums.trim().split("\\s+"), steps, stepSec, s.swing);

        float peak = 0.0001f;
        for (float v : mix) {
            peak = Math.max(peak, Math.abs(v));
        }
        float gain = 0.85f / peak;
        byte[] out = new byte[total * 2];
        for (int i = 0; i < total; i++) {
            int v = (int) (mix[i] * gain * 32767);
            out[2 * i] = (byte) v;
            out[2 * i + 1] = (byte) (v >> 8);
        }
        return out;
    }

    private static String[] tokens(String text) {
        return text.replace("|", " ").trim().split("\\s+");
    }

    /** Début (en échantillons) de la double-croche {@code step}, avec un peu de « swing » éventuel. */
    private static int start(int step, double stepSec, double swing) {
        double t = step * stepSec;
        if (step % 2 == 1) {
            t += swing * stepSec;
        }
        return (int) (t * Sound.RATE);
    }

    private static void voice(float[] mix, String[] notes, double stepSec, int wave, double vol,
            boolean vibrato, double swing) {
        int i = 0;
        while (i < notes.length) {
            String n = notes[i];
            if (n.equals("-") || n.equals(".")) {
                i++;
                continue;
            }
            int len = 1;
            while (i + len < notes.length && notes[i + len].equals("-")) {
                len++;
            }
            double freq = freq(n);
            int a = start(i, stepSec, swing);
            int b = Math.min(mix.length, start(i + len, stepSec, swing));
            tone(mix, a, b, freq, wave, vol, vibrato);
            i += len;
        }
    }

    private static void tone(float[] mix, int a, int b, double freq, int wave, double vol, boolean vibrato) {
        double phase = 0;
        int len = b - a;
        int rate = Sound.RATE;
        for (int k = 0; k < len; k++) {
            double t = k / (double) rate;
            double f = freq;
            if (vibrato && t > 0.15) {
                f *= 1 + 0.006 * Math.sin(2 * Math.PI * 5.5 * t);
            }
            phase += f / rate;
            double p = phase - Math.floor(phase);
            double sample;
            switch (wave) {
                case SQUARE12:
                    sample = p < 0.125 ? 1 : -1;
                    break;
                case SQUARE25:
                    sample = p < 0.25 ? 1 : -1;
                    break;
                case SQUARE50:
                    sample = p < 0.5 ? 1 : -1;
                    break;
                default:
                    sample = 4 * Math.abs(p - 0.5) - 1;
                    break;
            }
            double env = Math.min(1, k / (rate * 0.004));
            env *= 0.55 + 0.45 * Math.exp(-t * 6);
            int rel = len - k;
            if (rel < rate * 0.02) {
                env *= rel / (rate * 0.02);
            }
            mix[a + k] += (float) (sample * env * vol);
        }
    }

    /** Arpèges rapides sur l'accord de chaque mesure, discrets en fond. */
    private static void arpeggio(float[] mix, String[] chords, int steps, double stepSec, double swing) {
        int bars = steps / 16;
        for (int bar = 0; bar < bars; bar++) {
            int[] notes = chord(chords[bar % chords.length]);
            for (int st = 0; st < 16; st++) {
                int step = bar * 16 + st;
                int a = start(step, stepSec, swing);
                int b = Math.min(mix.length, a + (int) (stepSec * Sound.RATE * 0.8));
                int midi = notes[st % 3] + 12 * ((st / 3) % 2);
                tone(mix, a, b, 440 * Math.pow(2, (midi - 69) / 12.0), SQUARE12, 0.05, false);
            }
        }
    }

    private static void drums(float[] mix, String[] pattern, int steps, double stepSec, double swing) {
        Random rnd = new Random(7);
        int rate = Sound.RATE;
        for (int step = 0; step < steps; step++) {
            String d = pattern[step % pattern.length];
            int a = start(step, stepSec, swing);
            if (d.equals("k")) {
                int len = (int) (rate * 0.12);
                double phase = 0;
                for (int k = 0; k < len && a + k < mix.length; k++) {
                    double t = k / (double) rate;
                    phase += (150 * Math.exp(-t * 30) + 45) / rate;
                    mix[a + k] += (float) (Math.sin(2 * Math.PI * phase) * Math.exp(-t * 22) * 0.45);
                }
            } else if (d.equals("s")) {
                int len = (int) (rate * 0.11);
                for (int k = 0; k < len && a + k < mix.length; k++) {
                    double t = k / (double) rate;
                    double noise = rnd.nextDouble() * 2 - 1;
                    double body = Math.sin(2 * Math.PI * 190 * t);
                    mix[a + k] += (float) ((noise * 0.7 + body * 0.3) * Math.exp(-t * 28) * 0.28);
                }
            } else if (d.equals("h")) {
                int len = (int) (rate * 0.03);
                double last = 0;
                for (int k = 0; k < len && a + k < mix.length; k++) {
                    double t = k / (double) rate;
                    double noise = rnd.nextDouble() * 2 - 1;
                    double high = noise - last;
                    last = noise;
                    mix[a + k] += (float) (high * Math.exp(-t * 120) * 0.07);
                }
            }
        }
    }

    /** Notes MIDI de la triade (fondamentale à l'octave 4). */
    private static int[] chord(String name) {
        boolean minor = name.length() > 1 && name.endsWith("m");
        String root = minor ? name.substring(0, name.length() - 1) : name;
        int r = noteIndex(root) + 60;
        return new int[] {r, r + (minor ? 3 : 4), r + 7};
    }

    private static int noteIndex(String n) {
        int base;
        switch (n.charAt(0)) {
            case 'C':
                base = 0;
                break;
            case 'D':
                base = 2;
                break;
            case 'E':
                base = 4;
                break;
            case 'F':
                base = 5;
                break;
            case 'G':
                base = 7;
                break;
            case 'A':
                base = 9;
                break;
            default:
                base = 11;
                break;
        }
        if (n.length() > 1 && n.charAt(1) == '#') {
            base++;
        } else if (n.length() > 1 && n.charAt(1) == 'b') {
            base--;
        }
        return base;
    }

    /** Fréquence d'une note écrite comme « C#5 » ou « Bb1 ». */
    static double freq(String n) {
        int octave = n.charAt(n.length() - 1) - '0';
        int midi = noteIndex(n.substring(0, n.length() - 1)) + 12 * (octave + 1);
        return 440 * Math.pow(2, (midi - 69) / 12.0);
    }
}
