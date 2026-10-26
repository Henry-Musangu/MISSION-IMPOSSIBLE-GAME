package henry.desktop;

import henry.Sound;
import java.awt.GraphicsEnvironment;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

/** Joue les bruitages du jeu avec javax.sound. */
final class JavaSound implements Sound.Backend {

    private final Clip[] clips;

    private JavaSound(Clip[] clips) {
        this.clips = clips;
    }

    /** Installe le son si la machine a une carte son ; sinon le jeu reste muet. */
    static void install() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        try {
            AudioFormat format = new AudioFormat(Sound.RATE, 16, 1, true, false);
            Clip[] clips = new Clip[Sound.COUNT];
            for (int i = 0; i < Sound.COUNT; i++) {
                byte[] data = Sound.synthesize(i);
                Clip clip = AudioSystem.getClip();
                clip.open(format, data, 0, data.length);
                clips[i] = clip;
            }
            Sound.setBackend(new JavaSound(clips));
        } catch (Throwable t) {
            // pas de son disponible
        }
    }

    @Override
    public void play(int id) {
        Clip clip = clips[id];
        clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }
}
