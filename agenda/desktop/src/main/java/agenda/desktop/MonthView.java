package agenda.desktop;

import agenda.Fmt;
import agenda.Occurrence;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/** Le mois en grille de 6 semaines, avec les événements de chaque jour. */
final class MonthView extends JComponent {

    private static final int HEADER = 30;
    private static final int LINE = 19;

    private final Controller ctl;
    private YearMonth month;
    private Map<LocalDate, List<Occurrence>> days = Collections.emptyMap();
    private final List<Hit> hits = new ArrayList<Hit>();
    private Hit hover;
    private double wheel;

    /** Zone cliquable : une pastille d'événement ou le lien « +N autres ». */
    private static final class Hit {
        final Rectangle r;
        final Occurrence occurrence;
        final LocalDate day;

        Hit(Rectangle r, Occurrence occurrence, LocalDate day) {
            this.r = r;
            this.occurrence = occurrence;
            this.day = day;
        }
    }

    MonthView(Controller ctl) {
        this.ctl = ctl;
        setOpaque(true);
        setToolTipText("");
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                Hit h = hitAt(e.getX(), e.getY());
                LocalDate day = dayAt(e.getX(), e.getY());
                if (h != null && h.occurrence != null) {
                    MonthView.this.ctl.select(h.day);
                    MonthView.this.ctl.open(h.occurrence);
                } else if (h != null) {
                    MonthView.this.ctl.showDay(h.day);
                } else if (day != null) {
                    if (e.getClickCount() >= 2) {
                        MonthView.this.ctl.create(Desk.defaultStart(day, MonthView.this.ctl.today()), false);
                    } else {
                        MonthView.this.ctl.select(day);
                    }
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                Hit h = hitAt(e.getX(), e.getY());
                if (h != hover) {
                    hover = h;
                    setCursor(java.awt.Cursor.getPredefinedCursor(h != null ? java.awt.Cursor.HAND_CURSOR : java.awt.Cursor.DEFAULT_CURSOR));
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = null;
                repaint();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                wheel += e.getPreciseWheelRotation();
                if (Math.abs(wheel) >= 1) {
                    int step = wheel > 0 ? 1 : -1;
                    wheel = 0;
                    MonthView.this.ctl.select(month.plusMonths(step).atDay(1));
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);
    }

    void show(LocalDate selected) {
        month = YearMonth.from(selected);
        days = ctl.agenda().byDay(firstShown(), 42);
        repaint();
    }

    String title() {
        return Fmt.monthTitle(month);
    }

    private LocalDate firstShown() {
        LocalDate first = month.atDay(1);
        return first.minusDays(first.getDayOfWeek().getValue() - 1);
    }

    private double cellW() {
        return getWidth() / 7.0;
    }

    private double cellH() {
        return (getHeight() - HEADER) / 6.0;
    }

    private LocalDate dayAt(int x, int y) {
        if (y < HEADER) return null;
        int col = Math.min(6, (int) (x / cellW()));
        int row = Math.min(5, (int) ((y - HEADER) / cellH()));
        return firstShown().plusDays(row * 7 + col);
    }

    private Hit hitAt(int x, int y) {
        for (Hit h : hits) {
            if (h.r.contains(x, y)) return h;
        }
        return null;
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        Hit h = hitAt(e.getX(), e.getY());
        if (h == null) return null;
        if (h.occurrence == null) return "Voir toute la journée";
        return Desk.tooltip(h.occurrence, h.day);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        if (month == null) return;
        Graphics2D g = Style.smooth(g0.create());
        int w = getWidth();
        int h = getHeight();
        g.setColor(Style.SURFACE);
        g.fillRect(0, 0, w, h);
        hits.clear();

        double cw = cellW();
        double ch = cellH();
        LocalDate today = ctl.today();
        LocalDate selected = ctl.selected();
        LocalDate first = firstShown();

        // en-tête : noms des jours
        g.setFont(Style.font(Font.BOLD, 11f));
        FontMetrics fm = g.getFontMetrics();
        for (int c = 0; c < 7; c++) {
            String name = Fmt.DAYS_SHORT[c].replace(".", "").toUpperCase(java.util.Locale.FRENCH);
            g.setColor(c >= 5 ? Style.FAINT : Style.MUTED);
            int x = (int) (c * cw + 10);
            g.drawString(name, x, HEADER - 10);
        }

        Font numberFont = Style.font(Font.PLAIN, 12.5f);
        Font numberBold = Style.font(Font.BOLD, 12.5f);
        Font chipFont = Style.font(Font.PLAIN, 11.5f);
        Font chipBold = Style.font(Font.BOLD, 11.5f);

        for (int i = 0; i < 42; i++) {
            LocalDate day = first.plusDays(i);
            int col = i % 7;
            int row = i / 7;
            int x = (int) Math.round(col * cw);
            int y = (int) Math.round(HEADER + row * ch);
            int x2 = (int) Math.round((col + 1) * cw);
            int y2 = (int) Math.round(HEADER + (row + 1) * ch);
            int cwi = x2 - x;
            int chi = y2 - y;
            boolean inMonth = YearMonth.from(day).equals(month);

            Color bg = !inMonth ? Style.OUTSIDE : col >= 5 ? Style.WEEKEND : Style.SURFACE;
            if (day.equals(selected)) bg = Style.ACCENT_SOFT;
            g.setColor(bg);
            g.fillRect(x, y, cwi, chi);

            // numéro du jour
            String num = day.getDayOfMonth() == 1 ? "1 " + Fmt.MONTHS_SHORT[day.getMonthValue() - 1] : String.valueOf(day.getDayOfMonth());
            boolean isToday = day.equals(today);
            g.setFont(isToday || day.getDayOfMonth() == 1 ? numberBold : numberFont);
            fm = g.getFontMetrics();
            int nx = x + 8;
            int ny = y + 18;
            if (isToday) {
                int tw = Math.max(22, fm.stringWidth(num) + 12);
                g.setColor(Style.ACCENT);
                Style.fillRound(g, nx - 5, y + 4, tw, 20, 20);
                g.setColor(Color.WHITE);
                g.drawString(num, nx - 5 + (tw - fm.stringWidth(num)) / 2, ny);
            } else {
                g.setColor(inMonth ? Style.TEXT : Style.FAINT);
                g.drawString(num, nx, ny);
            }

            // événements
            List<Occurrence> list = days.get(day);
            if (list == null || list.isEmpty()) continue;
            int top = y + 28;
            int maxLines = Math.max(0, (y2 - 4 - top) / LINE);
            int shown = list.size() <= maxLines ? list.size() : Math.max(0, maxLines - 1);
            for (int k = 0; k < shown; k++) {
                Occurrence o = list.get(k);
                Rectangle r = new Rectangle(x + 4, top + k * LINE, cwi - 8, LINE - 2);
                Hit hit = new Hit(r, o, day);
                hits.add(hit);
                boolean hov = hover != null && hover.occurrence == o && hover.day.equals(day);
                Color base = Style.color(o.event.category);
                boolean filled = o.event.allDay || o.isMultiDay();
                if (filled) {
                    g.setColor(hov ? base.darker() : base);
                    Style.fillRound(g, r.x, r.y, r.width, r.height, 7);
                    g.setColor(Color.WHITE);
                    g.setFont(chipBold);
                    fm = g.getFontMetrics();
                    String label = o.event.displayTitle();
                    if (!o.event.allDay && o.date().equals(day)) label = Fmt.shortTime(o.start.toLocalTime()) + " " + label;
                    g.drawString(Style.fit(label, fm, r.width - 12), r.x + 6, r.y + r.height - 4);
                } else {
                    if (hov) {
                        g.setColor(Style.HOVER);
                        Style.fillRound(g, r.x, r.y, r.width, r.height, 7);
                    }
                    g.setColor(base);
                    g.fillOval(r.x + 4, r.y + r.height / 2 - 3, 7, 7);
                    g.setFont(chipBold);
                    fm = g.getFontMetrics();
                    String time = Fmt.shortTime(o.start.toLocalTime());
                    g.setColor(Style.MUTED);
                    int tx = r.x + 16;
                    g.drawString(time, tx, r.y + r.height - 4);
                    int tw = fm.stringWidth(time) + 5;
                    g.setFont(chipFont);
                    fm = g.getFontMetrics();
                    g.setColor(Style.TEXT);
                    g.drawString(Style.fit(o.event.displayTitle(), fm, r.width - 16 - tw - 4), tx + tw, r.y + r.height - 4);
                }
            }
            if (shown < list.size()) {
                int more = list.size() - shown;
                Rectangle r = new Rectangle(x + 4, top + shown * LINE, cwi - 8, LINE - 2);
                hits.add(new Hit(r, null, day));
                boolean hov = hover != null && hover.occurrence == null && hover.day.equals(day);
                g.setFont(Style.font(Font.BOLD, 11f));
                g.setColor(hov ? Style.ACCENT : Style.MUTED);
                g.drawString(more == 1 ? "+1 autre" : "+" + more + " autres", r.x + 6, r.y + r.height - 4);
            }
        }

        // quadrillage
        g.setColor(Style.GRID);
        g.setStroke(new BasicStroke(1f));
        g.drawLine(0, HEADER, w, HEADER);
        for (int r = 1; r < 6; r++) {
            int y = (int) Math.round(HEADER + r * ch);
            g.drawLine(0, y, w, y);
        }
        for (int c = 1; c < 7; c++) {
            int x = (int) Math.round(c * cw);
            g.drawLine(x, HEADER, x, h);
        }
        g.dispose();
    }
}
