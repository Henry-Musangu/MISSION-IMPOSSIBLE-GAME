package henry;

/** Texte qui flotte un instant (points gagnés, bonus). */
final class Popup {

    final String text;
    double x;
    double y;
    int life = 50;

    Popup(String text, double x, double y) {
        this.text = text;
        this.x = x;
        this.y = y;
    }

    boolean update() {
        y -= life > 30 ? 1.2 : 0.3;
        return --life > 0;
    }

    void draw(Gfx g, double camX) {
        g.setFont(15, true);
        double w = g.textWidth(text);
        g.setColor(Col.BLACK);
        g.drawText(text, x - camX - w / 2 + 1, y + 1);
        g.setColor(life < 12 && life % 4 < 2 ? Col.rgb(0xffd43b) : Col.WHITE);
        g.drawText(text, x - camX - w / 2, y);
    }
}
