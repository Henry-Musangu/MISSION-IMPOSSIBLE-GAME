package henry;

/** Rectangle de collision (remplace java.awt.geom, absent sur Android). */
final class Box {

    final double x;
    final double y;
    final double w;
    final double h;

    Box(double x, double y, double w, double h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    boolean intersects(Box o) {
        return x < o.x + o.w && o.x < x + w && y < o.y + o.h && o.y < y + h;
    }
}
