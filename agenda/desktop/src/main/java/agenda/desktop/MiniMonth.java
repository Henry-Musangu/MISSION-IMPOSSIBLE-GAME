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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;

/** Petit calendrier du panneau latéral, pour naviguer rapidement ; un point sous les jours occupés. */
final class MiniMonth extends JComponent {

    private static final int TOP = 34;
    private static final int NAMES = 20;
    private static final int CELL_H = 28;

    private final Controller ctl;
    private YearMonth month = YearMonth.now();
    private Map<LocalDate, List<Occurrence>> busy = Collections.emptyMap();
    private int hoverArrow;

    MiniMonth(Controller ctl) {
        this.ctl = ctl;
        setOpaque(true);
        MouseAdapter m = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int arrow = arrowAt(e.getX(), e.getY());
                if (arrow != 0) {
                    month = month.plusMonths(arrow);
                    refresh();
                    return;
                }
                LocalDate d = dayAt(e.getX(), e.getY());
                if (d != null) MiniMonth.this.ctl.select(d);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int a = arrowAt(e.getX(), e.getY());
                boolean hand = a != 0 || dayAt(e.getX(), e.getY()) != null;
                setCursor(Cursor.getPredefinedCursor(hand ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                if (a != hoverArrow) {
                    hoverArrow = a;
                    repaint();
                }
            }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
    }

    /** Suit le jour sélectionné. */
    void show(LocalDate selected) {
        month = YearMonth.from(selected);
        refresh();
    }

    void refresh() {
        busy = ctl.agenda().byDay(first(), 42);
        repaint();
    }

    private LocalDate first() {
        LocalDate f = month.atDay(1);
        return f.minusDays(f.getDayOfWeek().getValue() - 1);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(250, TOP + NAMES + 6 * CELL_H + 6);
    }

    private int arrowAt(int x, int y) {
        if (y > TOP - 4) return 0;
        if (x > getWidth() - 34) return 1;
        if (x > getWidth() - 64) return -1;
        return 0;
    }

    private LocalDate dayAt(int x, int y) {
        int top = TOP + NAMES;
        if (y < top || y >= top + 6 * CELL_H) return null;
        double cw = (getWidth() - 8) / 7.0;
        int col = (int) ((x - 4) / cw);
        if (col < 0 || col > 6) return null;
        int row = (y - top) / CELL_H;
        return first().plusDays(row * 7 + col);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = Style.smooth(g0.create());
        int w = getWidth();
        g.setColor(Style.SURFACE);
        g.fillRect(0, 0, w, getHeight());

        g.setFont(Style.font(Font.BOLD, 14f));
        g.setColor(Style.TEXT);
        g.drawString(Fmt.monthTitle(month), 8, 22);
        // flèches
        for (int k = -1; k <= 1; k += 2) {
            int cx = k < 0 ? w - 49 : w - 19;
            if (hoverArrow == k) {
                g.setColor(Style.HOVER);
                g.fillOval(cx - 12, 4, 24, 24);
            }
            g.setColor(Style.MUTED);
            int[] xs = k < 0 ? new int[] {cx + 3, cx - 3, cx + 3} : new int[] {cx - 3, cx + 3, cx - 3};
            g.fillPolygon(xs, new int[] {11, 16, 21}, 3);
        }

        double cw = (w - 8) / 7.0;
        g.setFont(Style.font(Font.BOLD, 10.5f));
        FontMetrics fm = g.getFontMetrics();
        for (int c = 0; c < 7; c++) {
            String s = Fmt.DAYS_LETTER[c];
            g.setColor(Style.FAINT);
            g.drawString(s, (int) (4 + c * cw + cw / 2 - fm.stringWidth(s) / 2.0), TOP + 12);
        }
        LocalDate today = ctl.today();
        LocalDate selected = ctl.selected();
        LocalDate first = first();
        Font normal = Style.font(Font.PLAIN, 12f);
        Font bold = Style.font(Font.BOLD, 12f);
        for (int i = 0; i < 42; i++) {
            LocalDate d = first.plusDays(i);
            int col = i % 7;
            int row = i / 7;
            int cx = (int) Math.round(4 + col * cw + cw / 2);
            int cy = TOP + NAMES + row * CELL_H + CELL_H / 2 - 2;
            boolean in = YearMonth.from(d).equals(month);
            boolean isToday = d.equals(today);
            boolean isSel = d.equals(selected);
            if (isSel) {
                g.setColor(isToday ? Style.ACCENT : Style.ACCENT_SOFT);
                g.fillOval(cx - 12, cy - 12, 24, 24);
            } else if (isToday) {
                g.setColor(Style.ACCENT);
                g.drawOval(cx - 12, cy - 12, 24, 24);
            }
            g.setFont(isToday || isSel ? bold : normal);
            fm = g.getFontMetrics();
            String s = String.valueOf(d.getDayOfMonth());
            g.setColor(isSel && isToday ? Color.WHITE : isToday || isSel ? Style.ACCENT_DARK : in ? Style.TEXT : Style.FAINT);
            g.drawString(s, cx - fm.stringWidth(s) / 2, cy + 4);
            List<Occurrence> list = busy.get(d);
            if (list != null && !list.isEmpty() && !(isSel && isToday)) {
                g.setColor(in ? Style.color(list.get(0).event.category) : Style.FAINT);
                g.fillOval(cx - 2, cy + 8, 4, 4);
            }
        }
        g.dispose();
    }
}
