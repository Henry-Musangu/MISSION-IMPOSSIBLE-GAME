package henry;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/** Panneau de fin de niveau : il tourne quand Henry passe devant. */
final class Goal {

    private static final Font LETTER = new Font(Font.SANS_SERIF, Font.BOLD, 26);

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

    void draw(Graphics2D g, double camX) {
        int cx = (int) (centerX() - camX);
        int b = (int) base;
        g.setColor(new Color(0x868e96));
        g.fillRect(cx - 3, b - 80, 6, 80);
        double turn = Math.cos(spin * spin * 0.004);
        int half = (int) Math.max(2, Math.abs(turn) * 24);
        boolean front = turn >= 0;
        g.setColor(new Color(0x1c2f8a));
        g.fillOval(cx - half - 2, b - 110 - 2, half * 2 + 4, 52);
        g.setColor(passed && spin >= 100 ? new Color(0x7b3fe4) : Color.WHITE);
        g.fillOval(cx - half, b - 110, half * 2, 48);
        if (half > 10) {
            g.setFont(LETTER);
            g.setColor(passed && spin >= 100 ? Color.WHITE : new Color(0xe03131));
            String s = passed && spin >= 100 ? "H" : (front ? "V" : "?");
            int sw = g.getFontMetrics().stringWidth(s);
            g.drawString(s, cx - sw / 2, b - 77);
        }
    }
}
