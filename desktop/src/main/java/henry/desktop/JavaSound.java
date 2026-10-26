package henry.desktop;

import henry.Music;
import henry.Sound;
import java.awt.GraphicsEnvironment;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

/** Joue les bruitages et la musique du jeu avec javax.sound. */
final class JavaSound implements Sound.Backend {

    private static final AudioFormat FORMAT = new AudioFormat(Sound.RATE, 16, 1, true, false);

    private final Clip[] clips;
    private final Clip[] songs = new Clip[Music.COUNT];
    private Clip playing;

    private JavaSound(Clip[] clips) {
        this.clips = clips;
    }

    /** Installe le son si la machine a une carte son ; sinon le jeu reste muet. */
    static void install() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        try {
            Clip[] clips = new Clip[Sound.COUNT];
            for (int i = 0; i < Sound.COUNT; i++) {
                clips[i] = open(Sound.synthesize(i));
            }
            Sound.setBackend(new JavaSound(clips));
        } catch (Throwable t) {
            // pas de son disponible
        }
    }

    private static Clip open(byte[] data) throws Exception {
        Clip clip = AudioSystem.getClip();
        clip.open(FORMAT, data, 0, data.length);
        return clip;
    }

    @Override
    public void play(int id) {
        Clip clip = clips[id];
        clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }

    @Override
    public synchronized void music(int track) {
        if (playing != null) {
            playing.stop();
            playing = null;
        }
        if (track < 0) {
            return;
        }
        try {
            if (songs[track] == null) {
                songs[track] = open(Music.render(track));
            }
            playing = songs[track];
            playing.setFramePosition(0);
            playing.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (Exception e) {
            // musique indisponible
        }
    }

    @Override
    public void vibrate(int millis) {
        // pas de vibreur sur un ordinateur
    }
}
