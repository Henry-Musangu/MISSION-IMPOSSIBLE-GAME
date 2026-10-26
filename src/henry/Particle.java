package henry;

import java.awt.Color;
import java.awt.Graphics2D;

/** Effets visuels : étincelles d'anneaux, explosions et débris. */
final class Particle {

    static final int SPARK = 0;
    static final int BOOM = 1;
    static final int DEBRIS = 2;

    final int kind;
    double x;
    double y;
    double vx;
    double vy;
    int life;
    final int maxLife;
    final Color color;

    Particle(int kind, double x, double y, double vx, double vy, int life, Color color) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.life = life;
        this.maxLife = life;
        this.color = color;
    }

    boolean update() {
        x += vx;
        y += vy;
        if (kind == DEBRIS) {
            vy += 0.3;
        }
        return --life > 0;
    }

    void draw(Graphics2D g, double camX) {
        double k = life / (double) maxLife;
        int sx = (int) (x - camX);
        int sy = (int) y;
        if (kind == SPARK) {
            int r = (int) (2 + 5 * k);
            g.setColor(color);
            g.drawLine(sx - r, sy, sx + r, sy);
            g.drawLine(sx, sy - r, sx, sy + r);
            g.fillRect(sx - 1, sy - 1, 3, 3);
        } else if (kind == BOOM) {
            int r = (int) (6 + 18 * (1 - k));
            int alpha = (int) (255 * k);
            g.setColor(new Color(255, 230, 120, alpha));
            g.fillOval(sx - r, sy - r, r * 2, r * 2);
            g.setColor(new Color(255, 120, 40, alpha));
            g.fillOval(sx - r / 2, sy - r / 2, r, r);
        } else {
            g.setColor(color);
            g.fillRect(sx - 2, sy - 2, 5, 5);
        }
    }
}
