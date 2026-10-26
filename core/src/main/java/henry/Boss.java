package henry;

/** Le Dr. Vortex dans sa capsule volante. Il faut le toucher 8 fois en boule. */
final class Boss {

    static final int W = 96;
    static final int H = 60;
    static final int MAX_HITS = 8;

    double x;
    double y;
    private final double baseY = 196;
    private final double left;
    private final double right;
    private int dir = -1;
    private int t;
    private int bombTimer;
    private boolean entering = true;
    int hits;
    int flash;
    boolean defeated;
    int deadTimer;

    Boss(double arenaLeft) {
        left = arenaLeft + 8;
        right = arenaLeft + Game.W - 8 - W;
        x = right;
        y = -H - 20;
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
                }
            } else {
                x += 3.5;
                y -= 2;
            }
            return;
        }
        if (entering) {
            y += 2;
            if (y >= baseY) {
                y = baseY;
                entering = false;
            }
            return;
        }
        double speed = 1.6 + hits * 0.25;
        x += dir * speed;
        if (x < left) {
            x = left;
            dir = 1;
        } else if (x > right) {
            x = right;
            dir = -1;
        }
        y = baseY + Math.sin(t * 0.04) * 22;
        bombTimer++;
        if (bombTimer >= Math.max(45, 110 - hits * 8)) {
            bombTimer = 0;
            game.dropBomb(x + W / 2.0, y + H);
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
        g.fillOval(sx + 36, sy + 4, 8, 8);
        g.fillOval(sx + 52, sy + 4, 8, 8);
        g.setColor(white ? Col.WHITE : Col.rgb(0x5c3d2e));
        g.fillArc(sx + 30, sy + 14, 18, 12, 0, 180);
        g.fillArc(sx + 48, sy + 14, 18, 12, 0, 180);
        g.setColor(white ? Col.WHITE : Col.rgb(0x7048e8));
        g.fillArc(sx + 28, sy - 12, 40, 22, 0, 180);
        // dôme de verre
        g.setColor(Col.rgba(150, 220, 255, 110));
        g.fillOval(sx + 22, sy - 16, 52, 48);
        // coque
        g.setColor(white ? Col.WHITE : Col.rgb(0x868e96));
        g.fillRoundRect(sx, sy + 20, W, H - 20, 30);
        g.setColor(white ? Col.WHITE : Col.rgb(0x495057));
        g.fillRect(sx + 6, sy + 34, W - 12, 6);
        g.setColor((tick / 10) % 2 == 0 ? Col.rgb(0xff4d4d) : Col.rgb(0xffd43b));
        g.fillOval(sx + 10, sy + 26, 8, 8);
        g.fillOval(sx + W - 18, sy + 26, 8, 8);
    }
}
