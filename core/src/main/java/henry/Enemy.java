package henry;

/** Les robots du Dr. Vortex : la coccinelle qui roule, la guêpe qui vole et le crabe qui tire. */
final class Enemy {

    static final int WALKER = 0;
    static final int FLYER = 1;
    static final int CRAB = 2;

    final int type;
    final int w;
    final int h;
    double x;
    double y;
    private final double homeX;
    private final double homeY;
    private int dir = -1;
    private int t;
    boolean alive = true;

    /** {@code baseX} : bord gauche de la tuile ; {@code baseY} : bas de la tuile. */
    Enemy(int type, double baseX, double baseY) {
        this.type = type;
        if (type == WALKER) {
            w = 30;
            h = 26;
        } else if (type == CRAB) {
            w = 36;
            h = 24;
        } else {
            w = 30;
            h = 22;
        }
        x = baseX + 1;
        y = baseY - h;
        homeX = x;
        homeY = y;
    }

    void update(Level level, Game game) {
        t++;
        if (type == WALKER || type == CRAB) {
            boolean firing = type == CRAB && t % 160 > 130;
            if (type == CRAB && t % 160 == 145) {
                game.shoot(Shot.PELLET, x + 4, y + 4, -2.2, -4.5);
                game.shoot(Shot.PELLET, x + w - 4, y + 4, 2.2, -4.5);
            }
            if (!firing) {
                walk(level, type == CRAB ? 0.6 : 1.0);
            }
        } else {
            double nx = homeX + Math.sin(t * 0.02) * 90;
            dir = nx > x ? 1 : -1;
            x = nx;
            y = homeY + Math.sin(t * 0.09) * 14;
        }
    }

    /** Avance en suivant le sol (pentes comprises) et fait demi-tour au bord du vide ou contre un mur. */
    private void walk(Level level, double speed) {
        double nx = x + dir * speed;
        double front = dir > 0 ? nx + w : nx;
        double foot = y + h;
        double ahead = level.groundBelow(front, foot, foot, 14, 18);
        boolean wall = level.isWall(Level.toTile(front), Level.toTile(foot - 18));
        if (wall || Double.isNaN(ahead)) {
            dir = -dir;
            return;
        }
        x = nx;
        double ground = level.groundBelow(x + w / 2.0, foot, foot, 14, 18);
        if (!Double.isNaN(ground)) {
            y = ground - h;
        }
    }

    Box bounds() {
        return new Box(x + 2, y + 2, w - 4, h - 2);
    }

    double centerX() {
        return x + w / 2.0;
    }

    void draw(Gfx g, double camX) {
        g.save();
        g.translate(x - camX + w / 2.0, y + h);
        if (dir > 0) {
            g.scale(-1, 1);
        }
        if (type == WALKER) {
            drawWalker(g);
        } else if (type == CRAB) {
            drawCrab(g);
        } else {
            drawFlyer(g);
        }
        g.restore();
    }

    /** Dessin centré en bas, tourné vers la gauche. */
    private void drawWalker(Gfx g) {
        g.setColor(Col.rgb(0x333333));
        g.fillOval(-13, -10, 10, 10);
        g.fillOval(3, -10, 10, 10);
        g.setColor(Col.rgb(0xaaaaaa));
        int spin = (t / 4) % 2;
        g.fillOval(-10 + spin, -7, 4, 4);
        g.fillOval(6 + spin, -7, 4, 4);
        g.setColor(Col.rgb(0xe03131));
        g.fillArc(-15, -28, 30, 30, 0, 180);
        g.setColor(Col.BLACK);
        g.fillRect(-15, -14, 30, 3);
        g.fillOval(-8, -24, 5, 5);
        g.fillOval(4, -22, 5, 5);
        g.fillOval(-2, -27, 4, 4);
        g.setColor(Col.rgb(0x222222));
        g.fillOval(-20, -20, 10, 9);
        g.setColor(Col.WHITE);
        g.fillOval(-19, -19, 5, 5);
        g.setColor(Col.RED);
        g.fillOval(-18, -18, 3, 3);
    }

    private void drawCrab(Gfx g) {
        boolean firing = t % 160 > 130;
        int legs = (t / 6) % 2 == 0 ? 2 : -2;
        g.setColor(Col.rgb(0xc92a2a));
        g.setStroke(3f);
        for (int i = -1; i <= 1; i += 2) {
            g.drawLine(i * 8, -8, i * 15, -1 + legs * i);
            g.drawLine(i * 4, -8, i * 10, 0 - legs * i);
        }
        g.setStroke(1f);
        g.setColor(Col.rgb(0xff6b6b));
        g.fillOval(-15, -22, 30, 18);
        g.setColor(Col.rgb(0xffa8a8));
        g.fillOval(-9, -20, 12, 6);
        // pinces
        double lift = firing ? -10 : 0;
        g.setColor(Col.rgb(0xe03131));
        g.fillOval(-26, -26 + lift, 12, 12);
        g.fillOval(14, -26 + lift, 12, 12);
        g.setColor(Col.rgb(0xffe3e3));
        g.fillRect(-22, -22 + lift, 4, 2);
        g.fillRect(18, -22 + lift, 4, 2);
        // yeux sur tiges
        g.setColor(Col.rgb(0x495057));
        g.fillRect(-6, -28, 2, 8);
        g.fillRect(4, -28, 2, 8);
        g.setColor(Col.WHITE);
        g.fillOval(-9, -33, 7, 7);
        g.fillOval(1, -33, 7, 7);
        g.setColor(Col.BLACK);
        g.fillOval(-8, -31, 3, 3);
        g.fillOval(2, -31, 3, 3);
    }

    private void drawFlyer(Gfx g) {
        g.setColor(Col.argb(170, 0xffffff));
        int flap = (t / 2) % 2 == 0 ? -10 : -5;
        g.fillOval(-6, -22 + flap / 2, 14, 10 - flap / 3);
        g.fillOval(2, -24 + flap / 2, 12, 9 - flap / 3);
        g.setColor(Col.rgb(0xffc300));
        g.fillOval(-12, -16, 26, 14);
        g.setColor(Col.BLACK);
        g.fillRect(-2, -16, 4, 14);
        g.fillRect(6, -15, 3, 12);
        g.fillPolygon(new double[] {13, 20, 13}, new double[] {-12, -9, -6}, 3);
        g.setColor(Col.rgb(0x444444));
        g.fillOval(-17, -17, 11, 11);
        g.setColor(Col.rgb(0x7fe0ff));
        g.fillOval(-15, -14, 5, 5);
    }
}
