package henry;

/**
 * Looping : si Henry arrive assez vite par la gauche, il fait le tour complet.
 * Le cercle est posé sur un sol plat, son point bas touche le sol en (cx, groundY).
 */
final class Loop {

    static final double R = 104;
    /** Vitesse minimale pour entrer dans le looping. */
    static final double MIN_SPEED = 5.0;

    final double cx;
    final double groundY;

    Loop(double cx, double groundY) {
        this.cx = cx;
        this.groundY = groundY;
    }

    double cy() {
        return groundY - R;
    }

    /** Point de la piste à l'angle {@code a} (0 = en bas, sens inverse des aiguilles d'une montre à l'écran). */
    double px(double a) {
        return cx + R * Math.sin(a);
    }

    double py(double a) {
        return cy() + R * Math.cos(a);
    }

    /** Bord avant de la piste (dessiné devant Henry) : donne l'épaisseur du looping en relief. */
    void drawRim(Gfx g, Theme th, double camX) {
        double sx = cx - camX;
        double cy = cy();
        double r = R + 8;
        g.setStroke(6f);
        g.setColor(Col.withAlpha(th.dirt2, 230));
        g.drawOval(sx - r, cy - r, 2 * r, 2 * r);
        g.setStroke(2.5f);
        g.setColor(th.surface);
        g.drawOval(sx - R - 4, cy - R - 4, 2 * R + 8, 2 * R + 8);
        g.setStroke(1f);
    }

    void draw(Gfx g, Theme th, double camX) {
        double sx = cx - camX;
        double cy = cy();
        double band = 26;
        double r = R + band / 2;
        // piste à damier
        g.setStroke((float) band);
        g.setColor(th.dirt2);
        g.drawOval(sx - r, cy - r, 2 * r, 2 * r);
        g.setStroke((float) (band - 8));
        g.setColor(th.dirt1);
        g.drawOval(sx - r, cy - r, 2 * r, 2 * r);
        // bordure de la surface roulante
        g.setStroke(5f);
        g.setColor(th.surface);
        g.drawOval(sx - R - 2, cy - R - 2, 2 * R + 4, 2 * R + 4);
        g.setStroke(1.5f);
        g.setColor(th.surfaceEdge);
        g.drawOval(sx - R - 5, cy - R - 5, 2 * R + 10, 2 * R + 10);
        g.setStroke(1f);
    }
}
