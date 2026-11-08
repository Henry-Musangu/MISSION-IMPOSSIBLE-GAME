package agenda.android;

import agenda.Agenda;
import agenda.Event;
import agenda.Fmt;
import agenda.Ics;
import agenda.Occurrence;
import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** L'écran principal : le mois et le programme du jour choisi, ou la liste des prochains événements. */
public class MainActivity extends Activity implements MonthView.Listener {

    static final String EXTRA_DATE = "date";

    private static final int REQ_IMPORT = 1;
    private static final int REQ_EXPORT = 2;
    private static final int REQ_EVENT = 3;
    private static final int REQ_NOTIFICATIONS = 4;

    private Agenda agenda;
    private LocalDate selected = LocalDate.now();
    private LocalDate today = LocalDate.now();
    private boolean listMode;
    private String query = "";

    private MonthView monthView;
    private View divider;
    private RowAdapter adapter;
    private ListView list;
    private MenuItem searchItem;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        agenda = AgendaApp.agenda(this);
        if (state != null) {
            selected = LocalDate.ofEpochDay(state.getLong("selected", LocalDate.now().toEpochDay()));
            listMode = state.getBoolean("list", false);
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Ui.BG);
        LinearLayout content = Ui.column(this);
        root.addView(content, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        monthView = new MonthView(this, this);
        content.addView(monthView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        divider = new View(this);
        divider.setBackgroundColor(Ui.BORDER);
        content.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Ui.dp(this, 1))));

        list = new ListView(this);
        list.setDivider(null);
        list.setDividerHeight(0);
        list.setClipToPadding(false);
        list.setPadding(0, 0, 0, Ui.dp(this, 96));
        list.setSelector(android.R.color.transparent);
        adapter = new RowAdapter(this);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                RowAdapter.Row r = adapter.getItem(position);
                if (r.occurrence != null) open(r.occurrence);
            }
        });
        content.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        // bouton rond « + »
        TextView fab = new TextView(this);
        fab.setText("+");
        fab.setTextSize(30);
        fab.setTextColor(0xFFFFFFFF);
        fab.setGravity(Gravity.CENTER);
        fab.setIncludeFontPadding(false);
        fab.setPadding(0, 0, 0, Ui.dp(this, 3));
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(Ui.ACCENT);
        fab.setBackground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x40FFFFFF), circle, null));
        fab.setElevation(Ui.dp(this, 6));
        fab.setContentDescription("Nouvel événement");
        fab.setClickable(true);
        int size = Ui.dp(this, 60);
        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(size, size, Gravity.BOTTOM | Gravity.END);
        flp.setMargins(0, 0, Ui.dp(this, 20), Ui.dp(this, 24));
        root.addView(fab, flp);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                create(selected, false);
            }
        });

        setContentView(root);
        askNotificationPermission();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    /** Ouverture depuis une notification (aller au jour) ou depuis un fichier .ics (proposer l'import). */
    private void handleIntent(Intent intent) {
        if (intent == null) return;
        if (intent.hasExtra(EXTRA_DATE)) {
            selected = LocalDate.ofEpochDay(intent.getLongExtra(EXTRA_DATE, LocalDate.now().toEpochDay()));
            listMode = false;
            intent.removeExtra(EXTRA_DATE);
        }
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            final Uri uri = intent.getData();
            intent.setAction(Intent.ACTION_MAIN);
            importFrom(uri);
        }
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalDate now = LocalDate.now();
        if (!now.equals(today)) {
            if (selected.equals(today)) selected = now;
            today = now;
        }
        refresh();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putLong("selected", selected.toEpochDay());
        out.putBoolean("list", listMode);
    }

    // ------------------------------------------------------------------ affichage

    private void refresh() {
        boolean searching = !query.isEmpty();
        boolean showMonth = !listMode && !searching;
        monthView.setVisibility(showMonth ? View.VISIBLE : View.GONE);
        divider.setVisibility(showMonth ? View.VISIBLE : View.GONE);
        ActionBar bar = getActionBar();
        List<RowAdapter.Row> rows = new ArrayList<RowAdapter.Row>();
        if (searching) {
            if (bar != null) bar.setTitle("Recherche");
            List<Occurrence> found = agenda.search(query, LocalDateTime.now());
            rows.add(RowAdapter.Row.heading(found.isEmpty() ? "Aucun résultat" : Fmt.plural(found.size(), "résultat"), false));
            LocalDateTime now = LocalDateTime.now();
            boolean pastHeader = false;
            for (Occurrence o : found) {
                if (o.end.isBefore(now) && !pastHeader) {
                    rows.add(RowAdapter.Row.heading("Passés", false));
                    pastHeader = true;
                }
                rows.add(RowAdapter.Row.item(o, null));
            }
        } else if (listMode) {
            if (bar != null) bar.setTitle("Prochains événements");
            Map<LocalDate, List<Occurrence>> map = agenda.byDay(today, 92);
            for (Map.Entry<LocalDate, List<Occurrence>> en : map.entrySet()) {
                if (en.getValue().isEmpty()) continue;
                rows.add(RowAdapter.Row.heading(Fmt.dayHeading(en.getKey(), today), en.getKey().equals(today)));
                for (Occurrence o : en.getValue()) rows.add(RowAdapter.Row.item(o, en.getKey()));
            }
            if (rows.isEmpty()) rows.add(RowAdapter.Row.message("Rien de prévu dans les trois prochains mois. Touchez + pour ajouter un événement."));
        } else {
            YearMonth ym = YearMonth.from(selected);
            if (bar != null) bar.setTitle(Fmt.monthTitle(ym));
            LocalDate first = ym.atDay(1).minusDays(ym.atDay(1).getDayOfWeek().getValue() - 1);
            monthView.set(ym, selected, today, agenda.byDay(first, 42));
            rows.add(RowAdapter.Row.heading(Fmt.dayHeading(selected, today), selected.equals(today)));
            List<Occurrence> day = agenda.on(selected);
            for (Occurrence o : day) rows.add(RowAdapter.Row.item(o, selected));
            if (day.isEmpty()) rows.add(RowAdapter.Row.message("Rien de prévu. Touchez + (ou appuyez longuement sur un jour) pour ajouter un événement."));
        }
        if (bar != null) bar.setSubtitle(null);
        adapter.setRows(rows);
    }

    @Override
    public void onDayTapped(LocalDate day) {
        selected = day;
        refresh();
    }

    @Override
    public void onDayLongPressed(LocalDate day) {
        selected = day;
        refresh();
        create(day, false);
    }

    @Override
    public void onSwipe(int direction) {
        YearMonth ym = YearMonth.from(selected).plusMonths(direction);
        selected = ym.atDay(Math.min(selected.getDayOfMonth(), ym.lengthOfMonth()));
        refresh();
    }

    // ------------------------------------------------------------------ événements

    private void create(LocalDate day, boolean allDay) {
        LocalDateTime start;
        if (day.equals(LocalDate.now()) && LocalTime.now().getHour() < 23) {
            start = day.atTime(LocalTime.now().getHour() + 1, 0);
        } else {
            start = day.atTime(9, 0);
        }
        Intent i = new Intent(this, EventActivity.class);
        i.putExtra(EventActivity.EXTRA_START, start.toString());
        i.putExtra(EventActivity.EXTRA_ALL_DAY, allDay);
        startActivityForResult(i, REQ_EVENT);
    }

    private void open(Occurrence o) {
        Intent i = new Intent(this, EventActivity.class);
        i.putExtra(EventActivity.EXTRA_UID, o.event.uid);
        i.putExtra(EventActivity.EXTRA_DATE, o.date().toEpochDay());
        startActivityForResult(i, REQ_EVENT);
    }

    // ------------------------------------------------------------------ menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem todayItem = menu.add(0, 1, 0, "Aujourd'hui");
        todayItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        searchItem = menu.add(0, 2, 1, "Rechercher");
        searchItem.setIcon(android.R.drawable.ic_menu_search);
        searchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        SearchView sv = new SearchView(getActionBar() != null ? getActionBar().getThemedContext() : this);
        sv.setQueryHint("Titre, lieu, notes…");
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String q) {
                query = q.trim();
                refresh();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String q) {
                query = q.trim();
                refresh();
                return true;
            }
        });
        searchItem.setActionView(sv);
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                query = "";
                refresh();
                return true;
            }
        });

        menu.add(0, 3, 2, listMode ? "Vue mois" : "Vue liste").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(0, 6, 3, "Nouvel événement sur la journée").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(0, 4, 4, "Importer un fichier .ics").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(0, 5, 5, "Exporter l'agenda (.ics)").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem m = menu.findItem(3);
        if (m != null) m.setTitle(listMode ? "Vue mois" : "Vue liste");
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                today = LocalDate.now();
                selected = today;
                refresh();
                list.setSelection(0);
                return true;
            case 3:
                listMode = !listMode;
                invalidateOptionsMenu();
                refresh();
                list.setSelection(0);
                return true;
            case 4: {
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                i.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {"text/calendar", "text/x-vcalendar", "application/ics", "application/octet-stream", "text/plain"});
                startActivityForResult(i, REQ_IMPORT);
                return true;
            }
            case 5: {
                Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("text/calendar");
                i.putExtra(Intent.EXTRA_TITLE, "agenda.ics");
                startActivityForResult(i, REQ_EXPORT);
                return true;
            }
            case 6:
                create(selected, true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (!query.isEmpty() && searchItem != null) {
            searchItem.collapseActionView();
            return;
        }
        if (listMode) {
            listMode = false;
            invalidateOptionsMenu();
            refresh();
            return;
        }
        super.onBackPressed();
    }

    // ------------------------------------------------------------------ fichiers et retours

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        if (requestCode == REQ_IMPORT && data.getData() != null) {
            importFrom(data.getData());
        } else if (requestCode == REQ_EXPORT && data.getData() != null) {
            exportTo(data.getData());
        } else if (requestCode == REQ_EVENT && data.hasExtra(EXTRA_DATE)) {
            selected = LocalDate.ofEpochDay(data.getLongExtra(EXTRA_DATE, selected.toEpochDay()));
            refresh();
        }
    }

    private void importFrom(Uri uri) {
        final List<Event> events;
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            if (in == null) throw new IOException("fichier introuvable");
            Reader r = new InputStreamReader(in, StandardCharsets.UTF_8);
            try {
                events = Ics.read(r);
            } finally {
                r.close();
            }
        } catch (IOException | RuntimeException e) {
            Toast.makeText(this, "Lecture impossible : " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        if (events.isEmpty()) {
            Toast.makeText(this, "Aucun événement dans ce fichier.", Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Importer")
                .setMessage("Ajouter " + Fmt.plural(events.size(), "événement") + " à votre agenda ?")
                .setPositiveButton("Importer", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int n = agenda.importAll(events);
                        refresh();
                        Toast.makeText(MainActivity.this, Fmt.plural(n, "événement") + " importé" + (n > 1 ? "s" : ""), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void exportTo(Uri uri) {
        try {
            OutputStream out = getContentResolver().openOutputStream(uri, "wt");
            if (out == null) throw new IOException("fichier introuvable");
            Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8);
            try {
                Ics.write(agenda.events(), w);
            } finally {
                w.close();
            }
            Toast.makeText(this, Fmt.plural(agenda.size(), "événement") + " exporté" + (agenda.size() > 1 ? "s" : ""), Toast.LENGTH_SHORT).show();
        } catch (IOException | RuntimeException e) {
            Toast.makeText(this, "Écriture impossible : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /** Android 13 et plus : demander une fois l'autorisation d'afficher les rappels. */
    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) return;
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return;
        SharedPreferences p = getSharedPreferences("agenda", MODE_PRIVATE);
        if (p.getBoolean("askedNotifications", false)) return;
        p.edit().putBoolean("askedNotifications", true).apply();
        requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS);
    }
}
