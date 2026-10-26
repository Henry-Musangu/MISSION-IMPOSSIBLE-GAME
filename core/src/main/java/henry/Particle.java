package henry;

/** Effets visuels : étincelles d'anneaux, explosions et débris. */
final class Particle {

    static final int SPARK = 0;
    static final int BOOM = 1;
    static final int DEBRIS = 2;
    static final int DUST = 3;

    final int kind;
    double x;
    double y;
    double vx;
    double vy;
    int life;
    final int maxLife;
    final int color;

    Particle(int kind, double x, double y, double vx, double vy, int life, int color) {
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
        } else if (kind == DUST) {
            vx *= 0.92;
            vy *= 0.92;
        }
        return --life > 0;
    }

    void draw(Gfx g, double camX) {
        double k = life / (double) maxLife;
        int sx = (int) (x - camX);
        int sy = (int) y;
        if (kind == SPARK) {
            int r = (int) (2 + 5 * k);
            g.setColor(color);
            g.setStroke(1f);
            g.drawLine(sx - r, sy, sx + r, sy);
            g.drawLine(sx, sy - r, sx, sy + r);
            g.fillRect(sx - 1, sy - 1, 3, 3);
        } else if (kind == BOOM) {
            int r = (int) (6 + 18 * (1 - k));
            int alpha = (int) (255 * k);
            g.setColor(Col.argb(alpha, 0xffe678));
            g.fillOval(sx - r, sy - r, r * 2, r * 2);
            g.setColor(Col.argb(alpha, 0xff7828));
            g.fillOval(sx - r / 2, sy - r / 2, r, r);
        } else if (kind == DUST) {
            double r = 3 + 7 * (1 - k);
            g.setColor(Col.argb((int) (150 * k), 0xf1f3f5));
            g.fillOval(sx - r, sy - r, 2 * r, 2 * r);
        } else {
            g.setColor(color);
            g.fillRect(sx - 2, sy - 2, 5, 5);
        }
    }
}
