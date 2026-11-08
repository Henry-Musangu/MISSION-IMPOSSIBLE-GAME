package agenda.desktop;

import agenda.DayLayout;
import agenda.Fmt;
import agenda.Occurrence;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/** Vue « Jour » ou « Semaine » : les heures de la journée en colonnes, les événements en blocs. */
final class TimeGridView extends JPanel {

    private static final int GUTTER = 58;
    private static final int HOUR = 46;
    private static final int DAY_HEAD = 46;
    private static final int LANE = 21;
    private static final int MAX_LANES = 3;

    private final Controller ctl;
    private final int count;
    private LocalDate first;
    private Map<LocalDate, List<Occurrence>> days = Collections.emptyMap();
    private final Body body = new Body();
    private final Head head = new Head();
    private final JScrollPane scroll;
    private boolean scrolled;

    /** Zone cliquable d'un événement. */
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

    /** @param count 1 pour la vue Jour, 7 pour la vue Semaine */
    TimeGridView(Controller ctl, int count) {
        super(new BorderLayout());
        this.ctl = ctl;
        this.count = count;
        scroll = new JScrollPane(body, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setColumnHeaderView(head);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(HOUR / 2);
        scroll.getViewport().setBackground(Style.SURFACE);
        add(scroll, BorderLayout.CENTER);
    }

    void show(LocalDate selected) {
        first = count == 1 ? selected : selected.minusDays(selected.getDayOfWeek().getValue() - 1);
        days = ctl.agenda().byDay(first, count);
        head.revalidate();
        head.repaint();
        body.repaint();
        if (!scrolled) {
            scrolled = true;
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    int hour = Math.max(0, Math.min(LocalTime.now().getHour() - 1, 7));
                    scroll.getViewport().setViewPosition(new java.awt.Point(0, Math.max(0, hour * HOUR - 12)));
                }
            });
        }
    }

    String title() {
        if (count == 1) return Fmt.capitalize(Fmt.longDate(first));
        return Fmt.weekTitle(first);
    }

    /** Fait défiler pour montrer cette heure. */
    void scrollTo(LocalTime t) {
        Rectangle r = new Rectangle(0, Math.max(0, t.getHour() * HOUR - HOUR), 10, HOUR * 3);
        body.scrollRectToVisible(r);
    }

    private double colW(int width) {
        return (width - GUTTER) / (double) count;
    }

    private LocalDate dayAtX(int x, int width) {
        if (x < GUTTER) return null;
        int c = Math.min(count - 1, (int) ((x - GUTTER) / colW(width)));
        return first.plusDays(c);
    }

    private List<Occurrence> fullDay(LocalDate day) {
        List<Occurrence> out = new ArrayList<Occurrence>();
        List<Occurrence> list = days.get(day);
        if (list == null) return out;
        for (Occurrence o : list) {
            if (o.fillsDay(day)) out.add(o);
        }
        return out;
    }

    private int lanes() {
        int n = 1;
        for (LocalDate d : days.keySet()) n = Math.max(n, fullDay(d).size());
        return Math.min(n, MAX_LANES + 1);
    }

    // ================================================================== en-tête

    private final class Head extends JComponent {
        final List<Hit> hits = new ArrayList<Hit>();
        Hit hover;

        Head() {
            setOpaque(true);
            setToolTipText("");
            MouseAdapter m = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (!SwingUtilities.isLeftMouseButton(e)) return;
                    Hit h = hitAt(e.getX(), e.getY());
                    LocalDate day = dayAtX(e.getX(), getWidth());
                    if (h != null && h.occurrence != null) {
                        ctl.open(h.occurrence);
                    } else if (h != null) {
                        ctl.showDay(h.day);
                    } else if (day != null && e.getY() < DAY_HEAD) {
                        if (count > 1) ctl.showDay(day); else ctl.select(day);
                    } else if (day != null && e.getClickCount() >= 2) {
                        ctl.create(day.atStartOfDay(), true);
                    } else if (day != null) {
                        ctl.select(day);
                    }
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    Hit h = hitAt(e.getX(), e.getY());
                    boolean hand = h != null || (e.getY() < DAY_HEAD && e.getX() > GUTTER && count > 1);
                    setCursor(Cursor.getPredefinedCursor(hand ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                    if (h != hover) {
                        hover = h;
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = null;
                    repaint();
                }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
        }

        Hit hitAt(int x, int y) {
            for (Hit h : hits) if (h.r.contains(x, y)) return h;
            return null;
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            Hit h = hitAt(e.getX(), e.getY());
            if (h != null) return h.occurrence == null ? "Voir toute la journée" : Desk.tooltip(h.occurrence, h.day);
            if (e.getY() >= DAY_HEAD && e.getX() > GUTTER) return "Double-clic : nouvel événement sur la journée";
            return null;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(100, DAY_HEAD + lanes() * LANE + 8);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            if (first == null) return;
            Graphics2D g = Style.smooth(g0.create());
            int w = getWidth();
            int h = getHeight();
            g.setColor(Style.SURFACE);
            g.fillRect(0, 0, w, h);
            hits.clear();
            double cw = colW(w);
            LocalDate today = ctl.today();

            g.setFont(Style.font(Font.PLAIN, 10.5f));
            g.setColor(Style.FAINT);
            g.drawString("journée", 8, DAY_HEAD + 15);

            for (int c = 0; c < count; c++) {
                LocalDate day = first.plusDays(c);
                int x = (int) Math.round(GUTTER + c * cw);
                int x2 = (int) Math.round(GUTTER + (c + 1) * cw);
                boolean isToday = day.equals(today);
                if (day.equals(ctl.selected()) && count > 1) {
                    g.setColor(Style.ACCENT_SOFT);
                    g.fillRect(x, 0, x2 - x, DAY_HEAD);
                }
                // nom et numéro du jour
                String name = Fmt.DAYS_SHORT[day.getDayOfWeek().getValue() - 1].replace(".", "").toUpperCase(java.util.Locale.FRENCH);
                g.setFont(Style.font(Font.BOLD, 10.5f));
                FontMetrics fm = g.getFontMetrics();
                g.setColor(isToday ? Style.ACCENT : Style.MUTED);
                int cx = (x + x2) / 2;
                g.drawString(name, cx - fm.stringWidth(name) / 2, 15);
                String num = String.valueOf(day.getDayOfMonth());
                g.setFont(Style.font(isToday ? Font.BOLD : Font.PLAIN, 17f));
                fm = g.getFontMetrics();
                if (isToday) {
                    g.setColor(Style.ACCENT);
                    g.fillOval(cx - 14, 18, 28, 26);
                    g.setColor(Color.WHITE);
                } else {
                    g.setColor(Style.TEXT);
                }
                g.drawString(num, cx - fm.stringWidth(num) / 2, 38);

                // événements sur la journée entière
                List<Occurrence> full = fullDay(day);
                int shown = full.size() <= MAX_LANES + 1 ? full.size() : MAX_LANES;
                g.setFont(Style.font(Font.BOLD, 11.5f));
                fm = g.getFontMetrics();
                for (int k = 0; k < shown; k++) {
                    Occurrence o = full.get(k);
                    Rectangle r = new Rectangle(x + 3, DAY_HEAD + 2 + k * LANE, x2 - x - 6, LANE - 3);
                    hits.add(new Hit(r, o, day));
                    Color base = Style.color(o.event.category);
                    boolean hov = hover != null && hover.occurrence == o && day.equals(hover.day);
                    g.setColor(hov ? base.darker() : base);
                    Style.fillRound(g, r.x, r.y, r.width, r.height, 7);
                    g.setColor(Color.WHITE);
                    g.drawString(Style.fit(o.event.displayTitle(), fm, r.width - 10), r.x + 6, r.y + r.height - 5);
                }
                if (shown < full.size()) {
                    Rectangle r = new Rectangle(x + 3, DAY_HEAD + 2 + shown * LANE, x2 - x - 6, LANE - 3);
                    hits.add(new Hit(r, null, day));
                    g.setColor(Style.MUTED);
                    g.drawString("+" + (full.size() - shown) + " autres", r.x + 6, r.y + r.height - 5);
                }
            }
            g.setColor(Style.GRID);
            g.drawLine(0, h - 1, w, h - 1);
            for (int c = 0; c <= count; c++) {
                int x = (int) Math.round(GUTTER + c * cw);
                g.drawLine(x, DAY_HEAD, x, h);
            }
            g.dispose();
        }
    }

    // ================================================================== grille horaire

    private final class Body extends JComponent implements Scrollable {
        final List<Hit> hits = new ArrayList<Hit>();
        Hit hover;

        Body() {
            setOpaque(true);
            setToolTipText("");
            MouseAdapter m = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (!SwingUtilities.isLeftMouseButton(e)) return;
                    Hit h = hitAt(e.getX(), e.getY());
                    LocalDate day = dayAtX(e.getX(), getWidth());
                    if (h != null) {
                        ctl.open(h.occurrence);
                    } else if (day != null && e.getClickCount() >= 2) {
                        int minutes = Math.max(0, Math.min(23 * 60 + 30, (e.getY() * 60 / HOUR) / 30 * 30));
                        ctl.create(day.atStartOfDay().plusMinutes(minutes), false);
                    } else if (day != null && count > 1) {
                        ctl.select(day);
                    }
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    Hit h = hitAt(e.getX(), e.getY());
                    if (h != hover) {
                        hover = h;
                        setCursor(Cursor.getPredefinedCursor(h != null ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = null;
                    repaint();
                }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
        }

        Hit hitAt(int x, int y) {
            for (int i = hits.size() - 1; i >= 0; i--) {
                if (hits.get(i).r.contains(x, y)) return hits.get(i);
            }
            return null;
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            Hit h = hitAt(e.getX(), e.getY());
            if (h != null) return Desk.tooltip(h.occurrence, h.day);
            if (e.getX() > GUTTER) return "Double-clic : nouvel événement à " + Fmt.time(LocalTime.of(Math.min(23, e.getY() / HOUR), (e.getY() * 60 / HOUR) % 60 / 30 * 30));
            return null;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(400, HOUR * 24 + 1);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) {
            return HOUR / 2;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) {
            return orientation == SwingConstants.VERTICAL ? r.height - HOUR : r.width;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            if (first == null) return;
            Graphics2D g = Style.smooth(g0.create());
            int w = getWidth();
            int h = getHeight();
            double cw = colW(w);
            LocalDate today = ctl.today();
            g.setColor(Style.SURFACE);
            g.fillRect(0, 0, w, h);
            hits.clear();

            // fonds des colonnes
            for (int c = 0; c < count; c++) {
                LocalDate day = first.plusDays(c);
                int x = (int) Math.round(GUTTER + c * cw);
                int x2 = (int) Math.round(GUTTER + (c + 1) * cw);
                if (day.equals(today)) {
                    g.setColor(new Color(0xF7FAFF));
                    g.fillRect(x, 0, x2 - x, h);
                } else if (day.getDayOfWeek().getValue() >= 6) {
                    g.setColor(Style.WEEKEND);
                    g.fillRect(x, 0, x2 - x, h);
                }
            }

            // lignes des heures
            g.setFont(Style.font(Font.PLAIN, 11f));
            FontMetrics fm = g.getFontMetrics();
            for (int hr = 0; hr < 24; hr++) {
                int y = hr * HOUR;
                g.setColor(Style.GRID);
                g.drawLine(GUTTER - 6, y, w, y);
                g.setColor(Style.GRID_LIGHT);
                g.drawLine(GUTTER, y + HOUR / 2, w, y + HOUR / 2);
                if (hr > 0) {
                    String label = String.format("%02d:00", hr);
                    g.setColor(Style.MUTED);
                    g.drawString(label, GUTTER - 10 - fm.stringWidth(label), y + 4);
                }
            }
            g.setColor(Style.GRID);
            for (int c = 0; c <= count; c++) {
                int x = (int) Math.round(GUTTER + c * cw);
                g.drawLine(x, 0, x, h);
            }

            // événements
            Font titleFont = Style.font(Font.BOLD, 12f);
            Font smallFont = Style.font(Font.PLAIN, 11f);
            for (int c = 0; c < count; c++) {
                LocalDate day = first.plusDays(c);
                List<Occurrence> list = days.get(day);
                if (list == null) continue;
                double x0 = GUTTER + c * cw + 2;
                double width = cw - 6;
                for (DayLayout.Slot s : DayLayout.layout(list, day, 24)) {
                    int sm = s.startMinute(day);
                    int em = Math.max(s.endMinute(day), sm + 24);
                    double colWidth = width / s.columns;
                    int x = (int) Math.round(x0 + s.column * colWidth);
                    int y = sm * HOUR / 60;
                    int bw = (int) Math.round(colWidth) - 2;
                    int bh = Math.max(16, (em - sm) * HOUR / 60 - 2);
                    Rectangle r = new Rectangle(x, y + 1, bw, bh);
                    Occurrence o = s.occurrence;
                    hits.add(new Hit(r, o, day));
                    boolean hov = hover != null && hover.occurrence == o && day.equals(hover.day);
                    Color base = Style.color(o.event.category);
                    g.setColor(hov ? new Color(o.event.category.light(0.7)) : Style.soft(o.event.category));
                    Style.fillRound(g, r.x, r.y, r.width, r.height, 8);
                    g.setColor(Style.SURFACE);
                    g.setStroke(new BasicStroke(1f));
                    Style.drawRound(g, r.x - 0.5, r.y - 0.5, r.width + 1, r.height + 1, 8);
                    g.setColor(base);
                    Style.fillRound(g, r.x, r.y, 4, r.height, 4);
                    g.fillRect(r.x + 2, r.y, 2, r.height);

                    Graphics2D gc = (Graphics2D) g.create(r.x + 8, r.y, r.width - 10, r.height);
                    Color ink = Style.deep(o.event.category);
                    String time = Fmt.whenOn(o, day);
                    gc.setFont(titleFont);
                    FontMetrics tf = gc.getFontMetrics();
                    if (r.height < 34) {
                        // bloc court : titre et heure sur une ligne
                        gc.setColor(ink);
                        String t = o.event.displayTitle();
                        String line = Style.fit(t, tf, r.width - 12);
                        gc.drawString(line, 0, Math.min(r.height - 4, 13));
                        int used = tf.stringWidth(line) + 6;
                        gc.setFont(smallFont);
                        gc.setColor(Style.MUTED);
                        gc.drawString(Style.fit(Fmt.time(s.start), gc.getFontMetrics(), r.width - 12 - used), used, Math.min(r.height - 4, 13));
                    } else {
                        int yy = 15;
                        gc.setColor(ink);
                        gc.drawString(Style.fit(o.event.displayTitle(), tf, r.width - 12), 0, yy);
                        gc.setFont(smallFont);
                        FontMetrics sf = gc.getFontMetrics();
                        gc.setColor(Style.MUTED);
                        yy += 15;
                        gc.drawString(Style.fit(time, sf, r.width - 12), 0, yy);
                        if (!o.event.location.trim().isEmpty() && r.height > yy + 18) {
                            yy += 15;
                            gc.drawString(Style.fit(o.event.location, sf, r.width - 12), 0, yy);
                        }
                    }
                    gc.dispose();
                }
            }

            // heure actuelle
            int ti = (int) java.time.temporal.ChronoUnit.DAYS.between(first, today);
            if (ti >= 0 && ti < count) {
                LocalDateTime now = LocalDateTime.now();
                int y = (now.getHour() * 60 + now.getMinute()) * HOUR / 60;
                int x = (int) Math.round(GUTTER + ti * cw);
                int x2 = (int) Math.round(GUTTER + (ti + 1) * cw);
                g.setColor(Style.NOW);
                g.setStroke(new BasicStroke(2f));
                g.drawLine(x, y, x2, y);
                g.fillOval(x - 5, y - 5, 10, 10);
            }
            g.dispose();
        }
    }
}
