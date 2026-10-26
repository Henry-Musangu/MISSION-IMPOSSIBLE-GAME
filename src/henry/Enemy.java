package henry;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/** Les robots du Dr. Vortex : la coccinelle-robot qui roule et la guêpe qui vole. */
final class Enemy {

    static final int WALKER = 0;
    static final int FLYER = 1;

    final int type;
    final int w;
    final int h;
    double x;
    double y;
    private final double homeX;
    private final double homeY;
    private int dir = -1;
    private int t;
    boolean alive = true;

    /** {@code baseX} : bord gauche de la tuile ; {@code baseY} : bas de la tuile. */
    Enemy(int type, double baseX, double baseY) {
        this.type = type;
        if (type == WALKER) {
            w = 30;
            h = 26;
        } else {
            w = 30;
            h = 22;
        }
        x = baseX + 1;
        y = baseY - h;
        homeX = x;
        homeY = y;
    }

    void update(Level level) {
        t++;
        if (type == WALKER) {
            x += dir;
            double front = dir > 0 ? x + w : x;
            int col = Level.toTile(front);
            boolean wall = level.isWall(col, Level.toTile(y + h - 4));
            byte below = level.tile(col, Level.toTile(y + h + 2));
            boolean edge = below == Level.EMPTY;
            if (wall || edge) {
                dir = -dir;
                x += dir * 2;
            }
        } else {
            double nx = homeX + Math.sin(t * 0.02) * 90;
            dir = nx > x ? 1 : -1;
            x = nx;
            y = homeY + Math.sin(t * 0.09) * 14;
        }
    }

    Rectangle2D bounds() {
        return new Rectangle2D.Double(x + 2, y + 2, w - 4, h - 2);
    }

    double centerX() {
        return x + w / 2.0;
    }

    void draw(Graphics2D g, double camX) {
        AffineTransform old = g.getTransform();
        g.translate(x - camX + w / 2.0, y + h);
        if (dir > 0) {
            g.scale(-1, 1);
        }
        if (type == WALKER) {
            drawWalker(g);
        } else {
            drawFlyer(g);
        }
        g.setTransform(old);
    }

    /** Dessin centré en bas, tourné vers la gauche. */
    private void drawWalker(Graphics2D g) {
        g.setColor(new Color(0x333333));
        g.fillOval(-13, -10, 10, 10);
        g.fillOval(3, -10, 10, 10);
        g.setColor(new Color(0xaaaaaa));
        int spin = (t / 4) % 2;
        g.fillOval(-10 + spin, -7, 4, 4);
        g.fillOval(6 + spin, -7, 4, 4);
        g.setColor(new Color(0xe03131));
        g.fillArc(-15, -28, 30, 30, 0, 180);
        g.setColor(Color.BLACK);
        g.fillRect(-15, -14, 30, 3);
        g.fillOval(-8, -24, 5, 5);
        g.fillOval(4, -22, 5, 5);
        g.fillOval(-2, -27, 4, 4);
        g.setColor(new Color(0x222222));
        g.fillOval(-20, -20, 10, 9);
        g.setColor(Color.WHITE);
        g.fillOval(-19, -19, 5, 5);
        g.setColor(Color.RED);
        g.fillOval(-18, -18, 3, 3);
    }

    private void drawFlyer(Graphics2D g) {
        g.setColor(new Color(255, 255, 255, 170));
        int flap = (t / 2) % 2 == 0 ? -10 : -5;
        g.fillOval(-6, -22 + flap / 2, 14, 10 - flap / 3);
        g.fillOval(2, -24 + flap / 2, 12, 9 - flap / 3);
        g.setColor(new Color(0xffc300));
        g.fillOval(-12, -16, 26, 14);
        g.setColor(Color.BLACK);
        g.fillRect(-2, -16, 4, 14);
        g.fillRect(6, -15, 3, 12);
        g.fillPolygon(new int[] {13, 20, 13}, new int[] {-12, -9, -6}, 3);
        g.setColor(new Color(0x444444));
        g.fillOval(-17, -17, 11, 11);
        g.setColor(new Color(0x7fe0ff));
        g.fillOval(-15, -14, 5, 5);
    }
}
