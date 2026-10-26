package henry;

/** Projectile ennemi : boulette du crabe ou boule de feu du Dr. Vortex. */
final class Shot {

    static final int PELLET = 0;
    static final int FIRE = 1;

    final int kind;
    double x;
    double y;
    double vx;
    double vy;
    int life = 300;

    Shot(int kind, double x, double y, double vx, double vy) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
    }

    /** @return false quand le projectile disparaît. */
    boolean update(Level level) {
        if (kind == PELLET) {
            vy += 0.18;
        }
        x += vx;
        y += vy;
        return --life > 0 && !level.solidAt(x, y) && y < level.pixelHeight() + 40;
    }

    Box bounds() {
        return new Box(x - 6, y - 6, 12, 12);
    }

    void draw(Gfx g, double camX, int tick) {
        double sx = x - camX;
        if (kind == FIRE) {
            g.setColor(Col.argb(160, 0xff922b));
            g.fillOval(sx - 10, y - 10, 20, 20);
            g.setColor(Col.rgb(0xffe066));
            g.fillOval(sx - 6, y - 6, 12, 12);
            g.setColor(Col.WHITE);
            g.fillOval(sx - 3, y - 3, 6, 6);
        } else {
            g.setColor((tick / 3) % 2 == 0 ? Col.rgb(0xff6b6b) : Col.rgb(0xffa94d));
            g.fillOval(sx - 6, y - 6, 12, 12);
            g.setColor(Col.WHITE);
            g.fillOval(sx - 3, y - 4, 4, 4);
        }
    }
}
