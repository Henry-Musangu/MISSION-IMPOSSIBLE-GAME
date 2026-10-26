package henry;

/**
 * Le Dr. Vortex dans sa capsule volante, à la fin de chaque zone.
 * Il faut le toucher en boule ; ses attaques changent selon la zone :
 * bombes (prairie), bombes et boules de feu visées (canyon), bombes et piqués (usine).
 */
final class Boss {

    static final int W = 96;
    static final int H = 60;

    final int pattern;
    final int maxHits;
    double x;
    double y;
    private final double baseY;
    private final double groundY;
    private final double left;
    private final double right;
    private int dir = -1;
    private int t;
    private int bombTimer;
    private int fireTimer;
    private int diveTimer;
    private boolean entering = true;
    int hits;
    int flash;
    boolean defeated;
    int deadTimer;

    Boss(double arenaLeft, double groundY, int pattern) {
        this.pattern = pattern;
        this.maxHits = pattern == 0 ? 6 : 8;
        this.groundY = groundY;
        baseY = groundY - 156;
        left = arenaLeft + 8;
        right = arenaLeft + Game.W - 8 - W;
        x = right;
        y = groundY - 480;
    }

    void update(Game game) {
        t++;
        if (flash > 0) {
            flash--;
        }
        if (defeated) {
            deadTimer++;
            if (deadTimer < 110) {
                if (deadTimer % 7 == 0) {
                    game.explosion(x + Math.random() * W, y + Math.random() * H, 1);
                    Sound.play(Sound.BOOM);
                    game.shake(6);
                }
            } else {
                x += 3.5;
                y -= 2;
            }
            return;
        }
        if (entering) {
            y += 2.5;
            if (y >= baseY) {
                y = baseY;
                entering = false;
            }
            return;
        }
        double speed = 1.6 + hits * 0.22;
        x += dir * speed;
        if (x < left) {
            x = left;
            dir = 1;
        } else if (x > right) {
            x = right;
            dir = -1;
        }

        // piqué vers le sol (usine)
        double dive = 0;
        if (pattern == 2) {
            diveTimer++;
            int cycle = 330 - hits * 15;
            int phase = diveTimer % cycle;
            if (phase > cycle - 110) {
                double k = (phase - (cycle - 110)) / 110.0;
                dive = Math.sin(k * Math.PI) * (groundY - baseY - H - 30);
            }
        }
        y = baseY + Math.sin(t * 0.04) * 22 + dive;

        bombTimer++;
        int bombEvery = Math.max(45, (pattern == 0 ? 120 : 110) - hits * 8);
        if (bombTimer >= bombEvery && dive < 5) {
            bombTimer = 0;
            game.dropBomb(x + W / 2.0, y + H);
        }
        if (pattern == 1) {
            fireTimer++;
            if (fireTimer >= Math.max(90, 170 - hits * 8)) {
                fireTimer = 0;
                double px = game.player.centerX();
                double py = game.player.y + Player.H / 2.0;
                double sx = x + W / 2.0;
                double sy = y + H - 10;
                double d = Math.max(1, Math.hypot(px - sx, py - sy));
                game.shoot(Shot.FIRE, sx, sy, (px - sx) / d * 3.2, (py - sy) / d * 3.2);
            }
        }
    }

    boolean dangerous() {
        return !defeated && !entering;
    }

    Box bounds() {
        return new Box(x + 8, y + 4, W - 16, H - 8);
    }

    double centerX() {
        return x + W / 2.0;
    }

    void draw(Gfx g, double camX, int tick) {
        int sx = (int) (x - camX);
        int sy = (int) y;
        boolean white = flash > 0 && (flash / 3) % 2 == 0;
        // flamme du réacteur
        if (!defeated || deadTimer > 110) {
            int f = 8 + (tick % 4) * 3;
            g.setColor(Col.rgb(0xff922b));
            g.fillPolygon(new double[] {sx + W / 2 - 10, sx + W / 2, sx + W / 2 + 10},
                    new double[] {sy + H - 4, sy + H + f, sy + H - 4}, 3);
            g.setColor(Col.rgb(0xffe066));
            g.fillPolygon(new double[] {sx + W / 2 - 5, sx + W / 2, sx + W / 2 + 5},
                    new double[] {sy + H - 4, sy + H + f / 2, sy + H - 4}, 3);
        }
        // pilote
        g.setColor(white ? Col.WHITE : Col.rgb(0xffc9a8));
        g.fillOval(sx + 30, sy - 6, 36, 34);
        g.setColor(Col.BLACK);
        if (defeated) {
            g.setStroke(2f);
            g.drawLine(sx + 37, sy + 5, sx + 43, sy + 11);
            g.drawLine(sx + 43, sy + 5, sx + 37, sy + 11);
            g.drawLine(sx + 53, sy + 5, sx + 59, sy + 11);
            g.drawLine(sx + 59, sy + 5, sx + 53, sy + 11);
            g.setStroke(1f);
        } else {
            g.fillOval(sx + 36, sy + 4, 8, 8);
            g.fillOval(sx + 52, sy + 4, 8, 8);
        }
        g.setColor(white ? Col.WHITE : Col.rgb(0x5c3d2e));
        g.fillArc(sx + 30, sy + 14, 18, 12, 0, 180);
        g.fillArc(sx + 48, sy + 14, 18, 12, 0, 180);
        g.setColor(white ? Col.WHITE : Col.rgb(0x7048e8));
        g.fillArc(sx + 28, sy - 12, 40, 22, 0, 180);
        // dôme de verre
        g.setColor(Col.rgba(150, 220, 255, 110));
        g.fillOval(sx + 22, sy - 16, 52, 48);
        g.setColor(Col.argb(160, 0xffffff));
        g.fillOval(sx + 30, sy - 10, 10, 6);
        // coque
        int hull = pattern == 1 ? 0xb5651d : (pattern == 2 ? 0x495057 : 0x868e96);
        g.setColor(white ? Col.WHITE : Col.rgb(hull));
        g.fillRoundRect(sx, sy + 20, W, H - 20, 30);
        g.setColor(white ? Col.WHITE : Col.rgb(0x343a40));
        g.fillRect(sx + 6, sy + 34, W - 12, 6);
        g.setColor((tick / 10) % 2 == 0 ? Col.rgb(0xff4d4d) : Col.rgb(0xffd43b));
        g.fillOval(sx + 10, sy + 26, 8, 8);
        g.fillOval(sx + W - 18, sy + 26, 8, 8);
        if (pattern == 1) {
            g.setColor(Col.rgb(0x343a40));
            g.fillRect(sx + W / 2 - 5, sy + H - 6, 10, 10);
        }
        // jauge de vie
        if (!defeated && !entering) {
            for (int i = 0; i < maxHits; i++) {
                g.setColor(i < maxHits - hits ? Col.rgb(0xff6b6b) : Col.argb(120, 0x343a40));
                g.fillRect(sx + 8 + i * ((W - 16) / maxHits), sy - 24, (W - 16) / maxHits - 2, 5);
            }
        }
    }
}
