package henry;

import java.util.Arrays;

/** Les trois zones du jeu, construites tuile par tuile. */
final class Levels {

    static final int COUNT = 3;

    private Levels() {
    }

    static Level create(int index) {
        switch (index) {
            case 0:
                return prairie();
            case 1:
                return canyon();
            default:
                return factory();
        }
    }

    private static Level prairie() {
        Builder b = new Builder(260);
        b.ground(0, 45, 11).at(3, 10, 'P');
        b.rings(8, 13, 9).at(20, 10, 'E');
        b.block(28, 10, 31, 10).block(32, 9, 35, 10).rings(29, 34, 7);
        b.arc(43, 53, 8);
        b.ground(51, 120, 11);
        b.at(60, 7, 'B');
        b.plat(64, 69, 8).rings(64, 69, 7);
        b.at(74, 10, 'J');
        b.plat(76, 92, 4).rings(78, 90, 3);
        b.at(82, 10, 'E').spikes(86, 87, 10);
        b.at(96, 10, 'E').at(102, 10, 'C');
        b.at(110, 10, 'E').at(116, 10, 'E');
        b.plat(122, 125, 9).rings(122, 125, 8);
        b.ground(127, 182, 11);
        b.rings(132, 160, 9).at(146, 10, 'E').at(152, 6, 'B').at(166, 10, 'E');
        b.spikes(172, 173, 10).at(178, 10, 'J');
        b.plat(183, 188, 5).rings(184, 187, 4);
        b.ground(189, 259, 11);
        b.at(196, 7, 'B').at(204, 10, 'E');
        b.at(207, 10, 'J').block(210, 8, 213, 10).rings(210, 213, 7);
        b.rings(220, 232, 9).at(228, 10, 'E');
        b.at(245, 10, 'F');
        return b.build("ZONE PRAIRIE ÉMERAUDE", 1, Theme.PRAIRIE);
    }

    private static Level canyon() {
        Builder b = new Builder(300);
        b.ground(0, 30, 11).at(3, 10, 'P');
        b.rings(6, 12, 9).at(16, 10, 'E');
        b.ground(31, 50, 9).at(40, 8, 'E').rings(33, 48, 7);
        b.plat(53, 55, 8).rings(53, 55, 7);
        b.ground(58, 110, 11);
        b.spikes(62, 63, 10).spikes(70, 71, 10);
        b.at(66, 6, 'B').rings(64, 69, 8);
        b.at(76, 10, 'J').plat(78, 84, 4).rings(79, 83, 3);
        b.at(88, 10, 'E').at(94, 10, 'E');
        b.block(98, 9, 101, 10).at(104, 10, 'C');
        b.ground(114, 119, 10).rings(114, 119, 8);
        b.ground(123, 125, 9).rings(123, 125, 7);
        b.at(121, 4, 'B');
        b.ground(128, 170, 11);
        b.rings(130, 150, 9).at(136, 10, 'E').at(142, 10, 'E').at(148, 10, 'E');
        b.spikes(155, 157, 10).plat(154, 158, 7);
        b.at(164, 10, 'J');
        b.ground(171, 200, 6).rings(172, 190, 5).at(180, 5, 'E').at(195, 5, 'C');
        b.ground(201, 240, 11);
        b.at(208, 7, 'B').at(214, 10, 'E').spikes(220, 221, 10);
        b.rings(224, 236, 9).at(230, 10, 'E');
        b.plat(242, 245, 9).rings(242, 245, 8);
        b.ground(247, 299, 11);
        b.at(255, 6, 'B').at(262, 10, 'E').rings(266, 276, 9);
        b.at(285, 10, 'F');
        return b.build("ZONE CANYON CRÉPUSCULE", 1, Theme.SUNSET);
    }

    private static Level factory() {
        int arena = 150;
        Builder b = new Builder(arena + Game.W / Game.T);
        b.ground(0, 40, 11).at(3, 10, 'P');
        b.rings(6, 12, 9).at(15, 10, 'E');
        b.spikes(20, 22, 10).plat(19, 23, 7).rings(19, 23, 6);
        b.at(28, 6, 'B').block(32, 9, 34, 10);
        b.at(38, 10, 'J').plat(40, 52, 4).rings(41, 51, 3);
        b.ground(48, 90, 11);
        b.at(54, 10, 'E').at(60, 7, 'B').at(66, 10, 'E');
        b.spikes(70, 71, 10).spikes(76, 77, 10).rings(72, 75, 8);
        b.at(82, 10, 'C');
        b.plat(92, 95, 9).rings(92, 95, 8);
        b.ground(97, 140, 11);
        b.at(104, 10, 'E').at(110, 6, 'B').at(116, 10, 'E').at(122, 7, 'B');
        b.rings(120, 130, 9).at(138, 10, 'C');
        b.ground(141, arena + Game.W / Game.T - 1, 11).rings(143, 147, 9);
        Level level = b.build("USINE DU DR. VORTEX", 1, Theme.INDUSTRY);
        level.bossArenaX = arena;
        return level;
    }

    /** Petit outil pour dessiner un niveau sans écrire de longues chaînes de caractères. */
    private static final class Builder {
        private static final int ROWS = 15;
        private final char[][] g;

        Builder(int width) {
            g = new char[ROWS][width];
            for (char[] row : g) {
                Arrays.fill(row, '.');
            }
        }

        Builder ground(int x0, int x1, int top) {
            return block(x0, top, x1, ROWS - 1);
        }

        Builder block(int x0, int y0, int x1, int y1) {
            for (int y = y0; y <= y1; y++) {
                for (int x = x0; x <= x1; x++) {
                    g[y][x] = '#';
                }
            }
            return this;
        }

        Builder plat(int x0, int x1, int y) {
            return line(x0, x1, y, '=');
        }

        Builder spikes(int x0, int x1, int y) {
            return line(x0, x1, y, '^');
        }

        Builder rings(int x0, int x1, int y) {
            return line(x0, x1, y, 'o');
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

        Builder at(int x, int y, char c) {
            g[y][x] = c;
            return this;
        }

        private Builder line(int x0, int x1, int y, char c) {
            for (int x = x0; x <= x1; x++) {
                g[y][x] = c;
            }
            return this;
        }

        Level build(String zone, int act, Theme theme) {
            return new Level(zone, act, theme, g);
        }
    }
}
