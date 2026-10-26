package henry;

import java.util.ArrayList;
import java.util.List;

/** Carte de tuiles d'un niveau, avec les objets qui y sont posés. */
final class Level {

    static final byte EMPTY = 0;
    static final byte SOLID = 1;
    static final byte ONEWAY = 2;
    static final byte SPIKE = 3;

    final String zone;
    final int act;
    final Theme theme;
    final int w;
    final int h;
    private final byte[][] tiles;

    final List<Ring> rings = new ArrayList<>();
    final List<Enemy> enemies = new ArrayList<>();
    final List<Spring> springs = new ArrayList<>();
    final List<Checkpoint> checkpoints = new ArrayList<>();
    Goal goal;
    double startX;
    double startY;
    /** Colonne où commence l'arène du boss, ou -1 s'il n'y a pas de boss. */
    int bossArenaX = -1;

    /**
     * Légende de la grille :
     * '#' sol, '=' plate-forme (traversable par dessous), '^' pics, 'o' anneau,
     * 'E' robot qui marche, 'B' guêpe volante, 'J' ressort, 'C' point de contrôle,
     * 'F' panneau d'arrivée, 'P' départ d'Henry.
     */
    Level(String zone, int act, Theme theme, char[][] grid) {
        this.zone = zone;
        this.act = act;
        this.theme = theme;
        this.h = grid.length;
        this.w = grid[0].length;
        this.tiles = new byte[h][w];
        int t = Game.T;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                switch (grid[y][x]) {
                    case '#':
                        tiles[y][x] = SOLID;
                        break;
                    case '=':
                        tiles[y][x] = ONEWAY;
                        break;
                    case '^':
                        tiles[y][x] = SPIKE;
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

    byte tile(int tx, int ty) {
        if (tx < 0 || tx >= w) {
            return SOLID;
        }
        if (ty < 0 || ty >= h) {
            return EMPTY;
        }
        return tiles[ty][tx];
    }

    boolean isWall(int tx, int ty) {
        byte t = tile(tx, ty);
        return t == SOLID || t == SPIKE;
    }

    static int toTile(double v) {
        return (int) Math.floor(v / Game.T);
    }
}
