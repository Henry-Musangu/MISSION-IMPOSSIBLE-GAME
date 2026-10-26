package henry;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

/** Borne de sauvegarde : après une chute, Henry repart d'ici. */
final class Checkpoint {

    private static final int HEIGHT = 64;

    final double x;
    final double base;
    boolean active;
    int spin;

    Checkpoint(double x, double base) {
        this.x = x;
        this.base = base;
    }

    Rectangle2D bounds() {
        return new Rectangle2D.Double(x + 4, base - HEIGHT, Game.T - 8, HEIGHT);
    }

    void update() {
        if (spin > 0) {
            spin--;
        }
    }

    void draw(Graphics2D g, double camX) {
        int cx = (int) (x - camX) + Game.T / 2;
        int b = (int) base;
        g.setColor(new Color(0x3b5bdb));
        g.fillRect(cx - 3, b - HEIGHT + 10, 6, HEIGHT - 10);
        g.setColor(new Color(0x1c2f8a));
        g.fillRect(cx - 6, b - 6, 12, 6);
        double a = spin * 0.4;
        int bx = cx + (int) (Math.sin(a) * 8);
        int by = b - HEIGHT + 6 - (int) (Math.cos(a) * 4) + 4;
        g.setColor(active ? new Color(0xff4d4d) : new Color(0x4dabf7));
        g.fillOval(bx - 7, by - 7, 14, 14);
        g.setColor(Color.WHITE);
        g.fillOval(bx - 4, by - 5, 5, 4);
    }
}
