package henry.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import henry.Game;

/**
 * Surface de jeu : un fil d'exécution fait avancer le jeu à 60 images/s
 * et le dessine, agrandi pour remplir l'écran du téléphone.
 */
final class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private static final double STEP = 1.0 / 60.0;

    private final Game game = new Game(true);
    private final TouchControls controls = new TouchControls();
    private final CanvasGfx gfx = new CanvasGfx();
    /** Verrou séparé pour la taille : le fil de jeu ne doit jamais attendre le verrou de la vue. */
    private final Object sizeLock = new Object();
    private Thread thread;
    private volatile boolean running;
    private boolean surfaceReady;
    private boolean resumed;
    private int width;
    private int height;

    GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        setFocusable(true);
        setKeepScreenOn(true);
    }

    // ---------------------------------------------------------------- cycle de vie

    synchronized void resume() {
        resumed = true;
        startLoop();
    }

    synchronized void pause() {
        resumed = false;
        synchronized (game) {
            game.pause();
        }
        controls.releaseAll(game);
        stopLoop();
    }

    /** Bouton « retour » : met en pause pendant une partie, sinon laisse quitter. */
    boolean handleBack() {
        synchronized (game) {
            if (game.isPlaying()) {
                game.input.tap(henry.Input.PAUSE);
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized void surfaceCreated(SurfaceHolder holder) {
        surfaceReady = true;
        startLoop();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        synchronized (sizeLock) {
            width = w;
            height = h;
        }
        controls.layout(w, h);
    }

    @Override
    public synchronized void surfaceDestroyed(SurfaceHolder holder) {
        surfaceReady = false;
        stopLoop();
    }

    private void startLoop() {
        if (running || !surfaceReady || !resumed) {
            return;
        }
        running = true;
        thread = new Thread(this, "henry-jeu");
        thread.start();
    }

    private void stopLoop() {
        running = false;
        Thread t = thread;
        thread = null;
        if (t != null && t != Thread.currentThread()) {
            try {
                t.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ---------------------------------------------------------------- boucle de jeu

    @Override
    public void run() {
        long last = System.nanoTime();
        double accumulator = 0;
        while (running) {
            long now = System.nanoTime();
            accumulator += (now - last) / 1e9;
            last = now;
            if (accumulator > 0.25) {
                accumulator = 0.25;
            }
            synchronized (game) {
                while (accumulator >= STEP) {
                    game.update();
                    accumulator -= STEP;
                }
            }
            drawFrame();
        }
    }

    private void drawFrame() {
        SurfaceHolder holder = getHolder();
        Canvas c;
        try {
            c = Build.VERSION.SDK_INT >= 26 ? holder.lockHardwareCanvas() : holder.lockCanvas();
        } catch (RuntimeException e) {
            c = null;
        }
        if (c == null) {
            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return;
        }
        try {
            int w;
            int h;
            synchronized (sizeLock) {
                w = width > 0 ? width : c.getWidth();
                h = height > 0 ? height : c.getHeight();
            }
            c.drawColor(Color.BLACK);
            float scale = Math.min(w / (float) Game.W, h / (float) Game.H);
            float ox = (w - Game.W * scale) / 2f;
            float oy = (h - Game.H * scale) / 2f;
            boolean playing;
            c.save();
            c.translate(ox, oy);
            c.scale(scale, scale);
            c.clipRect(0, 0, Game.W, Game.H);
            synchronized (game) {
                game.render(gfx.target(c));
                playing = game.isPlaying() && !game.isPaused();
            }
            c.restore();
            controls.draw(c, playing);
        } finally {
            holder.unlockCanvasAndPost(c);
        }
    }

    // ---------------------------------------------------------------- toucher

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        synchronized (game) {
            controls.onTouch(event, game);
        }
        return true;
    }
}
