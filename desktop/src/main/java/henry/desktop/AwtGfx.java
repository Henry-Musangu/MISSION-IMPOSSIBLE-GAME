package henry.desktop;

import henry.Gfx;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;

/** Implémentation de {@link Gfx} avec Java2D. */
public final class AwtGfx implements Gfx {

    private static final class AwtSprite implements Sprite {
        final BufferedImage image;

        AwtSprite(BufferedImage image) {
            this.image = image;
        }
    }

    private final Graphics2D g;
    private final Deque<AffineTransform> stack = new ArrayDeque<>();
    private final Font[] fontCache = new Font[2];

    public AwtGfx(Graphics2D g) {
        this.g = g;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    @Override
    public void setColor(int argb) {
        g.setColor(new Color(argb, true));
    }

    @Override
    public void setStroke(float width) {
        g.setStroke(new BasicStroke(width));
    }

    @Override
    public void fillRect(double x, double y, double w, double h) {
        g.fill(new Rectangle2D.Double(x, y, w, h));
    }

    @Override
    public void fillRoundRect(double x, double y, double w, double h, double arc) {
        g.fill(new RoundRectangle2D.Double(x, y, w, h, arc, arc));
    }

    @Override
    public void drawRoundRect(double x, double y, double w, double h, double arc) {
        g.draw(new RoundRectangle2D.Double(x, y, w, h, arc, arc));
    }

    @Override
    public void fillOval(double x, double y, double w, double h) {
        g.fill(new Ellipse2D.Double(x, y, w, h));
    }

    @Override
    public void drawOval(double x, double y, double w, double h) {
        g.draw(new Ellipse2D.Double(x, y, w, h));
    }

    @Override
    public void fillArc(double x, double y, double w, double h, double start, double extent) {
        g.fill(new Arc2D.Double(x, y, w, h, start, extent, Arc2D.PIE));
    }

    @Override
    public void drawArc(double x, double y, double w, double h, double start, double extent) {
        g.draw(new Arc2D.Double(x, y, w, h, start, extent, Arc2D.OPEN));
    }

    @Override
    public void drawLine(double x1, double y1, double x2, double y2) {
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    private static Path2D path(double[] xs, double[] ys, int n) {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(xs[0], ys[0]);
        for (int i = 1; i < n; i++) {
            p.lineTo(xs[i], ys[i]);
        }
        p.closePath();
        return p;
    }

    @Override
    public void fillPolygon(double[] xs, double[] ys, int n) {
        g.fill(path(xs, ys, n));
    }

    @Override
    public void drawPolygon(double[] xs, double[] ys, int n) {
        g.draw(path(xs, ys, n));
    }

    @Override
    public void fillVerticalGradient(double x, double y, double w, double h, int top, int bottom) {
        g.setPaint(new GradientPaint(0, (float) y, new Color(top, true), 0, (float) (y + h), new Color(bottom, true)));
        g.fill(new Rectangle2D.Double(x, y, w, h));
    }

    @Override
    public void setFont(float size, boolean italic) {
        int i = italic ? 1 : 0;
        Font f = fontCache[i];
        if (f == null) {
            f = new Font(Font.SANS_SERIF, italic ? Font.BOLD | Font.ITALIC : Font.BOLD, 10);
            fontCache[i] = f;
        }
        g.setFont(f.deriveFont(size));
    }

    @Override
    public float textWidth(String s) {
        return (float) g.getFont().getStringBounds(s, g.getFontRenderContext()).getWidth();
    }

    @Override
    public void drawText(String s, double x, double y) {
        g.drawString(s, (float) x, (float) y);
    }

    @Override
    public void save() {
        stack.push(g.getTransform());
    }

    @Override
    public void restore() {
        g.setTransform(stack.pop());
    }

    @Override
    public void translate(double dx, double dy) {
        g.translate(dx, dy);
    }

    @Override
    public void scale(double sx, double sy) {
        g.scale(sx, sy);
    }

    @Override
    public void rotate(double radians) {
        g.rotate(radians);
    }

    @Override
    public Sprite makeSprite(int w, int h, Painter painter) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sg = img.createGraphics();
        painter.paint(new AwtGfx(sg));
        sg.dispose();
        return new AwtSprite(img);
    }

    @Override
    public void drawSprite(Sprite sprite, double x, double y) {
        AwtSprite s = (AwtSprite) sprite;
        if (x == Math.rint(x) && y == Math.rint(y)) {
            g.drawImage(s.image, (int) x, (int) y, null);
        } else {
            g.drawImage(s.image, AffineTransform.getTranslateInstance(x, y), null);
        }
    }
}
