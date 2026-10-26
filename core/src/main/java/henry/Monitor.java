package henry;

/** Télévision à casser en boule : elle contient un bonus. */
final class Monitor {

    static final int RINGS = 0;
    static final int SHIELD = 1;
    static final int SHOES = 2;
    static final int STARS = 3;
    static final int LIFE = 4;

    static final int W = 28;
    static final int H = 30;

    final double x;
    final double base;
    final int type;
    boolean broken;
    /** Temps écoulé depuis la casse : l'icône monte puis le bonus est donné. */
    int popTimer;
    boolean given;

    Monitor(double x, double base, int type) {
        this.x = x;
        this.base = base;
        this.type = type;
    }

    Box bounds() {
        return new Box(x, base - H, W, H);
    }

    double centerX() {
        return x + W / 2.0;
    }

    void update() {
        if (broken) {
            popTimer++;
        }
    }

    void draw(Gfx g, double camX, int tick) {
        int sx = (int) (x - camX);
        int by = (int) base;
        if (broken) {
            g.setColor(Col.rgb(0x495057));
            g.fillRect(sx + 2, by - 8, W - 4, 8);
            g.setColor(Col.rgb(0x343a40));
            g.fillPolygon(new double[] {sx + 2, sx + 8, sx + 13, sx + 18, sx + 24, sx + W - 2},
                    new double[] {by - 8, by - 14, by - 9, by - 15, by - 10, by - 8}, 6);
            if (popTimer < 40) {
                drawIcon(g, sx + W / 2.0, by - H / 2.0 - 4 - popTimer * 1.2, tick);
            }
            return;
        }
        g.setColor(Col.rgb(0x343a40));
        g.fillRoundRect(sx, by - H, W, H, 6);
        g.setColor(Col.rgb(0xadb5bd));
        g.fillRoundRect(sx + 1, by - H + 1, W - 2, H - 6, 5);
        boolean flick = (tick / 6) % 8 == 0;
        g.setColor(flick ? Col.rgb(0xe9ecef) : Col.rgb(0x1c2f8a));
        g.fillRect(sx + 4, by - H + 4, W - 8, H - 13);
        if (!flick) {
            drawIcon(g, sx + W / 2.0, by - H / 2.0 - 3, tick);
        }
        g.setColor(Col.rgb(0x212529));
        g.fillRect(sx + 3, by - 5, W - 6, 5);
    }

    /** Petite image du bonus, centrée en (cx, cy). */
    void drawIcon(Gfx g, double cx, double cy, int tick) {
        switch (type) {
            case RINGS:
                g.setStroke(2.5f);
                g.setColor(Col.rgb(0xffd23f));
                g.drawOval(cx - 6, cy - 6, 12, 12);
                g.setStroke(1f);
                break;
            case SHIELD:
                g.setColor(Col.argb(170, 0x4dabf7));
                g.fillOval(cx - 7, cy - 7, 14, 14);
                g.setColor(Col.WHITE);
                g.fillOval(cx - 4, cy - 5, 4, 4);
                break;
            case SHOES:
                g.setColor(Col.rgb(0xe03131));
                g.fillRoundRect(cx - 7, cy - 1, 14, 6, 4);
                g.fillRect(cx - 6, cy - 6, 5, 6);
                g.setColor(Col.WHITE);
                g.fillRect(cx - 1, cy - 1, 2, 6);
                break;
            case STARS:
                g.setColor((tick / 4) % 2 == 0 ? Col.rgb(0xffe066) : Col.WHITE);
                star(g, cx, cy, 7, 3);
                break;
            default:
                g.setColor(Player.BODY);
                g.fillOval(cx - 6, cy - 6, 12, 12);
                g.setColor(Col.WHITE);
                g.fillOval(cx - 1, cy - 4, 5, 6);
                g.setColor(Col.BLACK);
                g.fillOval(cx + 1, cy - 3, 2, 3);
                break;
        }
    }

    static void star(Gfx g, double cx, double cy, double r1, double r2) {
        double[] xs = new double[10];
        double[] ys = new double[10];
        for (int i = 0; i < 10; i++) {
            double a = -Math.PI / 2 + i * Math.PI / 5;
            double r = i % 2 == 0 ? r1 : r2;
            xs[i] = cx + Math.cos(a) * r;
            ys[i] = cy + Math.sin(a) * r;
        }
        g.fillPolygon(xs, ys, 10);
    }
}
