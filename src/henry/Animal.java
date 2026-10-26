package henry;

import java.awt.Color;
import java.awt.Graphics2D;

/** Petit animal libéré d'un robot détruit : il sautille puis s'envole. */
final class Animal {

    private static final Color[] COLORS = {new Color(0x4dabf7), new Color(0xf783ac), new Color(0xffffff)};

    double x;
    double y;
    double vx;
    double vy;
    int life = 220;
    private final Color color;

    Animal(double x, double y, int seed) {
        this.x = x;
        this.y = y;
        this.vx = (seed % 2 == 0) ? 2.2 : -2.2;
        this.vy = -4;
        this.color = COLORS[Math.abs(seed) % COLORS.length];
    }

    boolean update(Level level) {
        vy += 0.25;
        x += vx;
        y += vy;
        if (vy > 0) {
            byte t = level.tile(Level.toTile(x), Level.toTile(y));
            if (t != Level.EMPTY) {
                y = Level.toTile(y) * Game.T;
                vy = -4.5;
            }
        }
        return --life > 0 && y < Game.H + 40;
    }

    void draw(Graphics2D g, double camX) {
        int sx = (int) (x - camX);
        int sy = (int) y;
        int dir = vx > 0 ? 1 : -1;
        g.setColor(color);
        g.fillOval(sx - 7, sy - 12, 14, 12);
        g.fillOval(sx - 4 + dir * 5, sy - 17, 9, 9);
        g.setColor(Color.BLACK);
        g.fillOval(sx + dir * 7, sy - 15, 2, 2);
        g.setColor(new Color(0xffa94d));
        g.fillPolygon(new int[] {sx + dir * 10, sx + dir * 14, sx + dir * 10}, new int[] {sy - 14, sy - 12, sy - 10}, 3);
        g.setColor(color.darker());
        int wing = (life / 4) % 2 == 0 ? -4 : 2;
        g.fillOval(sx - 6, sy - 9 + wing, 8, 5);
    }
}
