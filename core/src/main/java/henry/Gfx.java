package henry;

/**
 * Surface de dessin indépendante de la plateforme.
 * La version PC l'implémente avec Java2D, la version Android avec Canvas.
 *
 * Conventions (identiques à Java2D) : y vers le bas, angles des arcs en degrés
 * dans le sens inverse des aiguilles d'une montre à partir de 3 heures,
 * rotations en radians, texte positionné sur sa ligne de base.
 */
public interface Gfx {

    /** Image pré-dessinée, réutilisée à chaque image (tuiles du décor). */
    interface Sprite {
    }

    /** Dessine le contenu d'un {@link Sprite} une seule fois. */
    interface Painter {
        void paint(Gfx g);
    }

    void setColor(int argb);

    void setStroke(float width);

    void fillRect(double x, double y, double w, double h);

    void fillRoundRect(double x, double y, double w, double h, double arc);

    void drawRoundRect(double x, double y, double w, double h, double arc);

    void fillOval(double x, double y, double w, double h);

    void drawOval(double x, double y, double w, double h);

    void fillArc(double x, double y, double w, double h, double start, double extent);

    void drawArc(double x, double y, double w, double h, double start, double extent);

    void drawLine(double x1, double y1, double x2, double y2);

    void fillPolygon(double[] xs, double[] ys, int n);

    void drawPolygon(double[] xs, double[] ys, int n);

    void fillVerticalGradient(double x, double y, double w, double h, int top, int bottom);

    void setFont(float size, boolean italic);

    float textWidth(String s);

    void drawText(String s, double x, double y);

    void save();

    void restore();

    void translate(double dx, double dy);

    void scale(double sx, double sy);

    void rotate(double radians);

    Sprite makeSprite(int w, int h, Painter painter);

    void drawSprite(Sprite sprite, double x, double y);
}
