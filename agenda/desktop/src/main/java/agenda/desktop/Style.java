package agenda.desktop;

import agenda.Category;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import java.awt.Component;

/** Couleurs, polices et petits composants dessinés à la main, pour un rendu identique sur tous les systèmes. */
final class Style {

    private Style() {
    }

    static final Color BG = new Color(0xF4F6FA);
    static final Color SURFACE = Color.WHITE;
    static final Color BORDER = new Color(0xE2E8F0);
    static final Color GRID = new Color(0xE9EDF3);
    static final Color GRID_LIGHT = new Color(0xF2F4F8);
    static final Color TEXT = new Color(0x0F172A);
    static final Color MUTED = new Color(0x64748B);
    static final Color FAINT = new Color(0xA7B1C2);
    static final Color ACCENT = new Color(0x2563EB);
    static final Color ACCENT_DARK = new Color(0x1D4ED8);
    static final Color ACCENT_SOFT = new Color(0xEAF1FE);
    static final Color HOVER = new Color(0xF1F5F9);
    static final Color WEEKEND = new Color(0xFAFBFD);
    static final Color OUTSIDE = new Color(0xF7F8FB);
    static final Color NOW = new Color(0xEF4444);
    static final Color DANGER = new Color(0xDC2626);

    static final String FAMILY = pickFamily();

    static Font font(int style, float size) {
        return new Font(FAMILY, style, 12).deriveFont(style, size);
    }

    private static String pickFamily() {
        String[] wanted = {"Segoe UI", "Helvetica Neue", "SF Pro Text", "Inter", "Noto Sans", "Ubuntu", "Cantarell", "DejaVu Sans"};
        java.util.Set<String> have = new java.util.HashSet<String>(java.util.Arrays.asList(
                java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String w : wanted) {
            if (have.contains(w)) return w;
        }
        return Font.SANS_SERIF;
    }

    static Color color(int argb) {
        return new Color(argb, true);
    }

    static Color color(Category c) {
        return new Color(c.color);
    }

    /** Couleur claire de fond d'un bloc d'événement. */
    static Color soft(Category c) {
        return new Color(c.light(0.84));
    }

    /** Couleur foncée du texte sur fond clair. */
    static Color deep(Category c) {
        return new Color(Category.mix(c.color, 0xFF000000, 0.45));
    }

    static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    static void fillRound(Graphics2D g, double x, double y, double w, double h, double r) {
        g.fill(new RoundRectangle2D.Double(x, y, w, h, r, r));
    }

    static void drawRound(Graphics2D g, double x, double y, double w, double h, double r) {
        g.draw(new RoundRectangle2D.Double(x, y, w, h, r, r));
    }

    /** Raccourcit un texte avec « … » pour qu'il tienne dans la largeur donnée. */
    static String fit(String s, FontMetrics fm, int width) {
        if (s == null) return "";
        if (fm.stringWidth(s) <= width) return s;
        String dots = "…";
        int w = width - fm.stringWidth(dots);
        if (w <= 0) return "";
        int n = s.length();
        while (n > 0 && fm.stringWidth(s.substring(0, n)) > w) n--;
        return s.substring(0, n).trim() + dots;
    }

    /** Pastille de couleur (pour les listes de catégories). */
    static Icon dot(final Color c, final int size) {
        return new Icon() {
            @Override
            public void paintIcon(Component comp, Graphics g, int x, int y) {
                Graphics2D g2 = smooth(g.create());
                g2.setColor(c);
                g2.fillOval(x + 1, y + 1, size - 2, size - 2);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    // ------------------------------------------------------------------ boutons plats

    enum Kind { PLAIN, ACCENT, GHOST, DANGER }

    /** Bouton arrondi dessiné à la main. */
    static class FlatButton extends JButton {
        private final Kind kind;

        FlatButton(String text, Kind kind) {
            super(text);
            this.kind = kind;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(font(kind == Kind.ACCENT ? Font.BOLD : Font.PLAIN, 13f));
            setForeground(kind == Kind.ACCENT ? Color.WHITE : kind == Kind.DANGER ? DANGER : TEXT);
            setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(Math.max(d.width, d.height), Math.max(d.height, 32));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g.create());
            boolean over = getModel().isRollover();
            boolean down = getModel().isPressed();
            int w = getWidth(), h = getHeight();
            switch (kind) {
                case ACCENT:
                    g2.setColor(down ? ACCENT_DARK.darker() : over ? ACCENT_DARK : ACCENT);
                    fillRound(g2, 0, 0, w, h, 10);
                    break;
                case GHOST:
                    if (over || down) {
                        g2.setColor(down ? GRID : HOVER);
                        fillRound(g2, 0, 0, w, h, 10);
                    }
                    break;
                default:
                    g2.setColor(down ? GRID : over ? HOVER : SURFACE);
                    fillRound(g2, 0, 0, w, h, 10);
                    g2.setColor(BORDER);
                    g2.setStroke(new BasicStroke(1f));
                    drawRound(g2, 0.5, 0.5, w - 1, h - 1, 10);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Bouton d'un groupe « segmenté » (Jour / Semaine / Mois / Liste). */
    static class Segment extends JToggleButton {
        Segment(String text) {
            super(text);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(font(Font.PLAIN, 13f));
            setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
            setForeground(MUTED);
            addItemListener(new java.awt.event.ItemListener() {
                @Override
                public void itemStateChanged(java.awt.event.ItemEvent e) {
                    setForeground(isSelected() ? TEXT : MUTED);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g.create());
            if (isSelected()) {
                g2.setColor(SURFACE);
                fillRound(g2, 2, 2, getWidth() - 4, getHeight() - 4, 8);
                g2.setColor(BORDER);
                drawRound(g2, 2.5, 2.5, getWidth() - 5, getHeight() - 5, 8);
            } else if (getModel().isRollover()) {
                g2.setColor(new Color(0xE6EAF0));
                fillRound(g2, 2, 2, getWidth() - 4, getHeight() - 4, 8);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
