package henry.android;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import henry.Music;
import henry.Sound;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Sons sur Android : bruitages avec SoundPool, musique en boucle avec MediaPlayer, vibreur.
 * Tout est synthétisé au démarrage puis écrit en fichiers WAV dans le cache de l'application ;
 * les musiques sont préparées en arrière-plan pour ne pas ralentir le lancement.
 */
final class AndroidSound implements Sound.Backend {

    private static AndroidSound instance;

    private final SoundPool pool;
    private final int[] ids;
    private final File[] songFiles = new File[Music.COUNT];
    private final Vibrator vibrator;
    private MediaPlayer player;
    private int wanted = -1;
    private int playingTrack = -1;
    private boolean suspended;

    private AndroidSound(Context context, SoundPool pool, int[] ids) {
        this.pool = pool;
        this.ids = ids;
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    static void install(Context context) {
        if (instance != null) {
            return;
        }
        try {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            SoundPool sp = new SoundPool.Builder().setMaxStreams(8).setAudioAttributes(attrs).build();
            int[] ids = new int[Sound.COUNT];
            final File dir = context.getCacheDir();
            for (int i = 0; i < Sound.COUNT; i++) {
                File f = new File(dir, "henry_son_" + i + ".wav");
                writeWav(f, Sound.synthesize(i));
                ids[i] = sp.load(f.getAbsolutePath(), 1);
            }
            final AndroidSound s = new AndroidSound(context.getApplicationContext(), sp, ids);
            instance = s;
            Sound.setBackend(s);
            Thread prepare = new Thread(new Runnable() {
                @Override
                public void run() {
                    s.prepareSongs(dir);
                }
            }, "henry-musique");
            prepare.setPriority(Thread.MIN_PRIORITY);
            prepare.start();
        } catch (Exception e) {
            // pas de son : le jeu reste jouable
        }
    }

    /** Synthétise toutes les musiques (une seule fois) dans des fichiers WAV. */
    private void prepareSongs(File dir) {
        for (int t = 0; t < Music.COUNT; t++) {
            try {
                File f = new File(dir, "henry_musique_v1_" + t + ".wav");
                if (!f.exists() || f.length() < 1000) {
                    File tmp = new File(dir, "henry_musique_tmp.wav");
                    writeWav(tmp, Music.render(t));
                    if (!tmp.renameTo(f)) {
                        continue;
                    }
                }
                synchronized (this) {
                    songFiles[t] = f;
                    if (wanted == t && playingTrack != t) {
                        startTrack(t);
                    }
                }
            } catch (IOException e) {
                // cette musique ne sera pas jouée
            }
        }
    }

    static void release() {
        Sound.setBackend(null);
        AndroidSound s = instance;
        instance = null;
        if (s != null) {
            synchronized (s) {
                s.stopPlayer();
            }
            s.pool.release();
        }
    }

    /** L'application passe en arrière-plan : la musique s'arrête. */
    static void suspend() {
        AndroidSound s = instance;
        if (s != null) {
            synchronized (s) {
                s.suspended = true;
                if (s.player != null && s.player.isPlaying()) {
                    s.player.pause();
                }
            }
        }
    }

    /** Retour au premier plan : la musique reprend là où elle était. */
    static void resume() {
        AndroidSound s = instance;
        if (s != null) {
            synchronized (s) {
                s.suspended = false;
                if (s.player != null && s.wanted >= 0) {
                    s.player.start();
                }
            }
        }
    }

    @Override
    public void play(int id) {
        pool.play(ids[id], 1f, 1f, 1, 0, 1f);
    }

    @Override
    public synchronized void music(int track) {
        wanted = track;
        if (track == playingTrack) {
            return;
        }
        stopPlayer();
        if (track >= 0 && songFiles[track] != null) {
            startTrack(track);
        }
    }

    private void startTrack(int track) {
        stopPlayer();
        try {
            MediaPlayer mp = new MediaPlayer();
            mp.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            mp.setDataSource(songFiles[track].getAbsolutePath());
            mp.setLooping(true);
            mp.setVolume(0.55f, 0.55f);
            mp.prepare();
            if (!suspended) {
                mp.start();
            }
            player = mp;
            playingTrack = track;
        } catch (Exception e) {
            playingTrack = -1;
        }
    }

    private void stopPlayer() {
        if (player != null) {
            try {
                player.stop();
            } catch (IllegalStateException ignored) {
                // déjà arrêté
            }
            player.release();
            player = null;
        }
        playingTrack = -1;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void vibrate(int millis) {
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(millis);
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
