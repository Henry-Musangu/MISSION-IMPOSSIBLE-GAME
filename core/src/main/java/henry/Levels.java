package henry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Les six actes du jeu (trois zones de deux actes). Chaque acte est assemblé
 * de gauche à droite avec des « segments » : sol plat, collines, loopings, gouffres…
 * Le dernier acte de chaque zone se termine par un combat contre le Dr. Vortex.
 */
final class Levels {

    static final int COUNT = 6;
    static final int ROWS = 20;
    private static final int GROUND = 14;

    private Levels() {
    }

    static String zoneName(int index) {
        switch (index / 2) {
            case 0:
                return "ZONE PRAIRIE ÉMERAUDE";
            case 1:
                return "ZONE CANYON CRÉPUSCULE";
            default:
                return "USINE DU DR. VORTEX";
        }
    }

    static Level create(int index) {
        switch (index) {
            case 0:
                return prairie1();
            case 1:
                return prairie2();
            case 2:
                return canyon1();
            case 3:
                return canyon2();
            case 4:
                return factory1();
            default:
                return factory2();
        }
    }

    // ================================================================ zone 1

    private static Level prairie1() {
        Builder b = new Builder(GROUND);
        int c = b.flat(14);
        b.put(c + 3, 'P').ringsOver(c + 7, c + 12, 2);
        c = b.up22(2);
        b.flat(6);
        b.put(c + 6, 'E');
        b.down22(2);
        c = b.flat(12);
        b.put(c + 4, '1').put(c + 7, '2').ringsOver(c + 3, c + 9, 3);
        b.loop();
        c = b.flat(8);
        b.put(c + 5, 'E');
        b.up45(3);
        c = b.flat(6);
        b.ringsOver(c, c + 5, 2).put(c + 3, 'B', -3);
        b.down45(3);
        c = b.flat(22);
        // route du haut : ressort puis plates-formes avec des anneaux et des chaussures
        b.put(c + 2, 'J');
        b.plat(c + 4, c + 18, b.gr - 6).rings(c + 5, c + 17, b.gr - 8).at(c + 12, b.gr - 7, '3');
        b.put(c + 8, 'E').put(c + 14, 'E').ringsOver(c + 7, c + 15, 2);
        c = b.flat(6);
        b.put(c + 3, 'C');
        c = b.pit(5);
        b.arc(c - 1, c + 5, b.gr - 3);
        c = b.flat(10);
        b.put(c + 6, 'K');
        b.up22(3);
        b.flat(2);
        c = b.pit(6);
        b.arc(c, c + 5, b.gr - 4);
        b.gr = GROUND;
        c = b.flat(12);
        b.put(c + 4, 'E').put(c + 8, 'B', -4).put(c + 10, '4');
        b.loop();
        c = b.flat(10);
        b.ringsOver(c, c + 6, 2);
        b.up45(2);
        c = b.flat(4);
        b.down22(2);
        c = b.flat(22);
        b.put(c + 4, 'E').put(c + 9, 'K').ringsOver(c + 12, c + 18, 2);
        b.put(c + 18, 'F');
        b.flat(14);
        return b.build("ZONE PRAIRIE ÉMERAUDE", 1, 0, Theme.PRAIRIE);
    }

    private static Level prairie2() {
        Builder b = new Builder(GROUND);
        int c = b.flat(12);
        b.put(c + 3, 'P').ringsOver(c + 6, c + 11, 2);
        b.down22(1);
        c = b.flat(6);
        b.put(c + 3, 'E');
        b.up22(1);
        c = b.flat(8);
        b.put(c + 3, '1').put(c + 6, 'E');
        c = b.pit(8);
        b.movingH(c + 1, b.gr, 3, 2.5, 0.02).rings(c + 2, c + 6, b.gr - 3);
        c = b.flat(10);
        b.put(c + 4, 'K').put(c + 8, '3');
        b.up45(4);
        c = b.flat(8);
        b.put(c + 2, 'B', -3).put(c + 6, 'B', -3).ringsOver(c, c + 7, 2);
        b.down45(4);
        c = b.flat(4);
        b.loop();
        c = b.flat(6);
        b.put(c + 3, 'C');
        c = b.pit(7);
        b.movingV(c + 2, b.gr - 1, 3, 70, 0.025).rings(c + 2, c + 4, b.gr - 6);
        c = b.flat(14);
        b.spikes(c + 4, c + 5, b.gr - 1).spikes(c + 9, c + 10, b.gr - 1);
        b.ringsOver(c + 3, c + 11, 4).put(c + 12, 'E');
        b.up22(2);
        c = b.flat(4);
        b.put(c + 2, '2');
        b.down22(2);
        c = b.flat(10);
        b.put(c + 3, 'K').put(c + 7, 'B', -3);
        c = b.flat(4);
        b.put(c + 2, 'C');
        b.arena();
        return b.build("ZONE PRAIRIE ÉMERAUDE", 2, 0, Theme.PRAIRIE);
    }

    // ================================================================ zone 2

    private static Level canyon1() {
        Builder b = new Builder(GROUND);
        int c = b.flat(12);
        b.put(c + 3, 'P').ringsOver(c + 6, c + 11, 2);
        b.up45(2);
        c = b.flat(8);
        b.put(c + 4, 'E').put(c + 6, '1');
        b.gr += 2;
        c = b.flat(10);
        b.put(c + 5, 'K');
        b.up22(2);
        b.flat(2);
        c = b.pit(6);
        b.arc(c, c + 6, b.gr - 4);
        b.gr = GROUND;
        c = b.flat(8);
        b.put(c + 2, 'B', -3).spikes(c + 5, c + 6, b.gr - 1);
        b.loop();
        c = b.flat(8);
        b.put(c + 4, 'C');
        c = b.pit(8);
        b.movingV(c + 1, b.gr - 1, 2, 60, 0.03).movingV(c + 5, b.gr - 3, 2, 60, 0.03);
        b.rings(c + 1, c + 6, b.gr - 6);
        c = b.flat(6);
        b.put(c + 3, 'E');
        b.up45(5);
        c = b.flat(10);
        b.put(c + 2, '3').put(c + 6, 'E').ringsOver(c + 1, c + 9, 2);
        b.down22(3);
        b.down45(2);
        c = b.flat(14);
        b.put(c + 3, 'K').put(c + 8, 'B', -3).put(c + 11, '4');
        b.put(c + 6, 'J').plat(c + 7, c + 13, b.gr - 6).rings(c + 7, c + 13, b.gr - 7);
        b.down22(1);
        c = b.flat(8);
        b.spikes(c + 3, c + 4, b.gr - 1).ringsOver(c + 2, c + 5, 4);
        b.loop();
        c = b.flat(20);
        b.put(c + 4, 'E').put(c + 8, 'K').put(c + 15, 'F');
        b.flat(14);
        return b.build("ZONE CANYON CRÉPUSCULE", 1, 1, Theme.SUNSET);
    }

    private static Level canyon2() {
        Builder b = new Builder(GROUND);
        int c = b.flat(12);
        b.put(c + 3, 'P').ringsOver(c + 5, c + 10, 2);
        b.down45(2);
        c = b.flat(6);
        b.put(c + 3, 'K');
        b.up45(2);
        c = b.flat(6);
        b.put(c + 3, '1');
        c = b.pit(10);
        b.movingH(c + 1, b.gr, 3, 2.0, 0.025).movingH(c + 6, b.gr - 2, 3, 1.5, 0.03);
        b.rings(c + 2, c + 8, b.gr - 4);
        c = b.flat(8);
        b.put(c + 4, 'E');
        b.up22(3);
        c = b.flat(6);
        b.put(c + 3, 'B', -3);
        b.down45(3);
        c = b.flat(12);
        b.spikes(c + 3, c + 4, b.gr - 1).spikes(c + 8, c + 9, b.gr - 1).put(c + 6, 'E');
        b.ringsOver(c + 2, c + 10, 4);
        c = b.flat(4);
        b.put(c + 2, 'C');
        b.loop();
        c = b.flat(10);
        b.put(c + 3, 'K').put(c + 7, '2');
        c = b.pit(7);
        b.movingV(c + 2, b.gr - 1, 3, 80, 0.02).rings(c + 2, c + 4, b.gr - 7);
        c = b.flat(10);
        b.put(c + 3, 'E').put(c + 6, 'B', -3);
        c = b.flat(4);
        b.put(c + 2, 'C');
        b.arena();
        return b.build("ZONE CANYON CRÉPUSCULE", 2, 1, Theme.SUNSET);
    }

    // ================================================================ zone 3

    private static Level factory1() {
        Builder b = new Builder(GROUND);
        int c = b.flat(12);
        b.put(c + 3, 'P').ringsOver(c + 6, c + 11, 2);
        c = b.flat(8);
        b.put(c + 2, 'K').spikes(c + 6, c + 7, b.gr - 1);
        c = b.pit(9);
        b.movingH(c + 1, b.gr, 3, 2.5, 0.025).rings(c + 2, c + 7, b.gr - 3);
        c = b.flat(8);
        b.put(c + 3, '1').put(c + 5, 'E');
        b.up45(3);
        c = b.flat(6);
        b.put(c + 3, 'B', -3);
        b.down45(3);
        b.loop();
        c = b.flat(8);
        b.put(c + 3, 'C');
        c = b.pit(8);
        b.movingV(c + 1, b.gr - 1, 2, 70, 0.03).movingV(c + 5, b.gr - 2, 2, 70, 0.025);
        c = b.flat(14);
        b.put(c + 2, 'J').plat(c + 3, c + 12, b.gr - 6).rings(c + 4, c + 11, b.gr - 7).at(c + 8, b.gr - 7, '4');
        b.put(c + 6, 'K').spikes(c + 10, c + 11, b.gr - 1);
        b.up22(2);
        b.flat(2);
        c = b.pit(6);
        b.arc(c, c + 5, b.gr - 4);
        b.gr = GROUND;
        c = b.flat(10);
        b.put(c + 3, 'E').put(c + 6, 'B', -4).put(c + 8, '3');
        b.loop();
        c = b.flat(20);
        b.put(c + 4, 'K').put(c + 9, 'E').put(c + 15, 'F');
        b.flat(14);
        return b.build("USINE DU DR. VORTEX", 1, 2, Theme.INDUSTRY);
    }

    private static Level factory2() {
        Builder b = new Builder(GROUND);
        int c = b.flat(12);
        b.put(c + 3, 'P').ringsOver(c + 6, c + 11, 2);
        c = b.pit(8);
        b.movingH(c + 1, b.gr, 3, 2.0, 0.03).rings(c + 2, c + 6, b.gr - 3);
        c = b.flat(8);
        b.put(c + 3, 'K').put(c + 6, '2');
        b.up45(3);
        c = b.flat(8);
        b.spikes(c + 3, c + 4, b.gr - 1).put(c + 6, 'E').ringsOver(c + 2, c + 5, 4);
        b.down22(3);
        c = b.flat(6);
        b.put(c + 3, 'C');
        c = b.pit(10);
        b.movingV(c + 1, b.gr - 1, 2, 60, 0.03).movingH(c + 4, b.gr - 3, 3, 1.5, 0.03);
        b.rings(c + 1, c + 8, b.gr - 6);
        c = b.flat(10);
        b.put(c + 3, 'K').put(c + 6, 'B', -3).put(c + 8, '1');
        b.loop();
        c = b.flat(4);
        b.put(c + 2, 'C');
        b.arena();
        return b.build("USINE DU DR. VORTEX", 2, 2, Theme.INDUSTRY);
    }

    // ================================================================ construction

    /**
     * Construit un niveau colonne par colonne. {@link #gr} est la ligne du sol courant
     * (la première ligne pleine) ; chaque segment avance le curseur {@link #cx}.
     */
    private static final class Builder {
        private static final int MAX_W = 600;

        private final char[][] g = new char[ROWS][MAX_W];
        private final List<Platform> platforms = new ArrayList<>();
        private final List<Loop> loops = new ArrayList<>();
        private final List<double[]> extraRings = new ArrayList<>();
        int cx;
        int gr;
        private int arenaX = -1;
        private int arenaRow;

        Builder(int groundRow) {
            gr = groundRow;
            for (char[] row : g) {
                Arrays.fill(row, '.');
            }
        }

        private void column(int col, int top) {
            for (int y = Math.max(0, top); y < ROWS; y++) {
                g[y][col] = '#';
            }
        }

        /** Sol plat sur {@code n} colonnes. Renvoie la première colonne du segment. */
        int flat(int n) {
            int start = cx;
            for (int i = 0; i < n; i++) {
                column(cx++, gr);
            }
            return start;
        }

        /** Gouffre de {@code n} colonnes. */
        int pit(int n) {
            int start = cx;
            cx += n;
            return start;
        }

        /** Montée à 45° de {@code n} tuiles de haut. */
        int up45(int n) {
            int start = cx;
            for (int i = 0; i < n; i++) {
                g[gr - 1][cx] = '/';
                column(cx, gr);
                gr--;
                cx++;
            }
            return start;
        }

        /** Descente à 45° de {@code n} tuiles. */
        int down45(int n) {
            int start = cx;
            for (int i = 0; i < n; i++) {
                g[gr][cx] = '\\';
                column(cx, gr + 1);
                gr++;
                cx++;
            }
            return start;
        }

        /** Montée douce : deux colonnes par tuile de hauteur. */
        int up22(int n) {
            int start = cx;
            for (int i = 0; i < n; i++) {
                g[gr - 1][cx] = 'a';
                column(cx++, gr);
                g[gr - 1][cx] = 'b';
                column(cx++, gr);
                gr--;
            }
            return start;
        }

        /** Descente douce. */
        int down22(int n) {
            int start = cx;
            for (int i = 0; i < n; i++) {
                g[gr][cx] = 'c';
                column(cx++, gr + 1);
                g[gr][cx] = 'd';
                column(cx++, gr + 1);
                gr++;
            }
            return start;
        }

        /** Looping sur 14 colonnes de sol plat, avec des anneaux tout autour. */
        int loop() {
            int start = flat(14);
            double lcx = (start + 7) * Game.T;
            double gy = gr * Game.T;
            Loop l = new Loop(lcx, gy);
            loops.add(l);
            for (int i = 1; i < 12; i++) {
                double a = i * Math.PI * 2 / 12;
                double r = Loop.R - 26;
                extraRings.add(new double[] {lcx + r * Math.sin(a), l.cy() + r * Math.cos(a)});
            }
            return start;
        }

        /** Arène du boss : 25 colonnes plates (la largeur de l'écran). */
        void arena() {
            arenaX = cx;
            arenaRow = gr;
            flat(Game.W / Game.T);
        }

        /** Objet posé sur le sol de la colonne {@code col} ({@code lift} négatif : plus haut, en lignes). */
        Builder put(int col, char c, int lift) {
            int row = 0;
            while (row < ROWS && g[row][col] == '.') {
                row++;
            }
            g[row - 1 + lift][col] = c;
            return this;
        }

        Builder put(int col, char c) {
            return put(col, c, 0);
        }

        /** Anneaux qui suivent le relief, {@code height} lignes au-dessus du sol. */
        Builder ringsOver(int c0, int c1, int height) {
            for (int col = c0; col <= c1; col++) {
                int row = 0;
                while (row < ROWS && g[row][col] == '.') {
                    row++;
                }
                if (row < ROWS) {
                    g[row - height][col] = 'o';
                }
            }
            return this;
        }

        Builder rings(int c0, int c1, int row) {
            for (int col = c0; col <= c1; col++) {
                g[row][col] = 'o';
            }
            return this;
        }

        Builder plat(int c0, int c1, int row) {
            for (int col = c0; col <= c1; col++) {
                g[row][col] = '=';
            }
            return this;
        }

        Builder spikes(int c0, int c1, int row) {
            for (int col = c0; col <= c1; col++) {
                g[row][col] = '^';
            }
            return this;
        }

        Builder at(int col, int row, char c) {
            g[row][col] = c;
            return this;
        }

        /** Arc d'anneaux en forme de saut. */
        Builder arc(int x0, int x1, int y) {
            for (int x = x0; x <= x1; x++) {
                double k = (x - x0) / (double) (x1 - x0);
                int rise = (int) Math.round(Math.sin(Math.PI * k) * 3);
                g[y - rise][x] = 'o';
            }
            return this;
        }

        /** Plate-forme qui va et vient horizontalement ; sa surface est au niveau de la ligne {@code row}. */
        Builder movingH(int col, int row, int widthTiles, double rangeTiles, double speed) {
            platforms.add(new Platform(col * Game.T, row * Game.T, widthTiles * Game.T, Platform.HORIZONTAL,
                    rangeTiles * Game.T, speed));
            return this;
        }

        /** Plate-forme qui monte et descend de {@code range} pixels. */
        Builder movingV(int col, int row, int widthTiles, double range, double speed) {
            platforms.add(new Platform(col * Game.T, row * Game.T, widthTiles * Game.T, Platform.VERTICAL,
                    range, speed));
            return this;
        }

        Level build(String zone, int act, int zoneIndex, Theme theme) {
            char[][] grid = new char[ROWS][cx];
            for (int y = 0; y < ROWS; y++) {
                System.arraycopy(g[y], 0, grid[y], 0, cx);
            }
            Level level = new Level(zone, act, zoneIndex, theme, grid);
            level.platforms.addAll(platforms);
            level.loops.addAll(loops);
            for (double[] r : extraRings) {
                level.rings.add(new Ring(r[0], r[1]));
            }
            if (arenaX >= 0) {
                level.bossArenaX = arenaX;
                level.bossGroundRow = arenaRow;
            }
            return level;
        }
    }
}
