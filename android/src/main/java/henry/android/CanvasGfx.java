package henry.android;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import henry.Gfx;

/** Implémentation de {@link Gfx} avec le Canvas d'Android. */
final class CanvasGfx implements Gfx {

    /** Les tuiles sont dessinées en plus grand pour rester nettes une fois agrandies. */
    private static final int SPRITE_SCALE = 3;

    private static final Typeface BOLD = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
    private static final Typeface BOLD_ITALIC = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC);

    private static final class BitmapSprite implements Sprite {
        final Bitmap bitmap;
        final int w;
        final int h;

        BitmapSprite(Bitmap bitmap, int w, int h) {
            this.bitmap = bitmap;
            this.w = w;
            this.h = h;
        }
    }

    private Canvas c;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gradient = new Paint();
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final RectF rect = new RectF();
    private final Path path = new Path();
    private int gradTop;
    private int gradBottom;
    private float gradY0 = Float.NaN;
    private float gradY1;

    CanvasGfx() {
        fill.setStyle(Paint.Style.FILL);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.SQUARE);
        stroke.setStrokeJoin(Paint.Join.MITER);
        stroke.setStrokeWidth(1f);
        text.setTypeface(BOLD);
    }

    CanvasGfx(Canvas canvas) {
        this();
        c = canvas;
    }

    /** Change le canvas cible (un nouveau à chaque image sur Android). */
    CanvasGfx target(Canvas canvas) {
        c = canvas;
        return this;
    }

    private RectF rect(double x, double y, double w, double h) {
        rect.set((float) x, (float) y, (float) (x + w), (float) (y + h));
        return rect;
    }

    @Override
    public void setColor(int argb) {
        fill.setColor(argb);
        stroke.setColor(argb);
        text.setColor(argb);
    }

    @Override
    public void setStroke(float width) {
        stroke.setStrokeWidth(width);
    }

    @Override
    public void fillRect(double x, double y, double w, double h) {
        c.drawRect((float) x, (float) y, (float) (x + w), (float) (y + h), fill);
    }

    @Override
    public void fillRoundRect(double x, double y, double w, double h, double arc) {
        float r = (float) (arc / 2);
        c.drawRoundRect(rect(x, y, w, h), r, r, fill);
    }

    @Override
    public void drawRoundRect(double x, double y, double w, double h, double arc) {
        float r = (float) (arc / 2);
        c.drawRoundRect(rect(x, y, w, h), r, r, stroke);
    }

    @Override
    public void fillOval(double x, double y, double w, double h) {
        c.drawOval(rect(x, y, w, h), fill);
    }

    @Override
    public void drawOval(double x, double y, double w, double h) {
        c.drawOval(rect(x, y, w, h), stroke);
    }

    /**
     * Java2D compte les angles dans le sens inverse des aiguilles d'une montre,
     * Android dans le sens des aiguilles : on retourne l'arc.
     */
    private static float androidStart(double start, double extent) {
        return (float) (extent >= 0 ? -(start + extent) : -start);
    }

    @Override
    public void fillArc(double x, double y, double w, double h, double start, double extent) {
        c.drawArc(rect(x, y, w, h), androidStart(start, extent), (float) Math.abs(extent), true, fill);
    }

    @Override
    public void drawArc(double x, double y, double w, double h, double start, double extent) {
        c.drawArc(rect(x, y, w, h), androidStart(start, extent), (float) Math.abs(extent), false, stroke);
    }

    @Override
    public void drawLine(double x1, double y1, double x2, double y2) {
        c.drawLine((float) x1, (float) y1, (float) x2, (float) y2, stroke);
    }

    private Path polygon(double[] xs, double[] ys, int n) {
        path.reset();
        path.moveTo((float) xs[0], (float) ys[0]);
        for (int i = 1; i < n; i++) {
            path.lineTo((float) xs[i], (float) ys[i]);
        }
        path.close();
        return path;
    }

    @Override
    public void fillPolygon(double[] xs, double[] ys, int n) {
        c.drawPath(polygon(xs, ys, n), fill);
    }

    @Override
    public void drawPolygon(double[] xs, double[] ys, int n) {
        c.drawPath(polygon(xs, ys, n), stroke);
    }

    @Override
    public void fillVerticalGradient(double x, double y, double w, double h, int top, int bottom) {
        float y0 = (float) y;
        float y1 = (float) (y + h);
        if (top != gradTop || bottom != gradBottom || y0 != gradY0 || y1 != gradY1) {
            gradient.setShader(new LinearGradient(0, y0, 0, y1, top, bottom, Shader.TileMode.CLAMP));
            gradTop = top;
            gradBottom = bottom;
            gradY0 = y0;
            gradY1 = y1;
        }
        c.drawRect((float) x, y0, (float) (x + w), y1, gradient);
    }

    @Override
    public void setFont(float size, boolean italic) {
        text.setTypeface(italic ? BOLD_ITALIC : BOLD);
        text.setTextSize(size);
    }

    @Override
    public float textWidth(String s) {
        return text.measureText(s);
    }

    @Override
    public void drawText(String s, double x, double y) {
        c.drawText(s, (float) x, (float) y, text);
    }

    @Override
    public void clipPolygon(double[] xs, double[] ys, int n) {
        c.clipPath(polygon(xs, ys, n));
    }

    @Override
    public void save() {
        c.save();
    }

    @Override
    public void restore() {
        c.restore();
    }

    @Override
    public void translate(double dx, double dy) {
        c.translate((float) dx, (float) dy);
    }

    @Override
    public void scale(double sx, double sy) {
        c.scale((float) sx, (float) sy);
    }

    @Override
    public void rotate(double radians) {
        c.rotate((float) Math.toDegrees(radians));
    }

    @Override
    public Sprite makeSprite(int w, int h, Painter painter) {
        Bitmap bmp = Bitmap.createBitmap(w * SPRITE_SCALE, h * SPRITE_SCALE, Bitmap.Config.ARGB_8888);
        Canvas bc = new Canvas(bmp);
        bc.scale(SPRITE_SCALE, SPRITE_SCALE);
        painter.paint(new CanvasGfx(bc));
        return new BitmapSprite(bmp, w, h);
    }

    @Override
    public void drawSprite(Sprite sprite, double x, double y) {
        BitmapSprite s = (BitmapSprite) sprite;
        // léger chevauchement pour éviter les fines lignes entre deux tuiles agrandies
        rect.set((float) x, (float) y, (float) (x + s.w + 0.4), (float) (y + s.h + 0.4));
        c.drawBitmap(s.bitmap, null, rect, bitmapPaint);
    }
}
