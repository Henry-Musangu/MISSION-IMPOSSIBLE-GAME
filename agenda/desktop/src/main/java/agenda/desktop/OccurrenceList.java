package agenda.desktop;

import agenda.Fmt;
import agenda.Occurrence;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/** Liste d'événements, groupés par jour : panneau latéral, vue « Liste » et résultats de recherche. */
final class OccurrenceList extends JComponent implements Scrollable {

    private static final int HEADER_H = 34;
    private static final int MESSAGE_H = 48;

    /** Une ligne : un titre de jour, un événement ou un message. */
    static final class Row {
        final String heading;
        final Occurrence occurrence;
        /** Jour depuis lequel on regarde l'événement (null : afficher sa date complète). */
        final LocalDate day;
        final String message;
        final boolean highlight;

        private Row(String heading, Occurrence occurrence, LocalDate day, String message, boolean highlight) {
            this.heading = heading;
            this.occurrence = occurrence;
            this.day = day;
            this.message = message;
            this.highlight = highlight;
        }

        static Row heading(String text, boolean highlight) {
            return new Row(text, null, null, null, highlight);
        }

        static Row item(Occurrence o, LocalDate day) {
            return new Row(null, o, day, null, false);
        }

        static Row message(String text) {
            return new Row(null, null, null, text, false);
        }
    }

    private final Controller ctl;
    private final boolean compact;
    private List<Row> rows = new ArrayList<Row>();
    private final List<Rectangle> bounds = new ArrayList<Rectangle>();
    private int hover = -1;

    /** @param compact true pour le panneau latéral (plus étroit) */
    OccurrenceList(Controller ctl, boolean compact) {
        this.ctl = ctl;
        this.compact = compact;
        setOpaque(true);
        setToolTipText("");
        MouseAdapter m = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                int i = indexAt(e.getX(), e.getY());
                if (i >= 0 && rows.get(i).occurrence != null) {
                    OccurrenceList.this.ctl.open(rows.get(i).occurrence);
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int i = indexAt(e.getX(), e.getY());
                if (i >= 0 && rows.get(i).occurrence == null) i = -1;
                if (i != hover) {
                    hover = i;
                    setCursor(Cursor.getPredefinedCursor(i >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = -1;
                repaint();
            }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
    }

    void setRows(List<Row> rows) {
        this.rows = rows;
        hover = -1;
        revalidate();
        repaint();
    }

    private int indexAt(int x, int y) {
        for (int i = 0; i < bounds.size(); i++) {
            if (bounds.get(i).contains(x, y)) return i;
        }
        return -1;
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        int i = indexAt(e.getX(), e.getY());
        if (i < 0 || rows.get(i).occurrence == null) return null;
        Row r = rows.get(i);
        String notes = r.occurrence.event.notes.trim();
        if (notes.isEmpty()) return null;
        return "<html><div style='width:260px'>" + Desk.html(notes) + "</div></html>";
    }

    private int itemHeight(Row r) {
        int h = 46;
        if (!r.occurrence.event.location.trim().isEmpty()) h += 17;
        if (!compact && !r.occurrence.event.notes.trim().isEmpty()) h += 17;
        return h;
    }

    private int rowHeight(Row r) {
        if (r.heading != null) return HEADER_H;
        if (r.message != null) return MESSAGE_H;
        return itemHeight(r);
    }

    @Override
    public Dimension getPreferredSize() {
        int h = 8;
        for (Row r : rows) h += rowHeight(r) + (r.occurrence != null ? 6 : 0);
        return new Dimension(compact ? 260 : 500, h + 8);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 24;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return orientation == SwingConstants.VERTICAL ? visibleRect.height - 40 : visibleRect.width;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return getParent() != null && getParent().getHeight() > getPreferredSize().height;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = Style.smooth(g0.create());
        int w = getWidth();
        g.setColor(compact ? Style.SURFACE : Style.BG);
        g.fillRect(0, 0, w, getHeight());
        bounds.clear();
        int pad = compact ? 12 : 24;
        int maxW = compact ? w - 2 * pad : Math.min(w - 2 * pad, 760);
        int left = compact ? pad : (w - maxW) / 2;
        int y = 8;
        LocalDateTime now = LocalDateTime.now();

        Font headFont = Style.font(Font.BOLD, compact ? 12f : 13f);
        Font titleFont = Style.font(Font.BOLD, 13.5f);
        Font textFont = Style.font(Font.PLAIN, 12.5f);
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            int h = rowHeight(r);
            Rectangle b = new Rectangle(left, y, maxW, h);
            bounds.add(b);
            if (r.heading != null) {
                g.setFont(headFont);
                g.setColor(r.highlight ? Style.ACCENT : Style.MUTED);
                g.drawString(Style.fit(r.heading, g.getFontMetrics(), maxW), left + 2, y + h - 10);
            } else if (r.message != null) {
                g.setFont(textFont);
                g.setColor(Style.FAINT);
                g.drawString(r.message, left + 2, y + 28);
            } else {
                Occurrence o = r.occurrence;
                boolean past = o.end.isBefore(now) || (o.end.equals(o.start) && o.start.isBefore(now));
                g.setColor(i == hover ? Style.HOVER : Style.SURFACE);
                Style.fillRound(g, b.x, b.y, b.width, b.height, 12);
                g.setColor(Style.BORDER);
                Style.drawRound(g, b.x + 0.5, b.y + 0.5, b.width - 1, b.height - 1, 12);
                Color base = Style.color(o.event.category);
                g.setColor(past ? new Color(o.event.category.light(0.55)) : base);
                Style.fillRound(g, b.x + 10, b.y + 10, 4, b.height - 20, 4);

                int tx = b.x + 24;
                int tw = b.width - 34;
                int yy = b.y + 21;
                g.setFont(titleFont);
                FontMetrics fm = g.getFontMetrics();
                g.setColor(past ? Style.MUTED : Style.TEXT);
                String title = o.event.displayTitle();
                String rec = o.event.isRecurring() ? "  ↻" : "";
                g.drawString(Style.fit(title, fm, tw - fm.stringWidth(rec)) + rec, tx, yy);
                g.setFont(textFont);
                fm = g.getFontMetrics();
                g.setColor(Style.MUTED);
                yy += 18;
                String when = r.day != null ? Fmt.whenOn(o, r.day)
                        : Fmt.capitalize(Fmt.shortDate(o.date())) + (o.date().getYear() != ctl.today().getYear() ? " " + o.date().getYear() : "") + " · " + Fmt.when(o);
                g.drawString(Style.fit(when + "  ·  " + o.event.category.label, fm, tw), tx, yy);
                if (!o.event.location.trim().isEmpty()) {
                    yy += 17;
                    g.drawString(Style.fit("Lieu : " + o.event.location.trim(), fm, tw), tx, yy);
                }
                if (!compact && !o.event.notes.trim().isEmpty()) {
                    yy += 17;
                    g.setColor(Style.FAINT);
                    g.drawString(Style.fit(o.event.notes.trim().replace('\n', ' '), fm, tw), tx, yy);
                }
                y += 6;
            }
            y += h;
        }
        g.dispose();
    }
}
