package henry;

/** Couleurs, décor de fond et tuiles dessinées de chaque zone. */
final class Theme {

    static final int MEADOW = 0;
    static final int CANYON = 1;
    static final int FACTORY = 2;

    static final Theme PRAIRIE = new Theme(MEADOW, 0x2a7fff, 0xa8e4ff, 0x6fcf5a, 0x2f9e44,
            0xb5651d, 0x8b4513, 0x3ccf3c, 0x1f8a2a);
    static final Theme SUNSET = new Theme(CANYON, 0x4b2a7a, 0xff9a4d, 0x8a3d5c, 0x5c2340,
            0xd2603a, 0xa8432a, 0xf4c26b, 0xc98f3a);
    static final Theme INDUSTRY = new Theme(FACTORY, 0x0b0f2a, 0x2f2b5a, 0x2a2f4a, 0x1a1e33,
            0x7d8597, 0x5c6475, 0xf2c230, 0x2b2b2b);

    final int style;
    final int skyTop;
    final int skyBottom;
    final int far;
    final int near;
    final int dirt1;
    final int dirt2;
    final int surface;
    final int surfaceEdge;

    private Gfx.Sprite topTile;
    private Gfx.Sprite fillTile;
    private Gfx.Sprite platTile;
    private Gfx.Sprite spikeTile;
    private final Gfx.Sprite[] slopeTiles = new Gfx.Sprite[10];

    private Theme(int style, int skyTop, int skyBottom, int far, int near, int dirt1, int dirt2,
            int surface, int surfaceEdge) {
        this.style = style;
        this.skyTop = Col.rgb(skyTop);
        this.skyBottom = Col.rgb(skyBottom);
        this.far = Col.rgb(far);
        this.near = Col.rgb(near);
        this.dirt1 = Col.rgb(dirt1);
        this.dirt2 = Col.rgb(dirt2);
        this.surface = Col.rgb(surface);
        this.surfaceEdge = Col.rgb(surfaceEdge);
    }

    /** Prépare les tuiles la première fois qu'on en a besoin. */
    private void ensureSprites(Gfx g) {
        if (topTile != null) {
            return;
        }
        int t = Game.T;
        fillTile = g.makeSprite(t, t, new Gfx.Painter() {
            @Override
            public void paint(Gfx s) {
                paintFill(s);
            }
        });
        topTile = g.makeSprite(t, t, new Gfx.Painter() {
            @Override
            public void paint(Gfx s) {
                paintFill(s);
                paintTop(s);
            }
        });
        platTile = g.makeSprite(t, t, new Gfx.Painter() {
            @Override
            public void paint(Gfx s) {
                paintPlatform(s);
            }
        });
        spikeTile = g.makeSprite(t, t, new Gfx.Painter() {
            @Override
            public void paint(Gfx s) {
                paintSpikes(s);
            }
        });
        for (byte type = Level.UP45; type <= Level.DOWN22_LOW; type++) {
            final byte tt = type;
            slopeTiles[type] = g.makeSprite(t, t, new Gfx.Painter() {
                @Override
                public void paint(Gfx s) {
                    paintSlope(s, tt);
                }
            });
        }
    }

    Gfx.Sprite slope(Gfx g, byte type) {
        ensureSprites(g);
        return slopeTiles[type];
    }

    /** Couleur de l'eau (ou du liquide) visible au fond des gouffres. */
    int liquid() {
        if (style == FACTORY) {
            return Col.argb(200, 0x37b24d);
        } else if (style == CANYON) {
            return Col.argb(210, 0xd9480f);
        }
        return Col.argb(170, 0x1c7ed6);
    }

    /** Pente : damier découpé sous la ligne du sol, puis bande d'herbe qui suit la pente. */
    private void paintSlope(Gfx g, byte type) {
        int t = Game.T;
        int n = t + 1;
        double[] xs = new double[n + 2];
        double[] ys = new double[n + 2];
        for (int i = 0; i < n; i++) {
            xs[i] = i;
            ys[i] = t - Level.height(type, Math.min(i, t - 1));
        }
        xs[n] = t;
        ys[n] = t;
        xs[n + 1] = 0;
        ys[n + 1] = t;
        g.save();
        g.clipPolygon(xs, ys, n + 2);
        paintFill(g);
        g.restore();
        // bande de surface qui suit la pente
        int band = style == FACTORY ? 9 : 10;
        double[] bx = new double[2 * n];
        double[] by = new double[2 * n];
        for (int i = 0; i < n; i++) {
            bx[i] = xs[i];
            by[i] = ys[i];
            bx[2 * n - 1 - i] = xs[i];
            by[2 * n - 1 - i] = Math.min(t, ys[i] + band);
        }
        g.save();
        g.clipPolygon(xs, ys, n + 2);
        g.setColor(surfaceEdge);
        g.fillPolygon(bx, by, 2 * n);
        g.restore();
        for (int i = 0; i < n; i++) {
            by[2 * n - 1 - i] = Math.min(t, ys[i] + band - 3);
        }
        g.save();
        g.clipPolygon(xs, ys, n + 2);
        g.setColor(surface);
        g.fillPolygon(bx, by, 2 * n);
        g.restore();
    }

    Gfx.Sprite top(Gfx g) {
        ensureSprites(g);
        return topTile;
    }

    Gfx.Sprite fill(Gfx g) {
        ensureSprites(g);
        return fillTile;
    }

    Gfx.Sprite platform(Gfx g) {
        ensureSprites(g);
        return platTile;
    }

    Gfx.Sprite spikes(Gfx g) {
        ensureSprites(g);
        return spikeTile;
    }

    private void paintFill(Gfx g) {
        int t = Game.T;
        if (style == FACTORY) {
            g.setColor(dirt1);
            g.fillRect(0, 0, t, t);
            g.setColor(Col.brighter(dirt1));
            g.fillRect(0, 0, t, 2);
            g.fillRect(0, 0, 2, t);
            g.setColor(dirt2);
            g.fillRect(0, t - 2, t, 2);
            g.fillRect(t - 2, 0, 2, t);
            g.setColor(Col.rgb(0x9aa3b5));
            g.fillOval(5, 5, 4, 4);
            g.fillOval(t - 9, 5, 4, 4);
            g.fillOval(5, t - 9, 4, 4);
            g.fillOval(t - 9, t - 9, 4, 4);
        } else {
            int h = t / 2;
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    g.setColor(((i + j) & 1) == 0 ? dirt1 : dirt2);
                    g.fillRect(i * h, j * h, h, h);
                }
            }
            if (style == CANYON) {
                g.setColor(Col.argb(40, 0));
                g.fillRect(0, h, t, 1);
            }
        }
    }

    private void paintTop(Gfx g) {
        int t = Game.T;
        if (style == FACTORY) {
            g.setColor(surfaceEdge);
            g.fillRect(0, 0, t, 9);
            g.setColor(surface);
            for (int x = -8; x < t; x += 12) {
                g.fillPolygon(new double[] {x, x + 6, x + 12, x + 6}, new double[] {9, 0, 0, 9}, 4);
            }
            g.setColor(Col.BLACK);
            g.fillRect(0, 9, t, 2);
        } else {
            g.setColor(surfaceEdge);
            g.fillRect(0, 0, t, 11);
            g.setColor(surface);
            g.fillRect(0, 0, t, 8);
            for (int x = 0; x < t; x += 6) {
                g.fillPolygon(new double[] {x, x + 3, x + 6}, new double[] {8, 13, 8}, 3);
            }
            g.setColor(Col.brighter(surface));
            g.fillRect(0, 0, t, 2);
        }
    }

    private void paintPlatform(Gfx g) {
        int t = Game.T;
        if (style == FACTORY) {
            g.setColor(Col.rgb(0x9aa3b5));
            g.fillRect(0, 0, t, 10);
            g.setColor(Col.rgb(0x4a5060));
            g.fillRect(0, 8, t, 3);
            g.setColor(surface);
            g.fillRect(0, 0, t, 3);
        } else {
            g.setColor(Col.rgb(0x8a5a2b));
            g.fillRoundRect(0, 0, t, 12, 6);
            g.setColor(Col.rgb(0x5e3a17));
            g.fillRect(0, 6, t, 1);
            g.fillRect(t / 2, 0, 1, 12);
            g.setColor(surface);
            g.fillRect(0, 0, t, 3);
        }
    }

    private void paintSpikes(Gfx g) {
        int t = Game.T;
        g.setColor(Col.rgb(0x555a66));
        g.fillRect(0, t - 6, t, 6);
        g.setStroke(1f);
        for (int i = 0; i < 3; i++) {
            int x = i * 11;
            double[] xs = {x, x + 5, x + 10};
            double[] ys = {t - 6, 6, t - 6};
            g.setColor(Col.rgb(0xd8dde6));
            g.fillPolygon(xs, ys, 3);
            g.setColor(Col.rgb(0x7c8494));
            g.drawPolygon(xs, ys, 3);
            g.setColor(Col.WHITE);
            g.drawLine(x + 5, 8, x + 3, t - 8);
        }
    }
}
