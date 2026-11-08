package agenda.desktop;

import agenda.Category;
import agenda.Event;
import agenda.Fmt;
import agenda.Recurrence;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Formulaire de création / modification d'un événement. */
final class EventDialog extends JDialog {

    /** Ce que l'utilisateur a choisi. */
    enum Action { CANCEL, SAVE, DELETE }

    private static final String[] REPEAT = {"Ne se répète pas", "Tous les jours", "Toutes les semaines", "Tous les mois", "Tous les ans"};
    private static final String[] UNITS = {"", "jour(s)", "semaine(s)", "mois", "an(s)"};
    private static final String[] ENDS = {"Sans fin", "Jusqu'au", "Nombre de fois"};

    private final Event base;
    private Action action = Action.CANCEL;
    private Event result;
    private boolean adjusting;

    private final JTextField title = new JTextField(28);
    private final JTextField location = new JTextField(28);
    private final JComboBox<Category> category = new JComboBox<Category>(Category.values());
    private final JCheckBox allDay = new JCheckBox("Toute la journée");
    private final JSpinner startDate = dateSpinner();
    private final JSpinner startTime = timeSpinner();
    private final JSpinner endDate = dateSpinner();
    private final JSpinner endTime = timeSpinner();
    private final JComboBox<String> repeat = new JComboBox<String>(REPEAT);
    private final JSpinner interval = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
    private final JLabel unit = new JLabel();
    private final JToggleButton[] weekdays = new JToggleButton[7];
    private final JComboBox<String> monthMode = new JComboBox<String>();
    private final JComboBox<String> endMode = new JComboBox<String>(ENDS);
    private final JSpinner until = dateSpinner();
    private final JSpinner count = new JSpinner(new SpinnerNumberModel(10, 1, 999, 1));
    private final JLabel summary = new JLabel();
    private final JComboBox<String> reminder = new JComboBox<String>();
    private final JTextArea notes = new JTextArea(4, 28);
    private final JPanel repeatDetails = new JPanel(new GridBagLayout());
    private final JPanel weekPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
    private final JPanel endPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));

    /**
     * @param event  l'événement à modifier (une copie, avec les dates de l'occurrence choisie), ou un nouvel événement
     * @param isNew  vrai pour une création
     */
    EventDialog(Window owner, Event event, boolean isNew) {
        super(owner, isNew ? "Nouvel événement" : "Modifier l'événement", ModalityType.APPLICATION_MODAL);
        this.base = event;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(16, 18, 8, 18));
        int row = 0;

        title.setFont(Style.font(Font.BOLD, 15f));
        addRow(form, row++, "Titre", title);
        addRow(form, row++, "Lieu", location);

        category.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel, boolean focus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, sel, focus);
                Category c = (Category) value;
                if (c != null) {
                    l.setText(c.label);
                    l.setIcon(Style.dot(Style.color(c), 12));
                    l.setIconTextGap(8);
                }
                return l;
            }
        });
        addRow(form, row++, "Catégorie", category);

        addRow(form, row++, "", allDay);
        addRow(form, row++, "Début", line(startDate, startTime));
        addRow(form, row++, "Fin", line(endDate, endTime));

        // répétition
        addRow(form, row++, "Répétition", repeat);
        buildRepeatDetails();
        addRow(form, row++, "", repeatDetails);

        for (int m : Fmt.REMINDERS) reminder.addItem(Fmt.reminder(m));
        addRow(form, row++, "Rappel", reminder);

        notes.setLineWrap(true);
        notes.setWrapStyleWord(true);
        notes.setFont(location.getFont());
        JScrollPane notesScroll = new JScrollPane(notes);
        addRow(form, row++, "Notes", notesScroll);

        // boutons
        JPanel buttons = new JPanel(new BorderLayout());
        buttons.setBorder(BorderFactory.createEmptyBorder(8, 18, 16, 18));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        Style.FlatButton cancel = new Style.FlatButton("Annuler", Style.Kind.PLAIN);
        Style.FlatButton save = new Style.FlatButton("Enregistrer", Style.Kind.ACCENT);
        right.add(cancel);
        right.add(save);
        buttons.add(right, BorderLayout.EAST);
        if (!isNew) {
            Style.FlatButton delete = new Style.FlatButton("Supprimer", Style.Kind.DANGER);
            buttons.add(delete, BorderLayout.WEST);
            delete.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    action = Action.DELETE;
                    dispose();
                }
            });
        }
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        save.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                save();
            }
        });
        getRootPane().setDefaultButton(save);
        JComponent root = (JComponent) getContentPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
        root.getActionMap().put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, java.awt.event.InputEvent.CTRL_DOWN_MASK), "save");
        root.getActionMap().put("save", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                save();
            }
        });

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);

        load(event);
        wire();
        updateVisibility();
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(owner);
        title.requestFocusInWindow();
    }

    /** Affiche le formulaire (bloquant). */
    Action showDialog() {
        setVisible(true);
        return action;
    }

    /** L'événement modifié (après SAVE). */
    Event result() {
        return result;
    }

    // ------------------------------------------------------------------ construction

    private static void addRow(JPanel form, int row, String label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(5, 0, 5, 12);
        c.anchor = GridBagConstraints.NORTHWEST;
        if (!label.isEmpty()) {
            JLabel l = new JLabel(label);
            l.setForeground(Style.MUTED);
            l.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
            c.gridx = 0;
            form.add(l, c);
        }
        c.gridx = 1;
        c.insets = new Insets(5, 0, 5, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        if (field instanceof JScrollPane) {
            c.fill = GridBagConstraints.BOTH;
            c.weighty = 1;
        }
        form.add(field, c);
    }

    private static JPanel line(JComponent... parts) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) p.add(Box.createHorizontalStrut(8));
            p.add(parts[i]);
        }
        return p;
    }

    private static JSpinner dateSpinner() {
        JSpinner s = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH));
        s.setEditor(new JSpinner.DateEditor(s, "EEE dd/MM/yyyy"));
        return s;
    }

    private static JSpinner timeSpinner() {
        SpinnerDateModel model = new SpinnerDateModel(new Date(), null, null, Calendar.MINUTE) {
            @Override
            public Object getNextValue() {
                return step(15);
            }

            @Override
            public Object getPreviousValue() {
                return step(-15);
            }

            private Object step(int minutes) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(getDate());
                int m = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
                int next = minutes > 0 ? (m / 15 + 1) * 15 : ((m + 14) / 15 - 1) * 15;
                next = ((next % 1440) + 1440) % 1440;
                cal.set(Calendar.HOUR_OF_DAY, next / 60);
                cal.set(Calendar.MINUTE, next % 60);
                return cal.getTime();
            }
        };
        JSpinner s = new JSpinner(model);
        s.setEditor(new JSpinner.DateEditor(s, "HH:mm"));
        return s;
    }

    private void buildRepeatDetails() {
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 6, 0);
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        JPanel every = line(new JLabel("Tous les"), interval, unit);
        repeatDetails.add(every, c);

        String[] letters = Fmt.DAYS_LETTER;
        for (int i = 0; i < 7; i++) {
            final JToggleButton b = new JToggleButton(letters[i]);
            b.setToolTipText(Fmt.capitalize(Fmt.DAYS[i]));
            b.setMargin(new Insets(2, 6, 2, 6));
            b.setFocusPainted(false);
            weekdays[i] = b;
            weekPanel.add(b);
        }
        c.gridy = 1;
        repeatDetails.add(weekPanel, c);
        c.gridy = 2;
        repeatDetails.add(monthMode, c);
        endPanel.add(endMode);
        endPanel.add(until);
        endPanel.add(count);
        c.gridy = 3;
        repeatDetails.add(endPanel, c);
        summary.setFont(Style.font(Font.ITALIC, 12f));
        summary.setForeground(Style.MUTED);
        c.gridy = 4;
        repeatDetails.add(summary, c);
    }

    // ------------------------------------------------------------------ données

    private static Date toDate(LocalDateTime t) {
        return Date.from(t.atZone(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDateTime fromDate(Object d) {
        return LocalDateTime.ofInstant(((Date) d).toInstant(), ZoneId.systemDefault());
    }

    private LocalDate startDay() {
        return fromDate(startDate.getValue()).toLocalDate();
    }

    private LocalDateTime start() {
        LocalDate d = startDay();
        return allDay.isSelected() ? d.atStartOfDay() : d.atTime(fromDate(startTime.getValue()).toLocalTime().withSecond(0).withNano(0));
    }

    private LocalDateTime end() {
        LocalDate d = fromDate(endDate.getValue()).toLocalDate();
        return allDay.isSelected() ? d.plusDays(1).atStartOfDay() : d.atTime(fromDate(endTime.getValue()).toLocalTime().withSecond(0).withNano(0));
    }

    private void setStart(LocalDateTime t) {
        startDate.setValue(toDate(t));
        startTime.setValue(toDate(t));
    }

    private void setEnd(LocalDateTime t) {
        endDate.setValue(toDate(t));
        endTime.setValue(toDate(t));
    }

    private void load(Event e) {
        adjusting = true;
        title.setText(e.title);
        location.setText(e.location);
        category.setSelectedItem(e.category);
        allDay.setSelected(e.allDay);
        setStart(e.start);
        if (e.allDay) {
            LocalDate last = e.end.toLocalDate().minusDays(1);
            if (last.isBefore(e.start.toLocalDate())) last = e.start.toLocalDate();
            setEnd(last.atStartOfDay());
            startTime.setValue(toDate(e.start.toLocalDate().atTime(9, 0)));
            endTime.setValue(toDate(e.start.toLocalDate().atTime(10, 0)));
        } else {
            setEnd(e.end);
        }
        Recurrence r = e.recurrence;
        repeat.setSelectedIndex(r == null ? 0 : r.freq.ordinal() + 1);
        interval.setValue(r == null ? 1 : Math.max(1, r.interval));
        LocalDate start = e.start.toLocalDate();
        for (int i = 0; i < 7; i++) {
            DayOfWeek d = DayOfWeek.of(i + 1);
            boolean on = r != null && r.freq == Recurrence.Freq.WEEKLY && !r.days.isEmpty() ? r.days.contains(d) : d == start.getDayOfWeek();
            weekdays[i].setSelected(on);
        }
        fillMonthModes();
        if (r != null && r.freq == Recurrence.Freq.MONTHLY && r.weekOfMonth != 0) {
            monthMode.setSelectedIndex(r.weekOfMonth < 0 ? monthMode.getItemCount() - 1 : Math.min(1, monthMode.getItemCount() - 1));
        }
        if (r != null && r.until != null) {
            endMode.setSelectedIndex(1);
            until.setValue(toDate(r.until.atStartOfDay()));
        } else if (r != null && r.count > 0) {
            endMode.setSelectedIndex(2);
            count.setValue(r.count);
            until.setValue(toDate(start.plusMonths(3).atStartOfDay()));
        } else {
            endMode.setSelectedIndex(0);
            until.setValue(toDate(start.plusMonths(3).atStartOfDay()));
        }
        int idx = 0;
        for (int i = 0; i < Fmt.REMINDERS.length; i++) {
            if (Fmt.REMINDERS[i] == e.reminder) idx = i;
        }
        reminder.setSelectedIndex(idx);
        notes.setText(e.notes);
        adjusting = false;
    }

    /** Propose « le 10 », « le 2e mardi », et « le dernier mardi » si c'est le dernier du mois. */
    private void fillMonthModes() {
        LocalDate d = startDay();
        int selected = Math.max(0, monthMode.getSelectedIndex());
        boolean wasLast = monthMode.getItemCount() > 0 && selected == monthMode.getItemCount() - 1 && selected > 0
                && String.valueOf(monthMode.getItemAt(selected)).contains("dernier");
        monthMode.removeAllItems();
        String day = Fmt.DAYS[d.getDayOfWeek().getValue() - 1];
        monthMode.addItem("Le " + (d.getDayOfMonth() == 1 ? "1er" : String.valueOf(d.getDayOfMonth())) + " de chaque mois");
        int n = Recurrence.weekOfMonthOf(d);
        if (n <= 4) monthMode.addItem("Le " + Fmt.rank(n) + " " + day + " de chaque mois");
        if (Recurrence.isLastOfMonth(d)) monthMode.addItem("Le dernier " + day + " de chaque mois");
        monthMode.setSelectedIndex(wasLast ? monthMode.getItemCount() - 1 : Math.min(selected, monthMode.getItemCount() - 1));
    }

    private Recurrence recurrence() {
        int r = repeat.getSelectedIndex();
        if (r <= 0) return null;
        Recurrence rec = new Recurrence(Recurrence.Freq.values()[r - 1]);
        rec.interval = (Integer) interval.getValue();
        LocalDate start = startDay();
        if (rec.freq == Recurrence.Freq.WEEKLY) {
            for (int i = 0; i < 7; i++) {
                if (weekdays[i].isSelected()) rec.days.add(DayOfWeek.of(i + 1));
            }
            if (rec.days.size() == 1 && rec.days.contains(start.getDayOfWeek())) rec.days.clear();
        }
        if (rec.freq == Recurrence.Freq.MONTHLY && monthMode.getSelectedIndex() > 0) {
            String item = String.valueOf(monthMode.getSelectedItem());
            rec.weekOfMonth = item.contains("dernier") ? -1 : Recurrence.weekOfMonthOf(start);
        }
        if (endMode.getSelectedIndex() == 1) {
            rec.until = fromDate(until.getValue()).toLocalDate();
        } else if (endMode.getSelectedIndex() == 2) {
            rec.count = (Integer) count.getValue();
        }
        return rec;
    }

    private void wire() {
        final LocalDateTime[] lastStart = {start()};
        ChangeListener startMoved = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (adjusting) return;
                adjusting = true;
                // on garde la même durée en déplaçant la fin
                LocalDateTime s = start();
                java.time.Duration shift = java.time.Duration.between(lastStart[0], s);
                LocalDateTime oldEnd = allDay.isSelected() ? fromDate(endDate.getValue()).toLocalDate().atStartOfDay()
                        : fromDate(endDate.getValue()).toLocalDate().atTime(fromDate(endTime.getValue()).toLocalTime());
                LocalDateTime newEnd = oldEnd.plus(shift);
                endDate.setValue(toDate(newEnd));
                if (!allDay.isSelected()) endTime.setValue(toDate(newEnd));
                lastStart[0] = s;
                // le jour de la semaine suit le début si un seul jour était coché
                int on = 0;
                for (JToggleButton b : weekdays) if (b.isSelected()) on++;
                if (on <= 1) {
                    for (int i = 0; i < 7; i++) weekdays[i].setSelected(DayOfWeek.of(i + 1) == s.getDayOfWeek());
                }
                fillMonthModes();
                adjusting = false;
                updateSummary();
            }
        };
        startDate.addChangeListener(startMoved);
        startTime.addChangeListener(startMoved);
        ChangeListener simple = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (!adjusting) updateSummary();
            }
        };
        endDate.addChangeListener(simple);
        endTime.addChangeListener(simple);
        interval.addChangeListener(simple);
        until.addChangeListener(simple);
        count.addChangeListener(simple);
        ActionListener visibility = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!adjusting) updateVisibility();
            }
        };
        repeat.addActionListener(visibility);
        endMode.addActionListener(visibility);
        monthMode.addActionListener(visibility);
        allDay.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                lastStart[0] = start();
                updateVisibility();
            }
        });
        for (JToggleButton b : weekdays) b.addActionListener(visibility);
        title.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                guessCategory();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
    }

    /** « Anniversaire de ... » : on propose la catégorie et la répétition annuelle d'une création. */
    private void guessCategory() {
        if (base.uid != null) return;
        String t = Fmt.normalize(title.getText());
        if (t.startsWith("anniv") && category.getSelectedItem() == Category.PERSONNEL) {
            category.setSelectedItem(Category.ANNIVERSAIRE);
            if (repeat.getSelectedIndex() == 0) {
                allDay.setSelected(true);
                repeat.setSelectedIndex(4);
                updateVisibility();
            }
        }
    }

    private void updateVisibility() {
        boolean timed = !allDay.isSelected();
        startTime.setVisible(timed);
        endTime.setVisible(timed);
        int r = repeat.getSelectedIndex();
        repeatDetails.setVisible(r > 0);
        unit.setText(UNITS[Math.max(0, r)]);
        weekPanel.setVisible(r == 2);
        monthMode.setVisible(r == 3);
        until.setVisible(endMode.getSelectedIndex() == 1);
        count.setVisible(endMode.getSelectedIndex() == 2);
        updateSummary();
        if (isDisplayable()) {
            pack();
        }
    }

    private void updateSummary() {
        Recurrence r = recurrence();
        summary.setText(r == null ? " " : r.describe(startDay()));
    }

    private void save() {
        LocalDateTime s = start();
        LocalDateTime e = end();
        if (e.isBefore(s)) {
            JOptionPane.showMessageDialog(this, "La fin doit être après le début.", "Dates à corriger", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Recurrence r = recurrence();
        if (r != null && r.until != null && r.until.isBefore(s.toLocalDate())) {
            JOptionPane.showMessageDialog(this, "La date de fin de la répétition est avant le début.", "Dates à corriger", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Event out = base.copy();
        out.title = title.getText().trim();
        out.location = location.getText().trim();
        out.notes = notes.getText().trim();
        out.category = (Category) category.getSelectedItem();
        out.allDay = allDay.isSelected();
        out.start = s;
        out.end = e;
        out.recurrence = r;
        if (r == null) out.exdates.clear();
        out.reminder = Fmt.REMINDERS[Math.max(0, reminder.getSelectedIndex())];
        result = out;
        action = Action.SAVE;
        dispose();
    }

    /** Valeurs d'un nouvel événement. */
    static Event fresh(LocalDateTime start, boolean allDay) {
        Event e = new Event();
        e.allDay = allDay;
        if (allDay) {
            e.start = start.toLocalDate().atStartOfDay();
            e.end = e.start.plusDays(1);
            e.reminder = -1;
        } else {
            e.start = start;
            e.end = start.plusHours(1);
            e.reminder = 15;
            if (start.toLocalTime().isAfter(LocalTime.of(23, 0))) e.end = start.toLocalDate().plusDays(1).atStartOfDay();
        }
        return e;
    }
}
