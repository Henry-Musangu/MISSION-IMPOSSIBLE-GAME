package agenda.desktop;

import agenda.Agenda;
import agenda.AgendaFile;
import agenda.Event;
import agenda.Fmt;
import agenda.Ics;
import agenda.Occurrence;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

/** La fenêtre principale de l'agenda. */
final class AgendaFrame extends JFrame implements Controller {

    enum Mode { DAY, WEEK, MONTH, LIST }

    private final Agenda agenda = new Agenda();
    private final File file;
    private Mode mode = Mode.MONTH;
    private LocalDate selected = LocalDate.now();
    private LocalDate today = LocalDate.now();
    private String query = "";
    private boolean saveFailed;

    private final MonthView monthView = new MonthView(this);
    private final TimeGridView weekView = new TimeGridView(this, 7);
    private final TimeGridView dayView = new TimeGridView(this, 1);
    private final OccurrenceList listView = new OccurrenceList(this, false);
    private final JScrollPane listScroll = new JScrollPane(listView);
    private final CardLayout cards = new CardLayout();
    private final JPanel center = new JPanel(cards);

    private final MiniMonth mini = new MiniMonth(this);
    private final JLabel sideTitle = new JLabel();
    private final JLabel sideSub = new JLabel();
    private final OccurrenceList sideList = new OccurrenceList(this, true);

    private final JLabel title = new JLabel();
    private final JTextField search = new HintField("Rechercher…", 16);
    private final Style.Segment[] segments = new Style.Segment[4];
    private final Notifier notifier;

    AgendaFrame(File file) {
        super("Agenda");
        this.file = file;
        setIconImages(Icons.all());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                quit();
            }
        });

        load();
        agenda.addListener(new Runnable() {
            @Override
            public void run() {
                save();
            }
        });

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Style.BG);
        root.add(toolbar(), BorderLayout.NORTH);
        root.add(centerPanel(), BorderLayout.CENTER);
        root.add(sidebar(), BorderLayout.EAST);
        setContentPane(root);
        setJMenuBar(menus());
        keys();

        setMinimumSize(new Dimension(900, 600));
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(1320, screen.width - 60), Math.min(860, screen.height - 80));
        setLocationRelativeTo(null);

        setMode(Mode.MONTH);
        notifier = new Notifier(this, agenda, this);
        notifier.start();

        // minuit, heure actuelle : rafraîchir régulièrement
        Timer clock = new Timer(30000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LocalDate now = LocalDate.now();
                if (!now.equals(today)) {
                    if (selected.equals(today)) selected = now;
                    today = now;
                    setIconImages(Icons.all());
                    refresh();
                } else {
                    weekView.repaint();
                    dayView.repaint();
                }
            }
        });
        clock.start();
    }

    // ================================================================== construction de l'interface

    private JComponent toolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Style.SURFACE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Style.BORDER),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        Style.FlatButton todayBtn = new Style.FlatButton("Aujourd'hui", Style.Kind.PLAIN);
        todayBtn.setToolTipText("Revenir à aujourd'hui (Ctrl+T)");
        todayBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goToday();
            }
        });
        Style.FlatButton prev = new Style.FlatButton("‹", Style.Kind.GHOST);
        Style.FlatButton next = new Style.FlatButton("›", Style.Kind.GHOST);
        prev.setFont(Style.font(Font.BOLD, 20f));
        next.setFont(Style.font(Font.BOLD, 20f));
        prev.setBorder(BorderFactory.createEmptyBorder(0, 10, 3, 10));
        next.setBorder(BorderFactory.createEmptyBorder(0, 10, 3, 10));
        prev.setToolTipText("Précédent (Page préc.)");
        next.setToolTipText("Suivant (Page suiv.)");
        prev.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigate(-1);
            }
        });
        next.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigate(1);
            }
        });
        title.setFont(Style.font(Font.BOLD, 20f));
        title.setForeground(Style.TEXT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        left.add(todayBtn);
        left.add(prev);
        left.add(next);
        left.add(title);
        bar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JPanel group = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)) {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = Style.smooth(g.create());
                g2.setColor(new Color(0xEEF1F5));
                Style.fillRound(g2, 0, 0, getWidth(), getHeight(), 12);
                g2.dispose();
            }
        };
        group.setOpaque(false);
        group.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        ButtonGroup bg = new ButtonGroup();
        String[] names = {"Jour", "Semaine", "Mois", "Liste"};
        for (int i = 0; i < 4; i++) {
            final Mode m = Mode.values()[i];
            Style.Segment s = new Style.Segment(names[i]);
            s.setToolTipText(names[i] + " (Ctrl+" + (i + 1) + ")");
            s.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearSearch();
                    setMode(m);
                }
            });
            bg.add(s);
            group.add(s);
            segments[i] = s;
        }
        right.add(group);

        search.setToolTipText("Rechercher un événement (Ctrl+F) — Échap pour effacer");
        search.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Style.BORDER, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        search.setFont(Style.font(Font.PLAIN, 13f));
        final Timer debounce = new Timer(220, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runSearch();
            }
        });
        debounce.setRepeats(false);
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                debounce.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                debounce.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                debounce.restart();
            }
        });
        search.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clear");
        search.getActionMap().put("clear", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearSearch();
                setMode(mode);
                center.requestFocusInWindow();
            }
        });
        search.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                search.repaint();
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                search.repaint();
            }
        });
        right.add(search);

        Style.FlatButton add = new Style.FlatButton("+  Nouvel événement", Style.Kind.ACCENT);
        add.setToolTipText("Créer un événement (Ctrl+N)");
        add.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                create(Desk.defaultStart(selected, today), false);
            }
        });
        right.add(add);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    /** Champ de texte qui affiche un texte d'aide quand il est vide. */
    private static final class HintField extends JTextField {
        private final String hint;

        HintField(String hint, int columns) {
            super(columns);
            this.hint = hint;
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !isFocusOwner()) {
                java.awt.Graphics2D g2 = Style.smooth(g.create());
                g2.setColor(Style.FAINT);
                g2.setFont(getFont());
                java.awt.Insets in = getInsets();
                g2.drawString(hint, in.left, in.top + g2.getFontMetrics().getAscent());
                g2.dispose();
            }
        }
    }

    private JComponent centerPanel() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 0));
        listScroll.setBorder(BorderFactory.createEmptyBorder());
        listScroll.getVerticalScrollBar().setUnitIncrement(24);
        listScroll.getViewport().setBackground(Style.BG);
        center.add(dayView, Mode.DAY.name());
        center.add(weekView, Mode.WEEK.name());
        center.add(monthView, Mode.MONTH.name());
        center.add(listScroll, Mode.LIST.name());
        center.setBorder(BorderFactory.createLineBorder(Style.BORDER));
        center.setFocusable(true);
        monthView.setFocusable(true);
        wrap.add(center, BorderLayout.CENTER);
        return wrap;
    }

    private JComponent sidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setPreferredSize(new Dimension(300, 100));
        side.setOpaque(false);
        side.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Style.SURFACE);
        card.setBorder(BorderFactory.createLineBorder(Style.BORDER));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        mini.setAlignmentX(0);
        mini.setBorder(BorderFactory.createEmptyBorder(8, 6, 0, 6));
        top.add(mini);
        JPanel sep = new JPanel();
        sep.setBackground(Style.BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setPreferredSize(new Dimension(10, 1));
        sep.setAlignmentX(0);
        top.add(Box.createVerticalStrut(6));
        top.add(sep);
        sideSub.setFont(Style.font(Font.BOLD, 11.5f));
        sideSub.setForeground(Style.ACCENT);
        sideSub.setBorder(BorderFactory.createEmptyBorder(12, 14, 0, 14));
        sideSub.setAlignmentX(0);
        sideTitle.setFont(Style.font(Font.BOLD, 16f));
        sideTitle.setForeground(Style.TEXT);
        sideTitle.setBorder(BorderFactory.createEmptyBorder(2, 14, 4, 14));
        sideTitle.setAlignmentX(0);
        top.add(sideSub);
        top.add(sideTitle);
        card.add(top, BorderLayout.NORTH);

        JScrollPane sc = new JScrollPane(sideList);
        sc.setBorder(BorderFactory.createEmptyBorder());
        sc.getVerticalScrollBar().setUnitIncrement(24);
        sc.getViewport().setBackground(Style.SURFACE);
        card.add(sc, BorderLayout.CENTER);

        Style.FlatButton add = new Style.FlatButton("+  Ajouter ce jour-là", Style.Kind.PLAIN);
        add.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                create(Desk.defaultStart(selected, today), false);
            }
        });
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));
        bottom.add(add);
        card.add(bottom, BorderLayout.SOUTH);
        side.add(card);
        return side;
    }

    private JMenuBar menus() {
        JMenuBar bar = new JMenuBar();
        int ctrl = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

        JMenu file = new JMenu("Fichier");
        file.add(item("Nouvel événement", KeyStroke.getKeyStroke(KeyEvent.VK_N, ctrl), new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                create(Desk.defaultStart(selected, today), false);
            }
        }));
        file.addSeparator();
        file.add(item("Importer un fichier .ics…", KeyStroke.getKeyStroke(KeyEvent.VK_I, ctrl), new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                importIcs();
            }
        }));
        file.add(item("Exporter l'agenda (.ics)…", KeyStroke.getKeyStroke(KeyEvent.VK_E, ctrl), new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportIcs();
            }
        }));
        file.addSeparator();
        file.add(item("Quitter", KeyStroke.getKeyStroke(KeyEvent.VK_Q, ctrl), new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                quit();
            }
        }));
        bar.add(file);

        JMenu view = new JMenu("Affichage");
        String[] names = {"Jour", "Semaine", "Mois", "Liste"};
        int[] keys = {KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4};
        for (int i = 0; i < 4; i++) {
            final Mode m = Mode.values()[i];
            view.add(item(names[i], KeyStroke.getKeyStroke(keys[i], ctrl), new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearSearch();
                    setMode(m);
                }
            }));
        }
        view.addSeparator();
        view.add(item("Aujourd'hui", KeyStroke.getKeyStroke(KeyEvent.VK_T, ctrl), new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goToday();
            }
        }));
        view.add(item("Précédent", KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigate(-1);
            }
        }));
        view.add(item("Suivant", KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigate(1);
            }
        }));
        view.add(item("Rechercher", KeyStroke.getKeyStroke(KeyEvent.VK_F, ctrl), new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                search.requestFocusInWindow();
                search.selectAll();
            }
        }));
        bar.add(view);

        JMenu help = new JMenu("Aide");
        help.add(item("Raccourcis et fichier de l'agenda", null, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                about();
            }
        }));
        bar.add(help);
        return bar;
    }

    private static JMenuItem item(String text, KeyStroke key, ActionListener action) {
        JMenuItem it = new JMenuItem(text);
        if (key != null) it.setAccelerator(key);
        it.addActionListener(action);
        return it;
    }

    /** Flèches : changer de jour ; Entrée : nouvel événement ce jour-là. */
    private void keys() {
        bind(KeyEvent.VK_LEFT, -1);
        bind(KeyEvent.VK_RIGHT, 1);
        bind(KeyEvent.VK_UP, -7);
        bind(KeyEvent.VK_DOWN, 7);
        center.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "new");
        center.getActionMap().put("new", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                create(Desk.defaultStart(selected, today), false);
            }
        });
    }

    private void bind(int key, final int days) {
        String name = "move" + days;
        center.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(key, 0), name);
        center.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mode == Mode.MONTH || mode == Mode.WEEK || (mode == Mode.DAY && Math.abs(days) == 1)) {
                    select(selected.plusDays(days));
                }
            }
        });
    }

    // ================================================================== navigation

    private void setMode(Mode m) {
        mode = m;
        segments[m.ordinal()].setSelected(true);
        cards.show(center, m.name());
        refresh();
        center.requestFocusInWindow();
    }

    private void goToday() {
        today = LocalDate.now();
        selected = today;
        refresh();
        if (mode == Mode.DAY) dayView.scrollTo(java.time.LocalTime.now());
        if (mode == Mode.WEEK) weekView.scrollTo(java.time.LocalTime.now());
    }

    private void navigate(int dir) {
        switch (mode) {
            case DAY:
                selected = selected.plusDays(dir);
                break;
            case WEEK:
                selected = selected.plusWeeks(dir);
                break;
            case MONTH: {
                YearMonth ym = YearMonth.from(selected).plusMonths(dir);
                selected = ym.atDay(Math.min(selected.getDayOfMonth(), ym.lengthOfMonth()));
                break;
            }
            default:
                selected = selected.plusMonths(dir);
        }
        refresh();
    }

    void refresh() {
        boolean searching = !query.isEmpty();
        if (searching) {
            title.setText("Recherche");
            cards.show(center, Mode.LIST.name());
            listView.setRows(searchRows());
        } else {
            switch (mode) {
                case DAY:
                    dayView.show(selected);
                    title.setText(dayView.title());
                    break;
                case WEEK:
                    weekView.show(selected);
                    title.setText(weekView.title());
                    break;
                case MONTH:
                    monthView.show(selected);
                    title.setText(monthView.title());
                    break;
                default:
                    listView.setRows(upcomingRows());
                    title.setText("À partir du " + Fmt.dayMonthYear(selected));
            }
        }
        mini.show(selected);
        String rel = Fmt.relativeDay(selected, today);
        sideSub.setText(rel == null ? " " : rel.toUpperCase(java.util.Locale.FRENCH));
        sideTitle.setText(Fmt.capitalize(Fmt.longDate(selected)));
        List<OccurrenceList.Row> rows = new ArrayList<OccurrenceList.Row>();
        List<Occurrence> day = agenda.on(selected);
        for (Occurrence o : day) rows.add(OccurrenceList.Row.item(o, selected));
        if (day.isEmpty()) rows.add(OccurrenceList.Row.message("Rien de prévu ce jour-là."));
        sideList.setRows(rows);
        setTitle("Agenda — " + title.getText());
    }

    private List<OccurrenceList.Row> upcomingRows() {
        List<OccurrenceList.Row> rows = new ArrayList<OccurrenceList.Row>();
        Map<LocalDate, List<Occurrence>> map = agenda.byDay(selected, 62);
        for (Map.Entry<LocalDate, List<Occurrence>> en : map.entrySet()) {
            if (en.getValue().isEmpty()) continue;
            rows.add(OccurrenceList.Row.heading(Fmt.dayHeading(en.getKey(), today), en.getKey().equals(today)));
            for (Occurrence o : en.getValue()) rows.add(OccurrenceList.Row.item(o, en.getKey()));
        }
        if (rows.isEmpty()) {
            rows.add(OccurrenceList.Row.heading("Les deux prochains mois", false));
            rows.add(OccurrenceList.Row.message("Rien de prévu. Cliquez sur « + Nouvel événement » pour commencer."));
        }
        return rows;
    }

    private List<OccurrenceList.Row> searchRows() {
        List<OccurrenceList.Row> rows = new ArrayList<OccurrenceList.Row>();
        List<Occurrence> found = agenda.search(query, LocalDateTime.now());
        LocalDateTime now = LocalDateTime.now();
        boolean pastHeader = false;
        rows.add(OccurrenceList.Row.heading(found.isEmpty() ? "Aucun résultat pour « " + query + " »"
                : Fmt.plural(found.size(), "résultat") + " pour « " + query + " »", false));
        for (Occurrence o : found) {
            boolean past = o.end.isBefore(now);
            if (past && !pastHeader) {
                rows.add(OccurrenceList.Row.heading("Passés", false));
                pastHeader = true;
            }
            rows.add(OccurrenceList.Row.item(o, null));
        }
        return rows;
    }

    private void runSearch() {
        String q = search.getText().trim();
        if (q.equals(query)) return;
        query = q;
        refresh();
        if (q.isEmpty()) setMode(mode);
    }

    private void clearSearch() {
        if (!search.getText().isEmpty()) search.setText("");
        query = "";
    }

    // ================================================================== Controller

    @Override
    public Agenda agenda() {
        return agenda;
    }

    @Override
    public LocalDate today() {
        return today;
    }

    @Override
    public LocalDate selected() {
        return selected;
    }

    @Override
    public void select(LocalDate day) {
        selected = day;
        refresh();
    }

    @Override
    public void showDay(LocalDate day) {
        selected = day;
        clearSearch();
        setMode(Mode.DAY);
    }

    @Override
    public void create(LocalDateTime start, boolean allDay) {
        Event fresh = EventDialog.fresh(start, allDay);
        EventDialog d = new EventDialog(this, fresh, true);
        if (d.showDialog() == EventDialog.Action.SAVE) {
            Event e = d.result();
            agenda.add(e);
            selected = e.start.toLocalDate();
            refresh();
        }
    }

    @Override
    public void open(Occurrence o) {
        Event original = o.event;
        Event copy = original.copy();
        copy.start = o.start;
        copy.end = o.end;
        EventDialog d = new EventDialog(this, copy, false);
        EventDialog.Action a = d.showDialog();
        if (a == EventDialog.Action.SAVE) {
            Event edited = d.result();
            Agenda.Scope scope = Agenda.Scope.ALL;
            if (original.isRecurring()) {
                scope = askScope("Modifier l'événement répété", "Appliquer la modification à :");
                if (scope == null) return;
            }
            agenda.applyEdit(original, o.date(), edited, scope);
            refresh();
        } else if (a == EventDialog.Action.DELETE) {
            Agenda.Scope scope = Agenda.Scope.ALL;
            if (original.isRecurring()) {
                scope = askScope("Supprimer l'événement répété", "Supprimer :");
                if (scope == null) return;
            } else {
                int ok = JOptionPane.showConfirmDialog(this, "Supprimer « " + original.displayTitle() + " » ?",
                        "Supprimer", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                if (ok != JOptionPane.OK_OPTION) return;
            }
            agenda.delete(original, o.date(), scope);
            refresh();
        }
    }

    private Agenda.Scope askScope(String dialogTitle, String question) {
        Object[] options = {"Cet événement", "Celui-ci et les suivants", "Tous les événements", "Annuler"};
        int r = JOptionPane.showOptionDialog(this, question, dialogTitle, JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        switch (r) {
            case 0: return Agenda.Scope.THIS;
            case 1: return Agenda.Scope.FOLLOWING;
            case 2: return Agenda.Scope.ALL;
            default: return null;
        }
    }

    // ================================================================== fichiers

    private void load() {
        try {
            agenda.setAll(AgendaFile.load(file));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Impossible de lire l'agenda :\n" + file + "\n\n" + e.getMessage(),
                    "Agenda", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void save() {
        try {
            AgendaFile.save(file, agenda.events());
            saveFailed = false;
        } catch (IOException e) {
            if (!saveFailed) {
                saveFailed = true;
                JOptionPane.showMessageDialog(this, "Impossible d'enregistrer l'agenda :\n" + file + "\n\n" + e.getMessage(),
                        "Agenda", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JFileChooser chooser() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Calendrier iCalendar (*.ics)", "ics"));
        return fc;
    }

    private void importIcs() {
        JFileChooser fc = chooser();
        fc.setDialogTitle("Importer un calendrier");
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        try {
            List<Event> events;
            Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8);
            try {
                events = Ics.read(r);
            } finally {
                r.close();
            }
            if (events.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Aucun événement trouvé dans ce fichier.", "Importer", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            int ok = JOptionPane.showConfirmDialog(this, "Importer " + Fmt.plural(events.size(), "événement") + " de « " + f.getName() + " » ?",
                    "Importer", JOptionPane.OK_CANCEL_OPTION);
            if (ok != JOptionPane.OK_OPTION) return;
            int n = agenda.importAll(events);
            refresh();
            JOptionPane.showMessageDialog(this, Fmt.plural(n, "événement") + " importé" + (n > 1 ? "s" : "") + ".", "Importer", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Lecture impossible : " + e.getMessage(), "Importer", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportIcs() {
        JFileChooser fc = chooser();
        fc.setDialogTitle("Exporter l'agenda");
        fc.setSelectedFile(new File("agenda.ics"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".ics")) f = new File(f.getPath() + ".ics");
        if (f.exists()) {
            int ok = JOptionPane.showConfirmDialog(this, "« " + f.getName() + " » existe déjà. Le remplacer ?", "Exporter", JOptionPane.OK_CANCEL_OPTION);
            if (ok != JOptionPane.OK_OPTION) return;
        }
        try {
            Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8);
            try {
                Ics.write(agenda.events(), w);
            } finally {
                w.close();
            }
            JOptionPane.showMessageDialog(this, Fmt.plural(agenda.size(), "événement") + " exporté" + (agenda.size() > 1 ? "s" : "")
                    + " dans\n" + f.getPath() + "\n\nCe fichier peut être importé dans Google Agenda, Outlook, Apple Calendrier…",
                    "Exporter", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Écriture impossible : " + e.getMessage(), "Exporter", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void about() {
        String text = "<html><body style='width:380px'>"
                + "<h2 style='margin:0'>Agenda</h2>"
                + "<p>Votre calendrier personnel. Les événements sont enregistrés automatiquement dans :</p>"
                + "<p><tt>" + Desk.html(file.getAbsolutePath()) + "</tt></p>"
                + "<p>C'est un fichier iCalendar standard : vous pouvez le sauvegarder ou l'ouvrir dans un autre agenda.</p>"
                + "<p><b>Raccourcis</b><br>"
                + "Ctrl+N : nouvel événement · Ctrl+F : rechercher<br>"
                + "Ctrl+1 / 2 / 3 / 4 : jour, semaine, mois, liste<br>"
                + "Ctrl+T : aujourd'hui · Page préc. / suiv. : période précédente / suivante<br>"
                + "Flèches : changer de jour · Entrée : nouvel événement ce jour-là<br>"
                + "Double-clic sur un jour ou une heure : nouvel événement</p>"
                + "<p>Les rappels s'affichent tant que l'agenda est ouvert.</p></body></html>";
        JOptionPane.showMessageDialog(this, text, "Agenda", JOptionPane.INFORMATION_MESSAGE, new javax.swing.ImageIcon(Icons.calendar(64)));
    }

    private void quit() {
        notifier.dispose();
        dispose();
        System.exit(0);
    }

    /** Lancement depuis {@link Main}. */
    static void launch(final File file) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new AgendaFrame(file).setVisible(true);
            }
        });
    }
}
