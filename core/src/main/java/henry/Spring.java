package henry;

/** Ressort jaune qui propulse Henry dans les airs. */
final class Spring {

    static final double POWER = -14.5;

    final double x;
    final double y;
    int squash;

    Spring(double x, double y) {
        this.x = x;
        this.y = y;
    }

    double top() {
        return y + 14;
    }

    Box bounds() {
        return new Box(x + 2, top(), Game.T - 4, Game.T - 14);
    }

    void update() {
        if (squash > 0) {
            squash--;
        }
    }

    void draw(Gfx g, double camX) {
        int sx = (int) (x - camX);
        int base = (int) y + Game.T;
        int plate = squash > 0 ? base - 10 : base - 20;
        g.setColor(Col.rgb(0x9aa3b5));
        for (int yy = plate + 4; yy < base - 4; yy += 4) {
            g.fillRect(sx + 10, yy, 12, 2);
        }
        g.setColor(Col.rgb(0xe03131));
        g.fillRect(sx + 2, base - 5, Game.T - 4, 5);
        g.setColor(Col.rgb(0xffd23f));
        g.fillRoundRect(sx + 1, plate, Game.T - 2, 6, 4);
        g.setColor(Col.WHITE);
        g.fillRect(sx + 4, plate + 1, Game.T - 10, 1);
    }
}
