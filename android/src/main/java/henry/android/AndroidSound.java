package henry.android;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import henry.Sound;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Joue les bruitages du jeu avec SoundPool. Les sons sont synthétisés au démarrage
 * puis écrits en fichiers WAV dans le cache de l'application.
 */
final class AndroidSound implements Sound.Backend {

    private static SoundPool pool;

    private final int[] ids;

    private AndroidSound(int[] ids) {
        this.ids = ids;
    }

    static void install(Context context) {
        if (pool != null) {
            return;
        }
        try {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            SoundPool sp = new SoundPool.Builder().setMaxStreams(8).setAudioAttributes(attrs).build();
            int[] ids = new int[Sound.COUNT];
            File dir = context.getCacheDir();
            for (int i = 0; i < Sound.COUNT; i++) {
                File f = new File(dir, "henry_son_" + i + ".wav");
                writeWav(f, Sound.synthesize(i));
                ids[i] = sp.load(f.getAbsolutePath(), 1);
            }
            pool = sp;
            Sound.setBackend(new AndroidSound(ids));
        } catch (Exception e) {
            // pas de son : le jeu reste jouable
        }
    }

    static void release() {
        Sound.setBackend(null);
        if (pool != null) {
            pool.release();
            pool = null;
        }
    }

    @Override
    public void play(int id) {
        SoundPool sp = pool;
        if (sp != null) {
            sp.play(ids[id], 1f, 1f, 1, 0, 1f);
        }
    }

    /** En-tête WAV minimal pour du PCM 16 bits mono. */
    private static void writeWav(File file, byte[] pcm) throws IOException {
        int rate = Sound.RATE;
        byte[] header = new byte[44];
        putAscii(header, 0, "RIFF");
        putInt(header, 4, 36 + pcm.length);
        putAscii(header, 8, "WAVE");
        putAscii(header, 12, "fmt ");
        putInt(header, 16, 16);
        putShort(header, 20, 1);
        putShort(header, 22, 1);
        putInt(header, 24, rate);
        putInt(header, 28, rate * 2);
        putShort(header, 32, 2);
        putShort(header, 34, 16);
        putAscii(header, 36, "data");
        putInt(header, 40, pcm.length);
        FileOutputStream out = new FileOutputStream(file);
        try {
            out.write(header);
            out.write(pcm);
        } finally {
            out.close();
        }
    }

    private static void putAscii(byte[] b, int off, String s) {
        for (int i = 0; i < s.length(); i++) {
            b[off + i] = (byte) s.charAt(i);
        }
    }

    private static void putInt(byte[] b, int off, int v) {
        b[off] = (byte) v;
        b[off + 1] = (byte) (v >> 8);
        b[off + 2] = (byte) (v >> 16);
        b[off + 3] = (byte) (v >> 24);
    }

    private static void putShort(byte[] b, int off, int v) {
        b[off] = (byte) v;
        b[off + 1] = (byte) (v >> 8);
    }
}
