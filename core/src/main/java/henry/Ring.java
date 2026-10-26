package henry;

/** Anneau doré. Les anneaux perdus rebondissent puis disparaissent. */
final class Ring {

    static final int SIZE = 16;
    private static final int GOLD = Col.rgb(0xffd23f);
    private static final int GOLD_DARK = Col.rgb(0xc98a00);

    double x;
    double y;
    double vx;
    double vy;
    boolean collected;
    final boolean lost;
    int life;
    int delay;

    Ring(double x, double y) {
        this.x = x;
        this.y = y;
        this.lost = false;
    }

    Ring(double x, double y, double vx, double vy) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.lost = true;
        this.life = 256;
        this.delay = 40;
    }

    /** Physique des anneaux perdus : gravité et rebonds sur le décor. */
    void updateLost(Level level) {
        life--;
        if (delay > 0) {
            delay--;
        }
        vy += 0.18;
        x += vx;
        int side = Level.toTile(x + Math.signum(vx) * SIZE / 2.0);
        if (level.isWall(side, Level.toTile(y))) {
            vx = -vx * 0.75;
            x += vx;
        }
        y += vy;
        if (vy > 0) {
            int row = Level.toTile(y + SIZE / 2.0);
            byte t = level.tile(Level.toTile(x), row);
            if (t == Level.SOLID || t == Level.SPIKE || t == Level.ONEWAY) {
                y = row * Game.T - SIZE / 2.0;
                vy = -vy * 0.75;
                if (vy > -1.5) {
                    vy = -1.5;
                }
            }
        } else if (level.isWall(Level.toTile(x), Level.toTile(y - SIZE / 2.0))) {
            vy = -vy;
        }
    }

    boolean collectable() {
        return !collected && delay == 0;
    }

    Box bounds() {
        return new Box(x - SIZE / 2.0, y - SIZE / 2.0, SIZE, SIZE);
    }

    void draw(Gfx g, double camX, int tick) {
        if (lost && life < 60 && (life / 3) % 2 == 0) {
            return;
        }
        double turn = Math.abs(Math.cos(tick * 0.08 + x * 0.01));
        double w = Math.max(3, SIZE * turn);
        double sx = x - camX;
        g.setStroke(3.5f);
        g.setColor(GOLD_DARK);
        g.drawOval(sx - w / 2 + 1, y - SIZE / 2.0 + 1, w, SIZE);
        g.setColor(GOLD);
        g.drawOval(sx - w / 2, y - SIZE / 2.0, w, SIZE);
        g.setStroke(1f);
        g.setColor(Col.WHITE);
        g.fillOval((int) (sx - w / 4), (int) (y - SIZE / 2.0), 3, 3);
    }
}
