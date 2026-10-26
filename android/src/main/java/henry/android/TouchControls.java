package henry.android;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.view.MotionEvent;
import henry.Game;
import henry.Input;
import henry.Sound;

/**
 * Manette tactile : ◀ ▶ ▼ à gauche, gros bouton de saut à droite,
 * pause et son en haut à droite. Plusieurs doigts peuvent appuyer en même temps.
 */
final class TouchControls {

    private static final int LEFT = 0;
    private static final int RIGHT = 1;
    private static final int DOWN = 2;
    private static final int JUMP = 3;
    private static final int PAUSE = 4;
    private static final int MUTE = 5;
    private static final int COUNT = 6;

    private final float[] cx = new float[COUNT];
    private final float[] cy = new float[COUNT];
    private final float[] radius = new float[COUNT];
    private final boolean[] held = new boolean[COUNT];

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint icon = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    TouchControls() {
        ring.setStyle(Paint.Style.STROKE);
        icon.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        icon.setTextAlign(Paint.Align.CENTER);
    }

    /** Place les boutons selon la taille de l'écran (en pixels). */
    synchronized void layout(int width, int height) {
        float u = Math.min(height / 10f, width / 16f);
        place(LEFT, 1.55f * u, height - 2.2f * u, 1.05f * u);
        place(RIGHT, 4.25f * u, height - 2.2f * u, 1.05f * u);
        place(DOWN, 2.9f * u, height - 0.95f * u, 0.8f * u);
        place(JUMP, width - 2.1f * u, height - 2.1f * u, 1.45f * u);
        place(PAUSE, width - 0.9f * u, 0.9f * u, 0.55f * u);
        place(MUTE, width - 2.3f * u, 0.9f * u, 0.55f * u);
        ring.setStrokeWidth(Math.max(2f, u * 0.06f));
    }

    private void place(int b, float x, float y, float r) {
        cx[b] = x;
        cy[b] = y;
        radius[b] = r;
    }

    /** Bouton sous le doigt, ou -1. Les zones de toucher sont plus larges que les dessins. */
    private int hit(float x, float y, boolean playing) {
        int best = -1;
        float bestD = Float.MAX_VALUE;
        for (int b = 0; b < COUNT; b++) {
            if (!playing && b != MUTE) {
                continue;
            }
            float dx = x - cx[b];
            float dy = y - cy[b];
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            float reach = radius[b] * (b == PAUSE || b == MUTE ? 1.3f : 1.6f);
            if (d <= reach && d / radius[b] < bestD) {
                bestD = d / radius[b];
                best = b;
            }
        }
        return best;
    }

    /** Traduit les doigts posés sur l'écran en boutons du jeu. */
    synchronized void onTouch(MotionEvent e, Game game) {
        boolean playing = game.isPlaying() && !game.isPaused();
        int action = e.getActionMasked();
        Input in = game.input;

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            int i = e.getActionIndex();
            int b = hit(e.getX(i), e.getY(i), playing);
            if (b == PAUSE) {
                in.tap(Input.PAUSE);
            } else if (b == MUTE) {
                in.tap(Input.MUTE);
            } else if (game.isPaused()) {
                in.tap(Input.PAUSE);
            } else if (!game.isPlaying()) {
                in.tap(Input.START);
            }
        }

        for (int b = 0; b < COUNT; b++) {
            held[b] = false;
        }
        if (action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL) {
            int lifted = action == MotionEvent.ACTION_POINTER_UP ? e.getActionIndex() : -1;
            for (int i = 0; i < e.getPointerCount(); i++) {
                if (i == lifted) {
                    continue;
                }
                int b = hit(e.getX(i), e.getY(i), playing);
                if (b >= 0) {
                    held[b] = true;
                }
            }
        }
        in.set(Input.LEFT, held[LEFT]);
        in.set(Input.RIGHT, held[RIGHT]);
        in.set(Input.DOWN, held[DOWN]);
        in.set(Input.JUMP, held[JUMP]);
    }

    synchronized void releaseAll(Game game) {
        for (int b = 0; b < COUNT; b++) {
            held[b] = false;
        }
        game.input.releaseAll();
    }

    synchronized void draw(Canvas c, boolean playing) {
        if (playing) {
            drawButton(c, LEFT);
            drawButton(c, RIGHT);
            drawButton(c, DOWN);
            drawButton(c, JUMP);
            drawButton(c, PAUSE);
            arrow(c, LEFT, 180);
            arrow(c, RIGHT, 0);
            arrow(c, DOWN, 90);
            icon.setColor(0xccffffff);
            icon.setTextSize(radius[JUMP] * 0.9f);
            c.drawText("A", cx[JUMP], cy[JUMP] + radius[JUMP] * 0.32f, icon);
            float r = radius[PAUSE];
            c.drawRect(cx[PAUSE] - r * 0.35f, cy[PAUSE] - r * 0.4f, cx[PAUSE] - r * 0.1f, cy[PAUSE] + r * 0.4f, icon);
            c.drawRect(cx[PAUSE] + r * 0.1f, cy[PAUSE] - r * 0.4f, cx[PAUSE] + r * 0.35f, cy[PAUSE] + r * 0.4f, icon);
        }
        drawButton(c, MUTE);
        drawSpeaker(c);
    }

    private void drawButton(Canvas c, int b) {
        fill.setColor(held[b] ? 0x88ffffff : 0x40000000);
        c.drawCircle(cx[b], cy[b], radius[b], fill);
        ring.setColor(0x99ffffff);
        c.drawCircle(cx[b], cy[b], radius[b], ring);
    }

    private void arrow(Canvas c, int b, float degrees) {
        float r = radius[b] * 0.45f;
        c.save();
        c.translate(cx[b], cy[b]);
        c.rotate(degrees);
        path.reset();
        path.moveTo(r, 0);
        path.lineTo(-r * 0.6f, -r * 0.85f);
        path.lineTo(-r * 0.6f, r * 0.85f);
        path.close();
        icon.setColor(0xccffffff);
        c.drawPath(path, icon);
        c.restore();
    }

    private void drawSpeaker(Canvas c) {
        float r = radius[MUTE];
        float x = cx[MUTE];
        float y = cy[MUTE];
        icon.setColor(0xccffffff);
        path.reset();
        path.moveTo(x - r * 0.45f, y - r * 0.18f);
        path.lineTo(x - r * 0.2f, y - r * 0.18f);
        path.lineTo(x + r * 0.15f, y - r * 0.45f);
        path.lineTo(x + r * 0.15f, y + r * 0.45f);
        path.lineTo(x - r * 0.2f, y + r * 0.18f);
        path.lineTo(x - r * 0.45f, y + r * 0.18f);
        path.close();
        c.drawPath(path, icon);
        ring.setColor(0xccffffff);
        if (Sound.isEnabled()) {
            c.drawArc(x - r * 0.2f, y - r * 0.35f, x + r * 0.5f, y + r * 0.35f, -45, 90, false, ring);
        } else {
            c.drawLine(x + r * 0.3f, y - r * 0.25f, x + r * 0.6f, y + r * 0.25f, ring);
            c.drawLine(x + r * 0.6f, y - r * 0.25f, x + r * 0.3f, y + r * 0.25f, ring);
        }
    }
}
