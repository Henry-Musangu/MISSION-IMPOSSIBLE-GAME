package agenda.desktop;

import agenda.Agenda;
import agenda.Fmt;
import agenda.Reminder;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Les rappels sur PC : vérifiés toutes les 15 secondes tant que l'agenda est ouvert.
 * Une petite fenêtre s'affiche en bas à droite de l'écran (et une notification du système si possible).
 */
final class Notifier {

    private final JFrame frame;
    private final Agenda agenda;
    private final Controller ctl;
    private LocalDateTime lastCheck;
    private final Set<String> shown = new HashSet<String>();
    private TrayIcon tray;

    Notifier(JFrame frame, Agenda agenda, Controller ctl) {
        this.frame = frame;
        this.agenda = agenda;
        this.ctl = ctl;
    }

    void start() {
        lastCheck = LocalDateTime.now();
        installTray();
        Timer timer = new Timer(15000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                check();
            }
        });
        timer.setInitialDelay(2000);
        timer.start();
    }

    private void installTray() {
        try {
            if (!SystemTray.isSupported()) return;
            tray = new TrayIcon(Icons.calendar(32), "Agenda");
            tray.setImageAutoSize(true);
            tray.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    frame.setVisible(true);
                    frame.setState(JFrame.NORMAL);
                    frame.toFront();
                }
            });
            SystemTray.getSystemTray().add(tray);
        } catch (Exception | Error e) {
            tray = null;
        }
    }

    void check() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(lastCheck)) lastCheck = now;   // l'horloge a reculé
        List<Reminder> due = agenda.remindersBetween(lastCheck, now);
        lastCheck = now;
        for (Reminder r : due) {
            if (!r.occurrence.end.isAfter(now) && !r.occurrence.start.equals(r.occurrence.end)) continue;   // déjà fini
            if (!shown.add(r.key())) continue;
            notify(r);
        }
    }

    private void notify(final Reminder r) {
        String title = r.occurrence.event.displayTitle();
        String when = describe(r);
        Toolkit.getDefaultToolkit().beep();
        if (tray != null) {
            try {
                tray.displayMessage(title, when, TrayIcon.MessageType.INFO);
            } catch (RuntimeException ignored) {
                // pas de notification système : la fenêtre suffit
            }
        }
        popup(r, title, when);
    }

    private String describe(Reminder r) {
        LocalDate today = LocalDate.now();
        LocalDate d = r.occurrence.date();
        String rel = Fmt.relativeDay(d, today);
        String day = rel != null ? rel : Fmt.capitalize(Fmt.longDateNoYear(d, today));
        String s = day + " · " + Fmt.when(r.occurrence);
        if (!r.occurrence.event.location.trim().isEmpty()) s += " · " + r.occurrence.event.location.trim();
        return s;
    }

    private void popup(final Reminder r, String title, String when) {
        final JDialog d = new JDialog(frame, "Rappel", false);
        d.setAlwaysOnTop(true);
        d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JPanel p = new JPanel(new BorderLayout(14, 0));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 6, 0, 0, Style.color(r.occurrence.event.category)),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(title);
        t.setFont(Style.font(Font.BOLD, 15f));
        JLabel w = new JLabel(when);
        w.setForeground(Style.MUTED);
        JLabel rem = new JLabel(Fmt.reminder(r.occurrence.event.reminder).replace(" avant", " avant le début").replace("Au début de l'événement", "C'est maintenant"));
        rem.setForeground(Style.FAINT);
        text.add(t);
        text.add(javax.swing.Box.createVerticalStrut(4));
        text.add(w);
        text.add(javax.swing.Box.createVerticalStrut(2));
        text.add(rem);
        p.add(text, BorderLayout.CENTER);
        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
        Style.FlatButton open = new Style.FlatButton("Voir", Style.Kind.ACCENT);
        Style.FlatButton ok = new Style.FlatButton("OK", Style.Kind.PLAIN);
        buttons.add(open);
        buttons.add(javax.swing.Box.createVerticalStrut(6));
        buttons.add(ok);
        p.add(buttons, BorderLayout.EAST);
        d.setContentPane(p);
        open.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                d.dispose();
                frame.setVisible(true);
                frame.setState(JFrame.NORMAL);
                frame.toFront();
                ctl.select(r.occurrence.date());
                ctl.open(r.occurrence);
            }
        });
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                d.dispose();
            }
        });
        d.pack();
        d.setSize(Math.max(380, d.getWidth()), d.getHeight());
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int stack = 0;
        for (java.awt.Window win : frame.getOwnedWindows()) {
            if (win != d && win.isVisible() && win instanceof JDialog && "Rappel".equals(((JDialog) win).getTitle())) stack++;
        }
        d.setLocation(screen.x + screen.width - d.getWidth() - 16, screen.y + screen.height - (d.getHeight() + 12) * (stack + 1) - 4);
        d.setVisible(true);
    }

    void dispose() {
        if (tray != null) {
            try {
                SystemTray.getSystemTray().remove(tray);
            } catch (RuntimeException ignored) {
                // rien
            }
        }
    }
}
