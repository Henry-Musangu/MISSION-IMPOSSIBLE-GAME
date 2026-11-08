package agenda.android;

import agenda.Agenda;
import agenda.Category;
import agenda.Event;
import agenda.Fmt;
import agenda.Recurrence;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Formulaire de création ou de modification d'un événement. */
public class EventActivity extends Activity {

    static final String EXTRA_UID = "uid";
    static final String EXTRA_DATE = "date";
    static final String EXTRA_START = "start";
    static final String EXTRA_ALL_DAY = "allDay";

    private static final String[] REPEAT = {"Ne se répète pas", "Tous les jours", "Toutes les semaines", "Tous les mois", "Tous les ans"};
    private static final String[] UNITS = {"", "jour(s)", "semaine(s)", "mois", "an(s)"};
    private static final String[] ENDS = {"Sans fin", "Jusqu'à une date", "Nombre de fois"};

    private Agenda agenda;
    /** L'événement d'origine (null pour une création) et la date de l'occurrence modifiée. */
    private Event original;
    private LocalDate occurrenceDate;
    /** Copie de travail, avec les dates de l'occurrence. */
    private Event base;
    private String initialSnapshot;

    private LocalDate startDay;
    private LocalTime startTime;
    private LocalDate endDay;
    private LocalTime endTime;
    private LocalDate untilDay;
    private final boolean[] weekdays = new boolean[7];

    private EditText title;
    private EditText location;
    private Spinner category;
    private Switch allDay;
    private Button startDateBtn;
    private Button startTimeBtn;
    private Button endDateBtn;
    private Button endTimeBtn;
    private Spinner repeat;
    private LinearLayout repeatDetails;
    private EditText interval;
    private TextView unit;
    private LinearLayout weekRow;
    private final TextView[] dayChips = new TextView[7];
    private Spinner monthMode;
    private Spinner endMode;
    private Button untilBtn;
    private EditText count;
    private TextView summary;
    private Spinner reminder;
    private EditText notes;
    private boolean loading;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        agenda = AgendaApp.agenda(this);
        Intent in = getIntent();
        String uid = in.getStringExtra(EXTRA_UID);
        if (uid != null) {
            original = agenda.find(uid);
            if (original == null) {
                finish();
                return;
            }
            occurrenceDate = LocalDate.ofEpochDay(in.getLongExtra(EXTRA_DATE, original.start.toLocalDate().toEpochDay()));
            base = original.copy();
            base.start = occurrenceDate.atTime(original.start.toLocalTime());
            base.end = base.start.plus(original.duration());
        } else {
            LocalDateTime start = LocalDateTime.parse(in.getStringExtra(EXTRA_START));
            boolean whole = in.getBooleanExtra(EXTRA_ALL_DAY, false);
            base = new Event();
            base.allDay = whole;
            base.start = whole ? start.toLocalDate().atStartOfDay() : start;
            base.end = whole ? base.start.plusDays(1) : start.plusHours(1);
            base.reminder = whole ? -1 : 15;
        }
        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setTitle(original == null ? "Nouvel événement" : "Modifier");
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setHomeAsUpIndicator(android.R.drawable.ic_menu_close_clear_cancel);
        }
        build();
        load(base);
        if (state != null) restore(state);
        updateVisibility();
        initialSnapshot = snapshot();
    }

    // ------------------------------------------------------------------ construction de l'écran

    private void build() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.SURFACE);
        scroll.setFillViewport(true);
        LinearLayout form = Ui.column(this);
        int pad = Ui.dp(this, 18);
        form.setPadding(pad, Ui.dp(this, 8), pad, Ui.dp(this, 32));
        scroll.addView(form);

        title = edit("Titre", false);
        title.setTextSize(22);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        form.addView(title, full());
        location = edit("Lieu", false);
        location.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        form.addView(location, full());

        form.addView(label("Catégorie"));
        category = new Spinner(this);
        category.setAdapter(new CategoryAdapter());
        form.addView(category, full());

        allDay = new Switch(this);
        allDay.setText("Toute la journée");
        allDay.setTextSize(16);
        allDay.setTextColor(Ui.TEXT);
        allDay.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 6));
        form.addView(allDay, full());

        form.addView(label("Début"));
        startDateBtn = pickerButton();
        startTimeBtn = pickerButton();
        form.addView(pair(startDateBtn, startTimeBtn), full());
        form.addView(label("Fin"));
        endDateBtn = pickerButton();
        endTimeBtn = pickerButton();
        form.addView(pair(endDateBtn, endTimeBtn), full());

        form.addView(label("Répétition"));
        repeat = spinner(REPEAT);
        form.addView(repeat, full());
        repeatDetails = Ui.column(this);
        repeatDetails.setPadding(Ui.dp(this, 4), Ui.dp(this, 6), 0, Ui.dp(this, 4));
        LinearLayout every = Ui.row(this);
        every.addView(Ui.text(this, "Tous les", 16, Ui.TEXT, false));
        interval = edit("1", false);
        interval.setInputType(InputType.TYPE_CLASS_NUMBER);
        interval.setGravity(Gravity.CENTER);
        interval.setMinEms(2);
        every.addView(interval, new LinearLayout.LayoutParams(Ui.dp(this, 64), ViewGroup.LayoutParams.WRAP_CONTENT));
        unit = Ui.text(this, "", 16, Ui.TEXT, false);
        every.addView(unit);
        repeatDetails.addView(every);
        weekRow = Ui.row(this);
        weekRow.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
        for (int i = 0; i < 7; i++) {
            final int idx = i;
            TextView chip = Ui.text(this, Fmt.DAYS_LETTER[i], 15, Ui.TEXT, true);
            chip.setGravity(Gravity.CENTER);
            chip.setContentDescription(Fmt.DAYS[i]);
            int s = Ui.dp(this, 38);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(s, s);
            lp.rightMargin = Ui.dp(this, 6);
            weekRow.addView(chip, lp);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    weekdays[idx] = !weekdays[idx];
                    boolean any = false;
                    for (boolean b : weekdays) any |= b;
                    if (!any) weekdays[idx] = true;   // au moins un jour
                    paintChips();
                    updateSummary();
                }
            });
            dayChips[i] = chip;
        }
        repeatDetails.addView(weekRow);
        monthMode = new Spinner(this);
        repeatDetails.addView(monthMode, full());
        LinearLayout endRow = Ui.row(this);
        endMode = spinner(ENDS);
        endRow.addView(endMode, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        untilBtn = pickerButton();
        endRow.addView(untilBtn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        count = edit("10", false);
        count.setInputType(InputType.TYPE_CLASS_NUMBER);
        count.setGravity(Gravity.CENTER);
        endRow.addView(count, new LinearLayout.LayoutParams(Ui.dp(this, 80), ViewGroup.LayoutParams.WRAP_CONTENT));
        repeatDetails.addView(endRow, full());
        summary = Ui.text(this, "", 14, Ui.MUTED, false);
        summary.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
        summary.setPadding(0, Ui.dp(this, 6), 0, 0);
        repeatDetails.addView(summary);
        form.addView(repeatDetails, full());

        form.addView(label("Rappel"));
        String[] rem = new String[Fmt.REMINDERS.length];
        for (int i = 0; i < rem.length; i++) rem[i] = Fmt.reminder(Fmt.REMINDERS[i]);
        reminder = spinner(rem);
        form.addView(reminder, full());

        form.addView(label("Notes"));
        notes = edit("Notes, adresse, numéro…", true);
        notes.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        notes.setMinLines(3);
        notes.setGravity(Gravity.TOP | Gravity.START);
        form.addView(notes, full());

        if (original != null) {
            Button delete = new Button(this, null, android.R.attr.borderlessButtonStyle);
            delete.setText("Supprimer l'événement");
            delete.setTextColor(Ui.DANGER);
            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    delete();
                }
            });
            LinearLayout.LayoutParams lp = full();
            lp.topMargin = Ui.dp(this, 20);
            form.addView(delete, lp);
        }
        // identifiants fixes : Android conserve ainsi les saisies quand on tourne l'écran
        int id = 1000;
        for (View v : new View[] {title, location, category, allDay, repeat, interval, endMode, count, reminder, notes}) {
            v.setId(++id);
        }
        setContentView(scroll);
        wire();
    }

    private LinearLayout.LayoutParams full() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private TextView label(String s) {
        TextView t = Ui.text(this, s.toUpperCase(java.util.Locale.FRENCH), 12, Ui.MUTED, true);
        t.setPadding(Ui.dp(this, 4), Ui.dp(this, 16), 0, Ui.dp(this, 2));
        t.setLetterSpacing(0.06f);
        return t;
    }

    private EditText edit(String hint, boolean multi) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(16);
        e.setSingleLine(!multi);
        return e;
    }

    private Spinner spinner(String[] items) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> a = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(a);
        return s;
    }

    private Button pickerButton() {
        Button b = new Button(this, null, android.R.attr.borderlessButtonStyle);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setTextColor(Ui.TEXT);
        b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        return b;
    }

    private LinearLayout pair(Button a, Button b) {
        LinearLayout row = Ui.row(this);
        row.addView(a, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 3));
        row.addView(b, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2));
        return row;
    }

    /** Liste des catégories avec leur pastille de couleur. */
    private final class CategoryAdapter extends ArrayAdapter<Category> {
        CategoryAdapter() {
            super(EventActivity.this, android.R.layout.simple_spinner_item, Category.values());
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return decorate((TextView) super.getView(position, convertView, parent), position);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return decorate((TextView) super.getDropDownView(position, convertView, parent), position);
        }

        private TextView decorate(TextView t, int position) {
            Category c = getItem(position);
            t.setText(c.label);
            t.setCompoundDrawables(Ui.dot(EventActivity.this, c, 12), null, null, null);
            t.setCompoundDrawablePadding(Ui.dp(EventActivity.this, 10));
            return t;
        }
    }

    // ------------------------------------------------------------------ données

    private void load(Event e) {
        loading = true;
        title.setText(e.title);
        location.setText(e.location);
        category.setSelection(e.category.ordinal());
        allDay.setChecked(e.allDay);
        startDay = e.start.toLocalDate();
        startTime = e.allDay ? LocalTime.of(9, 0) : e.start.toLocalTime();
        if (e.allDay) {
            endDay = e.end.toLocalDate().minusDays(1);
            if (endDay.isBefore(startDay)) endDay = startDay;
            endTime = LocalTime.of(10, 0);
        } else {
            endDay = e.end.toLocalDate();
            endTime = e.end.toLocalTime();
        }
        Recurrence r = e.recurrence;
        repeat.setSelection(r == null ? 0 : r.freq.ordinal() + 1);
        interval.setText(String.valueOf(r == null ? 1 : Math.max(1, r.interval)));
        for (int i = 0; i < 7; i++) {
            DayOfWeek d = DayOfWeek.of(i + 1);
            weekdays[i] = r != null && r.freq == Recurrence.Freq.WEEKLY && !r.days.isEmpty() ? r.days.contains(d) : d == startDay.getDayOfWeek();
        }
        fillMonthModes(r != null && r.freq == Recurrence.Freq.MONTHLY ? r.weekOfMonth : 0);
        untilDay = r != null && r.until != null ? r.until : startDay.plusMonths(3);
        endMode.setSelection(r == null ? 0 : r.until != null ? 1 : r.count > 0 ? 2 : 0);
        count.setText(String.valueOf(r != null && r.count > 0 ? r.count : 10));
        int idx = 0;
        for (int i = 0; i < Fmt.REMINDERS.length; i++) if (Fmt.REMINDERS[i] == e.reminder) idx = i;
        reminder.setSelection(idx);
        notes.setText(e.notes);
        loading = false;
        paintButtons();
        paintChips();
    }

    /** « Le 10 », « Le 2e mardi », et « Le dernier mardi » quand c'est le dernier du mois. */
    private void fillMonthModes(int weekOfMonth) {
        List<String> items = new ArrayList<String>();
        String day = Fmt.DAYS[startDay.getDayOfWeek().getValue() - 1];
        items.add("Le " + (startDay.getDayOfMonth() == 1 ? "1er" : String.valueOf(startDay.getDayOfMonth())) + " de chaque mois");
        int n = Recurrence.weekOfMonthOf(startDay);
        if (n <= 4) items.add("Le " + Fmt.rank(n) + " " + day + " de chaque mois");
        if (Recurrence.isLastOfMonth(startDay)) items.add("Le dernier " + day + " de chaque mois");
        ArrayAdapter<String> a = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        boolean wasLoading = loading;
        loading = true;
        monthMode.setAdapter(a);
        int sel = 0;
        if (weekOfMonth < 0) sel = items.size() - 1;
        else if (weekOfMonth > 0) sel = Math.min(1, items.size() - 1);
        if (weekOfMonth < 0 && !Recurrence.isLastOfMonth(startDay)) sel = Math.min(1, items.size() - 1);
        monthMode.setSelection(sel);
        loading = wasLoading;
    }

    private int selectedWeekOfMonth() {
        int i = monthMode.getSelectedItemPosition();
        if (i <= 0) return 0;
        String item = String.valueOf(monthMode.getSelectedItem());
        return item.contains("dernier") ? -1 : Recurrence.weekOfMonthOf(startDay);
    }

    private static int number(EditText e, int def, int min, int max) {
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(e.getText().toString().trim())));
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    private Recurrence recurrence() {
        int r = repeat.getSelectedItemPosition();
        if (r <= 0) return null;
        Recurrence rec = new Recurrence(Recurrence.Freq.values()[r - 1]);
        rec.interval = number(interval, 1, 1, 99);
        if (rec.freq == Recurrence.Freq.WEEKLY) {
            for (int i = 0; i < 7; i++) if (weekdays[i]) rec.days.add(DayOfWeek.of(i + 1));
            if (rec.days.size() == 1 && rec.days.contains(startDay.getDayOfWeek())) rec.days.clear();
        }
        if (rec.freq == Recurrence.Freq.MONTHLY) rec.weekOfMonth = selectedWeekOfMonth();
        if (endMode.getSelectedItemPosition() == 1) rec.until = untilDay;
        else if (endMode.getSelectedItemPosition() == 2) rec.count = number(count, 10, 1, 999);
        return rec;
    }

    private LocalDateTime start() {
        return allDay.isChecked() ? startDay.atStartOfDay() : startDay.atTime(startTime);
    }

    private LocalDateTime end() {
        return allDay.isChecked() ? endDay.plusDays(1).atStartOfDay() : endDay.atTime(endTime);
    }

    /** Le résultat du formulaire. */
    private Event build(Event from) {
        Event out = from.copy();
        out.title = title.getText().toString().trim();
        out.location = location.getText().toString().trim();
        out.notes = notes.getText().toString().trim();
        out.category = (Category) category.getSelectedItem();
        out.allDay = allDay.isChecked();
        out.start = start();
        out.end = end();
        out.recurrence = recurrence();
        if (out.recurrence == null) out.exdates.clear();
        out.reminder = Fmt.REMINDERS[Math.max(0, reminder.getSelectedItemPosition())];
        return out;
    }

    /** Résumé du formulaire, pour savoir s'il a été modifié. */
    private String snapshot() {
        Event e = build(base);
        Recurrence r = e.recurrence;
        return e.title + "|" + e.location + "|" + e.notes + "|" + e.category + "|" + e.allDay + "|" + e.start + "|" + e.end
                + "|" + e.reminder + "|" + (r == null ? "-" : r.freq + "/" + r.interval + "/" + r.days + "/" + r.weekOfMonth + "/" + r.until + "/" + r.count);
    }

    // ------------------------------------------------------------------ réactions

    private void wire() {
        startDateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickDate(startDay, new DateChosen() {
                    @Override
                    public void chosen(LocalDate d) {
                        long shift = d.toEpochDay() - startDay.toEpochDay();
                        boolean followWeekday = countWeekdays() <= 1;
                        startDay = d;
                        endDay = endDay.plusDays(shift);
                        if (followWeekday) {
                            for (int i = 0; i < 7; i++) weekdays[i] = DayOfWeek.of(i + 1) == d.getDayOfWeek();
                        }
                        fillMonthModes(selectedWeekOfMonth() < 0 ? -1 : selectedWeekOfMonth() > 0 ? 1 : 0);
                        paintButtons();
                        paintChips();
                        updateSummary();
                    }
                });
            }
        });
        endDateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickDate(endDay, new DateChosen() {
                    @Override
                    public void chosen(LocalDate d) {
                        endDay = d;
                        paintButtons();
                    }
                });
            }
        });
        untilBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickDate(untilDay, new DateChosen() {
                    @Override
                    public void chosen(LocalDate d) {
                        untilDay = d;
                        paintButtons();
                        updateSummary();
                    }
                });
            }
        });
        startTimeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePickerDialog(EventActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int h, int m) {
                        LocalDateTime oldStart = startDay.atTime(startTime);
                        LocalDateTime oldEnd = endDay.atTime(endTime);
                        startTime = LocalTime.of(h, m);
                        LocalDateTime newEnd = startDay.atTime(startTime).plus(java.time.Duration.between(oldStart, oldEnd));
                        endDay = newEnd.toLocalDate();
                        endTime = newEnd.toLocalTime();
                        paintButtons();
                    }
                }, startTime.getHour(), startTime.getMinute(), true).show();
            }
        });
        endTimeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePickerDialog(EventActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int h, int m) {
                        endTime = LocalTime.of(h, m);
                        paintButtons();
                    }
                }, endTime.getHour(), endTime.getMinute(), true).show();
            }
        });
        allDay.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton b, boolean checked) {
                updateVisibility();
            }
        });
        AdapterView.OnItemSelectedListener changed = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!loading) updateVisibility();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        repeat.setOnItemSelectedListener(changed);
        endMode.setOnItemSelectedListener(changed);
        monthMode.setOnItemSelectedListener(changed);
        TextWatcher numbers = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!loading) updateSummary();
            }
        };
        interval.addTextChangedListener(numbers);
        count.addTextChangedListener(numbers);
        title.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (loading || original != null) return;
                // « Anniversaire de ... » : catégorie, journée entière et répétition annuelle
                String t = Fmt.normalize(s.toString());
                if (t.startsWith("anniv") && category.getSelectedItemPosition() == Category.PERSONNEL.ordinal()) {
                    category.setSelection(Category.ANNIVERSAIRE.ordinal());
                    if (repeat.getSelectedItemPosition() == 0) {
                        allDay.setChecked(true);
                        repeat.setSelection(4);
                    }
                }
            }
        });
    }

    private int countWeekdays() {
        int n = 0;
        for (boolean b : weekdays) if (b) n++;
        return n;
    }

    private interface DateChosen {
        void chosen(LocalDate d);
    }

    private void pickDate(LocalDate initial, final DateChosen then) {
        DatePickerDialog d = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int y, int m, int day) {
                then.chosen(LocalDate.of(y, m + 1, day));
            }
        }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth());
        d.getDatePicker().setFirstDayOfWeek(java.util.Calendar.MONDAY);
        d.show();
    }

    private void paintButtons() {
        startDateBtn.setText(Fmt.capitalize(Fmt.shortDate(startDay)) + " " + startDay.getYear());
        endDateBtn.setText(Fmt.capitalize(Fmt.shortDate(endDay)) + " " + endDay.getYear());
        startTimeBtn.setText(Fmt.time(startTime));
        endTimeBtn.setText(Fmt.time(endTime));
        untilBtn.setText("Le " + Fmt.compactDate(untilDay));
    }

    private void paintChips() {
        for (int i = 0; i < 7; i++) {
            boolean on = weekdays[i];
            dayChips[i].setBackground(on ? Ui.round(Ui.ACCENT, Ui.dp(this, 19), 0, 0)
                    : Ui.round(Ui.SURFACE, Ui.dp(this, 19), Ui.BORDER, Ui.dp(this, 1)));
            dayChips[i].setTextColor(on ? 0xFFFFFFFF : Ui.TEXT);
        }
    }

    private void updateVisibility() {
        boolean timed = !allDay.isChecked();
        startTimeBtn.setVisibility(timed ? View.VISIBLE : View.INVISIBLE);
        endTimeBtn.setVisibility(timed ? View.VISIBLE : View.INVISIBLE);
        int r = repeat.getSelectedItemPosition();
        repeatDetails.setVisibility(r > 0 ? View.VISIBLE : View.GONE);
        unit.setText(UNITS[Math.max(0, r)]);
        weekRow.setVisibility(r == 2 ? View.VISIBLE : View.GONE);
        monthMode.setVisibility(r == 3 ? View.VISIBLE : View.GONE);
        untilBtn.setVisibility(endMode.getSelectedItemPosition() == 1 ? View.VISIBLE : View.GONE);
        count.setVisibility(endMode.getSelectedItemPosition() == 2 ? View.VISIBLE : View.GONE);
        updateSummary();
    }

    private void updateSummary() {
        Recurrence r = recurrence();
        summary.setText(r == null ? "" : r.describe(startDay));
    }

    // ------------------------------------------------------------------ enregistrer / supprimer

    private void save() {
        final Event edited = build(base);
        if (edited.end.isBefore(edited.start)) {
            Toast.makeText(this, "La fin doit être après le début.", Toast.LENGTH_LONG).show();
            return;
        }
        if (edited.recurrence != null && edited.recurrence.until != null && edited.recurrence.until.isBefore(startDay)) {
            Toast.makeText(this, "La fin de la répétition est avant le début.", Toast.LENGTH_LONG).show();
            return;
        }
        if (original == null) {
            agenda.add(edited);
            done(edited.start.toLocalDate());
            return;
        }
        if (!original.isRecurring()) {
            agenda.applyEdit(original, occurrenceDate, edited, Agenda.Scope.ALL);
            done(edited.start.toLocalDate());
            return;
        }
        askScope("Modifier l'événement répété", new ScopeChosen() {
            @Override
            public void chosen(Agenda.Scope scope) {
                agenda.applyEdit(original, occurrenceDate, edited, scope);
                done(edited.start.toLocalDate());
            }
        });
    }

    private void delete() {
        if (!original.isRecurring()) {
            new AlertDialog.Builder(this)
                    .setMessage("Supprimer « " + original.displayTitle() + " » ?")
                    .setPositiveButton("Supprimer", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            agenda.delete(original, occurrenceDate, Agenda.Scope.ALL);
                            done(null);
                        }
                    })
                    .setNegativeButton("Annuler", null)
                    .show();
            return;
        }
        askScope("Supprimer l'événement répété", new ScopeChosen() {
            @Override
            public void chosen(Agenda.Scope scope) {
                agenda.delete(original, occurrenceDate, scope);
                done(null);
            }
        });
    }

    private interface ScopeChosen {
        void chosen(Agenda.Scope scope);
    }

    private void askScope(String dialogTitle, final ScopeChosen then) {
        String[] items = {"Cet événement uniquement", "Celui-ci et les suivants", "Tous les événements"};
        new AlertDialog.Builder(this)
                .setTitle(dialogTitle)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        then.chosen(which == 0 ? Agenda.Scope.THIS : which == 1 ? Agenda.Scope.FOLLOWING : Agenda.Scope.ALL);
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void done(LocalDate show) {
        Intent result = new Intent();
        if (show != null) result.putExtra(MainActivity.EXTRA_DATE, show.toEpochDay());
        setResult(RESULT_OK, result);
        finish();
    }

    /** Quitter sans enregistrer : on demande confirmation si le formulaire a été modifié. */
    private void leave() {
        if (initialSnapshot == null || initialSnapshot.equals(snapshot())) {
            finish();
            return;
        }
        new AlertDialog.Builder(this)
                .setMessage("Abandonner les modifications ?")
                .setPositiveButton("Abandonner", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNegativeButton("Continuer", null)
                .show();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        leave();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Enregistrer").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            save();
            return true;
        }
        if (item.getItemId() == android.R.id.home) {
            leave();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ------------------------------------------------------------------ rotation de l'écran

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putLong("startDay", startDay.toEpochDay());
        out.putString("startTime", startTime.toString());
        out.putLong("endDay", endDay.toEpochDay());
        out.putString("endTime", endTime.toString());
        out.putLong("untilDay", untilDay.toEpochDay());
        out.putBooleanArray("weekdays", weekdays);
        out.putInt("monthMode", monthMode.getSelectedItemPosition());
    }

    private void restore(Bundle s) {
        startDay = LocalDate.ofEpochDay(s.getLong("startDay", startDay.toEpochDay()));
        startTime = LocalTime.parse(s.getString("startTime", startTime.toString()));
        endDay = LocalDate.ofEpochDay(s.getLong("endDay", endDay.toEpochDay()));
        endTime = LocalTime.parse(s.getString("endTime", endTime.toString()));
        untilDay = LocalDate.ofEpochDay(s.getLong("untilDay", untilDay.toEpochDay()));
        boolean[] w = s.getBooleanArray("weekdays");
        if (w != null && w.length == 7) System.arraycopy(w, 0, weekdays, 0, 7);
        fillMonthModes(0);
        int mm = s.getInt("monthMode", 0);
        if (mm < monthMode.getCount()) monthMode.setSelection(mm);
        paintButtons();
        paintChips();
    }
}
