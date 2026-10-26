package henry;

/** Bombe lâchée par le Dr. Vortex. Elle explose en touchant le sol. */
final class Bomb {

    double x;
    double y;
    private double vy = 1;
    int blast;

    Bomb(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /** @return false quand la bombe a fini d'exploser. */
    boolean update(Game game, Level level) {
        if (blast > 0) {
            return --blast > 0;
        }
        vy = Math.min(vy + 0.25, 9);
        y += vy;
        if (level.tile(Level.toTile(x), Level.toTile(y + 8)) != Level.EMPTY || y > level.pixelHeight()) {
            blast = 26;
            game.explosion(x, y, 4);
            Sound.play(Sound.BOOM);
        }
        return true;
    }

    Box bounds() {
        if (blast > 0) {
            return new Box(x - 22, y - 30, 44, 38);
        }
        return new Box(x - 8, y - 8, 16, 16);
    }

    void draw(Gfx g, double camX, int tick) {
        if (blast > 0) {
            return;
        }
        int sx = (int) (x - camX);
        int sy = (int) y;
        g.setColor(Col.rgb(0x222222));
        g.fillOval(sx - 9, sy - 9, 18, 18);
        g.setColor(Col.rgb(0x666666));
        g.fillOval(sx - 5, sy - 6, 6, 5);
        g.setColor((tick / 4) % 2 == 0 ? Col.RED : Col.YELLOW);
        g.fillOval(sx - 2, sy - 14, 5, 5);
    }
}
