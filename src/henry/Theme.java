package henry;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Couleurs, décor de fond et tuiles dessinées de chaque zone. */
final class Theme {

    static final int MEADOW = 0;
    static final int CANYON = 1;
    static final int FACTORY = 2;

    static final Theme PRAIRIE = new Theme(MEADOW, new Color(0x2a7fff), new Color(0xa8e4ff),
            new Color(0x6fcf5a), new Color(0x2f9e44), new Color(0xb5651d), new Color(0x8b4513),
            new Color(0x3ccf3c), new Color(0x1f8a2a));
    static final Theme SUNSET = new Theme(CANYON, new Color(0x4b2a7a), new Color(0xff9a4d),
            new Color(0x8a3d5c), new Color(0x5c2340), new Color(0xd2603a), new Color(0xa8432a),
            new Color(0xf4c26b), new Color(0xc98f3a));
    static final Theme INDUSTRY = new Theme(FACTORY, new Color(0x0b0f2a), new Color(0x2f2b5a),
            new Color(0x2a2f4a), new Color(0x1a1e33), new Color(0x7d8597), new Color(0x5c6475),
            new Color(0xf2c230), new Color(0x2b2b2b));

    final int style;
    final Color skyTop;
    final Color skyBottom;
    final Color far;
    final Color near;
    final Color dirt1;
    final Color dirt2;
    final Color surface;
    final Color surfaceEdge;
    final BufferedImage topTile;
    final BufferedImage fillTile;
    final BufferedImage platTile;
    final BufferedImage spikeTile;

    private Theme(int style, Color skyTop, Color skyBottom, Color far, Color near, Color dirt1, Color dirt2,
            Color surface, Color surfaceEdge) {
        this.style = style;
        this.skyTop = skyTop;
        this.skyBottom = skyBottom;
        this.far = far;
        this.near = near;
        this.dirt1 = dirt1;
        this.dirt2 = dirt2;
        this.surface = surface;
        this.surfaceEdge = surfaceEdge;
        fillTile = makeFill();
        topTile = makeTop();
        platTile = makePlatform();
        spikeTile = makeSpikes();
    }

    private static Graphics2D smooth(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return g;
    }

    private BufferedImage makeFill() {
        int t = Game.T;
        BufferedImage img = new BufferedImage(t, t, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = smooth(img);
        if (style == FACTORY) {
            g.setColor(dirt1);
            g.fillRect(0, 0, t, t);
            g.setColor(dirt1.brighter());
            g.fillRect(0, 0, t, 2);
            g.fillRect(0, 0, 2, t);
            g.setColor(dirt2);
            g.fillRect(0, t - 2, t, 2);
            g.fillRect(t - 2, 0, 2, t);
            g.setColor(new Color(0x9aa3b5));
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
                g.setColor(new Color(0, 0, 0, 40));
                g.drawLine(0, h, t, h);
            }
        }
        g.dispose();
        return img;
    }

    private BufferedImage makeTop() {
        int t = Game.T;
        BufferedImage img = new BufferedImage(t, t, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = smooth(img);
        g.drawImage(fillTile, 0, 0, null);
        if (style == FACTORY) {
            g.setColor(surfaceEdge);
            g.fillRect(0, 0, t, 9);
            g.setColor(surface);
            for (int x = -8; x < t; x += 12) {
                g.fillPolygon(new int[] {x, x + 6, x + 12, x + 6}, new int[] {9, 0, 0, 9}, 4);
            }
            g.setColor(Color.BLACK);
            g.fillRect(0, 9, t, 2);
        } else {
            g.setColor(surfaceEdge);
            g.fillRect(0, 0, t, 11);
            g.setColor(surface);
            g.fillRect(0, 0, t, 8);
            for (int x = 0; x < t; x += 6) {
                g.fillPolygon(new int[] {x, x + 3, x + 6}, new int[] {8, 13, 8}, 3);
            }
            g.setColor(surface.brighter());
            g.fillRect(0, 0, t, 2);
        }
        g.dispose();
        return img;
    }

    private BufferedImage makePlatform() {
        int t = Game.T;
        BufferedImage img = new BufferedImage(t, t, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = smooth(img);
        if (style == FACTORY) {
            g.setColor(new Color(0x9aa3b5));
            g.fillRect(0, 0, t, 10);
            g.setColor(new Color(0x4a5060));
            g.fillRect(0, 8, t, 3);
            g.setColor(surface);
            g.fillRect(0, 0, t, 3);
        } else {
            g.setColor(new Color(0x8a5a2b));
            g.fillRoundRect(0, 0, t, 12, 6, 6);
            g.setColor(new Color(0x5e3a17));
            g.drawLine(0, 6, t, 6);
            g.drawLine(t / 2, 0, t / 2, 12);
            g.setColor(surface);
            g.fillRect(0, 0, t, 3);
        }
        g.dispose();
        return img;
    }

    private BufferedImage makeSpikes() {
        int t = Game.T;
        BufferedImage img = new BufferedImage(t, t, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = smooth(img);
        g.setColor(new Color(0x555a66));
        g.fillRect(0, t - 6, t, 6);
        for (int i = 0; i < 3; i++) {
            int x = i * 11;
            Polygon p = new Polygon(new int[] {x, x + 5, x + 10}, new int[] {t - 6, 6, t - 6}, 3);
            g.setColor(new Color(0xd8dde6));
            g.fillPolygon(p);
            g.setColor(new Color(0x7c8494));
            g.setStroke(new BasicStroke(1f));
            g.drawPolygon(p);
            g.setColor(Color.WHITE);
            g.drawLine(x + 5, 8, x + 3, t - 8);
        }
        g.dispose();
        return img;
    }
}
