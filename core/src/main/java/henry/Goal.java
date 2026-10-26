package henry;

/** Panneau de fin de niveau : il tourne quand Henry passe devant. */
final class Goal {

    final double x;
    final double base;
    boolean passed;
    int spin;

    Goal(double x, double base) {
        this.x = x;
        this.base = base;
    }

    double centerX() {
        return x + Game.T / 2.0;
    }

    void update() {
        if (passed && spin < 100) {
            spin++;
        }
    }

    void draw(Gfx g, double camX) {
        int cx = (int) (centerX() - camX);
        int b = (int) base;
        g.setColor(Col.rgb(0x868e96));
        g.fillRect(cx - 3, b - 80, 6, 80);
        double turn = Math.cos(spin * spin * 0.004);
        int half = (int) Math.max(2, Math.abs(turn) * 24);
        boolean front = turn >= 0;
        boolean done = passed && spin >= 100;
        g.setColor(Col.rgb(0x1c2f8a));
        g.fillOval(cx - half - 2, b - 110 - 2, half * 2 + 4, 52);
        g.setColor(done ? Player.BODY : Col.WHITE);
        g.fillOval(cx - half, b - 110, half * 2, 48);
        if (half > 10) {
            g.setFont(26, false);
            g.setColor(done ? Col.WHITE : Col.rgb(0xe03131));
            String s = done ? "H" : (front ? "V" : "?");
            g.drawText(s, cx - g.textWidth(s) / 2, b - 77);
        }
    }
}
