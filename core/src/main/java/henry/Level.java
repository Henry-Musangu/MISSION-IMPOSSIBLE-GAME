package henry;

import java.util.ArrayList;
import java.util.List;

/**
 * Carte de tuiles d'un niveau, avec les objets qui y sont posés.
 * Les pentes sont décrites par une hauteur de sol pour chaque colonne de pixels de la tuile.
 */
final class Level {

    static final byte EMPTY = 0;
    static final byte SOLID = 1;
    static final byte ONEWAY = 2;
    static final byte SPIKE = 3;
    /** Pente à 45° qui monte vers la droite ('/'). */
    static final byte UP45 = 4;
    /** Pente à 45° qui descend vers la droite ('\'). */
    static final byte DOWN45 = 5;
    /** Pente douce qui monte : moitié basse ('a') puis moitié haute ('b'). */
    static final byte UP22_LOW = 6;
    static final byte UP22_HIGH = 7;
    /** Pente douce qui descend : moitié haute ('c') puis moitié basse ('d'). */
    static final byte DOWN22_HIGH = 8;
    static final byte DOWN22_LOW = 9;

    final String zone;
    final int act;
    final int zoneIndex;
    final Theme theme;
    final int w;
    final int h;
    private final byte[][] tiles;

    final List<Ring> rings = new ArrayList<>();
    final List<Enemy> enemies = new ArrayList<>();
    final List<Spring> springs = new ArrayList<>();
    final List<Checkpoint> checkpoints = new ArrayList<>();
    final List<Monitor> monitors = new ArrayList<>();
    final List<Platform> platforms = new ArrayList<>();
    final List<Loop> loops = new ArrayList<>();
    Goal goal;
    double startX;
    double startY;
    /** Colonne où commence l'arène du boss, ou -1 s'il n'y a pas de boss. */
    int bossArenaX = -1;
    /** Ligne (en tuiles) du sol de l'arène du boss. */
    int bossGroundRow;
    /** Plate-forme mobile trouvée par le dernier appel à {@link #groundBelow}. */
    Platform lastPlatform;

    /**
     * Légende de la grille :
     * '#' sol, '=' plate-forme (traversable par dessous), '^' pics,
     * '/' et '\' pentes à 45°, 'a' 'b' et 'c' 'd' pentes douces,
     * 'o' anneau, 'E' robot coccinelle, 'B' guêpe, 'K' crabe qui tire,
     * '1' à '5' moniteurs (anneaux, bouclier, chaussures, invincibilité, vie),
     * 'J' ressort, 'C' borne, 'F' panneau d'arrivée, 'P' départ d'Henry.
     */
    Level(String zone, int act, int zoneIndex, Theme theme, char[][] grid) {
        this.zone = zone;
        this.act = act;
        this.zoneIndex = zoneIndex;
        this.theme = theme;
        this.h = grid.length;
        this.w = grid[0].length;
        this.tiles = new byte[h][w];
        int t = Game.T;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                char c = grid[y][x];
                switch (c) {
                    case '#':
                        tiles[y][x] = SOLID;
                        break;
                    case '=':
                        tiles[y][x] = ONEWAY;
                        break;
                    case '^':
                        tiles[y][x] = SPIKE;
                        break;
                    case '/':
                        tiles[y][x] = UP45;
                        break;
                    case '\\':
                        tiles[y][x] = DOWN45;
                        break;
                    case 'a':
                        tiles[y][x] = UP22_LOW;
                        break;
                    case 'b':
                        tiles[y][x] = UP22_HIGH;
                        break;
                    case 'c':
                        tiles[y][x] = DOWN22_HIGH;
                        break;
                    case 'd':
                        tiles[y][x] = DOWN22_LOW;
                        break;
                    case 'o':
                        rings.add(new Ring(x * t + t / 2.0, y * t + t / 2.0));
                        break;
                    case 'E':
                        enemies.add(new Enemy(Enemy.WALKER, x * t, (y + 1) * t));
                        break;
                    case 'B':
                        enemies.add(new Enemy(Enemy.FLYER, x * t, (y + 1) * t));
                        break;
                    case 'K':
                        enemies.add(new Enemy(Enemy.CRAB, x * t, (y + 1) * t));
                        break;
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                        monitors.add(new Monitor(x * t + 2, (y + 1) * t, c - '1'));
                        break;
                    case 'J':
                        springs.add(new Spring(x * t, y * t));
                        break;
                    case 'C':
                        checkpoints.add(new Checkpoint(x * t, (y + 1) * t));
                        break;
                    case 'F':
                        goal = new Goal(x * t, (y + 1) * t);
                        break;
                    case 'P':
                        startX = x * t + 5;
                        startY = (y + 1) * t - Player.H;
                        break;
                    default:
                        break;
                }
            }
        }
    }

    int pixelWidth() {
        return w * Game.T;
    }

    int pixelHeight() {
        return h * Game.T;
    }

    byte tile(int tx, int ty) {
        if (tx < 0 || tx >= w) {
            return SOLID;
        }
        if (ty < 0 || ty >= h) {
            return EMPTY;
        }
        return tiles[ty][tx];
    }

    /** Mur vertical : seules les tuiles pleines arrêtent un déplacement horizontal. */
    boolean isWall(int tx, int ty) {
        byte t = tile(tx, ty);
        return t == SOLID || t == SPIKE;
    }

    /** Tuile qui bloque par le dessous (plafond). */
    boolean isCeiling(int tx, int ty) {
        byte t = tile(tx, ty);
        return t != EMPTY && t != ONEWAY;
    }

    static boolean isSlope(byte t) {
        return t >= UP45;
    }

    /** Hauteur du sol (0 à 32 pixels) dans la tuile {@code t}, à la colonne {@code lx} (0 à 31). */
    static int height(byte t, int lx) {
        switch (t) {
            case SOLID:
            case SPIKE:
            case ONEWAY:
                return Game.T;
            case UP45:
                return lx + 1;
            case DOWN45:
                return Game.T - lx;
            case UP22_LOW:
                return lx / 2 + 1;
            case UP22_HIGH:
                return 17 + lx / 2;
            case DOWN22_HIGH:
                return Game.T - lx / 2;
            case DOWN22_LOW:
                return 16 - lx / 2;
            default:
                return 0;
        }
    }

    /** Vrai si le point (px, py) est dans une partie pleine du décor. */
    boolean solidAt(double px, double py) {
        int tx = toTile(px);
        int ty = toTile(py);
        byte t = tile(tx, ty);
        if (t == EMPTY || t == ONEWAY) {
            return false;
        }
        int lx = (int) (px - tx * Game.T);
        double surface = (ty + 1) * Game.T - height(t, Math.max(0, Math.min(Game.T - 1, lx)));
        return py >= surface;
    }

    /**
     * Cherche le sol sous le point (px, footY) : renvoie la hauteur (y) de la surface la plus haute
     * comprise entre {@code footY - maxUp} et {@code footY + maxDown}, ou NaN s'il n'y en a pas.
     * Les plates-formes (fixes ou mobiles) ne comptent que si on arrive par le dessus.
     */
    double groundBelow(double px, double footY, double prevFoot, double maxUp, double maxDown) {
        lastPlatform = null;
        int tx = toTile(px);
        int lx = Math.max(0, Math.min(Game.T - 1, (int) Math.floor(px - tx * Game.T)));
        double best = Double.NaN;
        int r0 = toTile(footY - maxUp);
        int r1 = toTile(footY + maxDown);
        for (int r = r0; r <= r1; r++) {
            byte t = tile(tx, r);
            int hgt = height(t, lx);
            if (hgt == 0) {
                continue;
            }
            byte above = tile(tx, r - 1);
            if (above != EMPTY && above != ONEWAY && height(above, lx) > 0) {
                continue; // enfoui sous une autre tuile
            }
            double sy = (r + 1) * Game.T - hgt;
            if (sy < footY - maxUp || sy > footY + maxDown) {
                continue;
            }
            if (t == ONEWAY && sy < prevFoot - 4) {
                continue;
            }
            best = sy;
            break;
        }
        for (Platform p : platforms) {
            if (px >= p.x && px <= p.x + p.w) {
                double sy = p.y;
                if (sy >= footY - maxUp && sy <= footY + maxDown && sy >= prevFoot - 4 - Math.abs(p.dy)
                        && (Double.isNaN(best) || sy < best)) {
                    best = sy;
                    lastPlatform = p;
                }
            }
        }
        return best;
    }

    /** Angle théorique de la pente sous le point (radians, positif quand ça monte vers la droite). */
    double slopeAngle(double px, double surfaceY) {
        byte t = tile(toTile(px), toTile(surfaceY + 0.5));
        switch (t) {
            case UP45:
                return Math.PI / 4;
            case DOWN45:
                return -Math.PI / 4;
            case UP22_LOW:
            case UP22_HIGH:
                return Math.atan(0.5);
            case DOWN22_HIGH:
            case DOWN22_LOW:
                return -Math.atan(0.5);
            default:
                return 0;
        }
    }

    boolean spikesAt(double px, double surfaceY) {
        return tile(toTile(px), toTile(surfaceY + 1)) == SPIKE;
    }

    static int toTile(double v) {
        return (int) Math.floor(v / Game.T);
    }
}
