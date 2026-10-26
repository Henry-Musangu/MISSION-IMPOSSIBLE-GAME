package henry;

/** Plate-forme mobile : on peut monter dessus par le dessous, elle transporte Henry. */
final class Platform {

    static final int HORIZONTAL = 0;
    static final int VERTICAL = 1;

    final double baseX;
    final double baseY;
    final int w;
    final int kind;
    final double range;
    private final double speed;
    private int t;
    double x;
    double y;
    double dx;
    double dy;

    /** (x, y) : coin haut-gauche ; {@code range} : amplitude du mouvement en pixels. */
    Platform(double x, double y, int w, int kind, double range, double speed) {
        this.baseX = x;
        this.baseY = y;
        this.x = x;
        this.y = y;
        this.w = w;
        this.kind = kind;
        this.range = range;
        this.speed = speed;
    }

    void update() {
        t++;
        double ox = x;
        double oy = y;
        double s = Math.sin(t * speed) * range;
        if (kind == HORIZONTAL) {
            x = baseX + s;
        } else {
            y = baseY + s;
        }
        dx = x - ox;
        dy = y - oy;
    }

    void draw(Gfx g, Theme th, double camX) {
        double sx = x - camX;
        if (th.style == Theme.FACTORY) {
            g.setColor(Col.rgb(0x868e96));
            g.fillRect(sx, y, w, 14);
            g.setColor(th.surface);
            for (int i = 0; i < w; i += 16) {
                g.fillPolygon(new double[] {sx + i, sx + i + 8, sx + i + 16, sx + i + 8},
                        new double[] {y + 5, y, y, y + 5}, 4);
            }
            g.setColor(Col.rgb(0x343a40));
            g.fillRect(sx, y + 12, w, 3);
        } else {
            g.setColor(Col.rgb(0x8a5a2b));
            g.fillRoundRect(sx, y, w, 16, 8);
            g.setColor(Col.rgb(0x5e3a17));
            g.fillRect(sx + 4, y + 8, w - 8, 2);
            g.setColor(th.surface);
            g.fillRoundRect(sx, y, w, 6, 6);
            g.setColor(Col.brighter(th.surface));
            g.fillRect(sx + 4, y + 1, w - 8, 1);
        }
        g.setColor(Col.rgb(0x495057));
        g.fillOval(sx + 6, y + 9, 4, 4);
        g.fillOval(sx + w - 10, y + 9, 4, 4);
    }
}
