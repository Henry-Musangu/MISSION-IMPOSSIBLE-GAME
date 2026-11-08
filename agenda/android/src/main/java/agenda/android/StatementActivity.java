package agenda.android;

import agenda.Fmt;
import agenda.Statement;
import android.app.ActionBar;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;

/** Relevé d'activités sur la période choisie, à l'écran, en PDF ou à l'impression. */
public class StatementActivity extends Activity {

    private static final String[] PERIODS = {"Aujourd'hui", "Cette semaine", "La semaine dernière", "Ce mois-ci",
            "Le mois dernier", "Les 3 derniers mois", "Cette année", "L'année dernière", "Période choisie"};
    private static final int CUSTOM = PERIODS.length - 1;
    private static final int REQ_PDF = 1;

    private LocalDate from;
    private LocalDate to;
    private Spinner period;
    private Button fromBtn;
    private Button toBtn;
    private StatementView view;
    private boolean silent;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setTitle("Relevé d'activités");
            bar.setDisplayHomeAsUpEnabled(true);
        }
        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Ui.BG);

        LinearLayout controls = Ui.column(this);
        controls.setBackgroundColor(Ui.SURFACE);
        int pad = Ui.dp(this, 14);
        controls.setPadding(pad, Ui.dp(this, 8), pad, Ui.dp(this, 6));
        period = new Spinner(this);
        ArrayAdapter<String> a = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, PERIODS);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        period.setAdapter(a);
        controls.addView(period);
        LinearLayout dates = Ui.row(this);
        dates.addView(Ui.text(this, "Du", 15, Ui.MUTED, false));
        fromBtn = dateButton();
        dates.addView(fromBtn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        dates.addView(Ui.text(this, "au", 15, Ui.MUTED, false));
        toBtn = dateButton();
        dates.addView(toBtn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        controls.addView(dates);
        root.addView(controls);
        View line = new View(this);
        line.setBackgroundColor(Ui.BORDER);
        root.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Ui.dp(this, 1))));

        ScrollView scroll = new ScrollView(this);
        view = new StatementView(this);
        scroll.addView(view);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);

        int preset = 3;
        if (state != null) {
            preset = state.getInt("preset", 3);
            from = LocalDate.ofEpochDay(state.getLong("from"));
            to = LocalDate.ofEpochDay(state.getLong("to"));
        } else {
            applyPreset(preset);
        }
        silent = true;
        period.setSelection(preset);
        period.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (silent) {
                    silent = false;
                    return;
                }
                if (position != CUSTOM) {
                    applyPreset(position);
                    refresh();
                } else {
                    pick(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        fromBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pick(true);
            }
        });
        toBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pick(false);
            }
        });
        refresh();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("preset", period.getSelectedItemPosition());
        out.putLong("from", from.toEpochDay());
        out.putLong("to", to.toEpochDay());
    }

    private Button dateButton() {
        Button b = new Button(this, null, android.R.attr.borderlessButtonStyle);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setTextColor(Ui.ACCENT);
        b.setGravity(Gravity.CENTER);
        return b;
    }

    private void applyPreset(int p) {
        LocalDate today = LocalDate.now();
        switch (p) {
            case 0:
                from = today;
                to = today;
                break;
            case 1:
                from = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                to = from.plusDays(6);
                break;
            case 2:
                from = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1);
                to = from.plusDays(6);
                break;
            case 3:
                from = today.withDayOfMonth(1);
                to = today.with(TemporalAdjusters.lastDayOfMonth());
                break;
            case 4:
                from = today.minusMonths(1).withDayOfMonth(1);
                to = from.with(TemporalAdjusters.lastDayOfMonth());
                break;
            case 5:
                from = today.minusMonths(2).withDayOfMonth(1);
                to = today.with(TemporalAdjusters.lastDayOfMonth());
                break;
            case 6:
                from = today.withDayOfYear(1);
                to = today.with(TemporalAdjusters.lastDayOfYear());
                break;
            case 7:
                from = today.minusYears(1).withDayOfYear(1);
                to = from.with(TemporalAdjusters.lastDayOfYear());
                break;
            default:
                break;
        }
    }

    private void pick(final boolean start) {
        LocalDate d = start ? from : to;
        DatePickerDialog dlg = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker v, int y, int m, int day) {
                LocalDate chosen = LocalDate.of(y, m + 1, day);
                if (start) {
                    from = chosen;
                    if (to.isBefore(from)) to = from;
                } else {
                    to = chosen;
                    if (from.isAfter(to)) from = to;
                }
                if (period.getSelectedItemPosition() != CUSTOM) {
                    silent = true;
                    period.setSelection(CUSTOM);
                }
                refresh();
            }
        }, d.getYear(), d.getMonthValue() - 1, d.getDayOfMonth());
        dlg.getDatePicker().setFirstDayOfWeek(java.util.Calendar.MONDAY);
        dlg.show();
    }

    private Statement statement() {
        return Statement.build(AgendaApp.agenda(this), from, to, LocalDateTime.now());
    }

    private void refresh() {
        fromBtn.setText(Fmt.compactDate(from));
        toBtn.setText(Fmt.compactDate(to));
        view.setStatement(statement(), MainActivity.ownerName(this));
    }

    // ------------------------------------------------------------------ vue à l'écran

    /** Le relevé dessiné à l'écran, sur toute la largeur. */
    private static final class StatementView extends View {
        private Statement st;
        private String owner = "";
        private List<StatementRenderer.Block> blocks;
        private int laidOutWidth = -1;

        StatementView(Context c) {
            super(c);
            setBackgroundColor(Ui.SURFACE);
        }

        void setStatement(Statement st, String owner) {
            this.st = st;
            this.owner = owner;
            laidOutWidth = -1;
            requestLayout();
            invalidate();
        }

        private void layoutFor(int w) {
            if (st == null || w == laidOutWidth) return;
            float margin = Ui.dp(getContext(), 14);
            float density = getResources().getDisplayMetrics().density;
            blocks = new StatementRenderer(getContext(), st, owner, w - 2 * margin, density, margin).blocks();
            laidOutWidth = w;
        }

        @Override
        protected void onMeasure(int wSpec, int hSpec) {
            int w = MeasureSpec.getSize(wSpec);
            layoutFor(w);
            float h = Ui.dp(getContext(), 14);
            if (blocks != null) for (StatementRenderer.Block b : blocks) h += b.height();
            setMeasuredDimension(w, (int) h + Ui.dp(getContext(), 24));
        }

        @Override
        protected void onDraw(Canvas c) {
            layoutFor(getWidth());
            if (blocks == null) return;
            float y = Ui.dp(getContext(), 14);
            for (StatementRenderer.Block b : blocks) {
                b.draw(c, y);
                y += b.height();
            }
        }
    }

    // ------------------------------------------------------------------ PDF et impression

    /** Écrit le relevé en PDF (format A4) ; renvoie le nombre de pages. */
    private int writePdf(OutputStream out) throws IOException {
        final int pageW = 595;
        final int pageH = 842;
        final float margin = 36;
        final float footer = 22;
        StatementRenderer r = new StatementRenderer(this, statement(), MainActivity.ownerName(this), pageW - 2 * margin, 1f, margin);
        List<StatementRenderer.Block> blocks = r.blocks();
        StatementRenderer.Block tableHeader = r.tableHeader();

        // 1er passage : compter les pages
        int pages = paginate(blocks, tableHeader, pageH, margin, footer, null, null, 0);
        PdfDocument doc = new PdfDocument();
        try {
            paginate(blocks, tableHeader, pageH, margin, footer, doc, r, pages);
            doc.writeTo(out);
        } finally {
            doc.close();
        }
        return pages;
    }

    /** Répartit les blocs sur les pages ; dessine seulement si {@code doc} n'est pas nul. */
    private static int paginate(List<StatementRenderer.Block> blocks, StatementRenderer.Block tableHeader, int pageH,
                                float margin, float footer, PdfDocument doc, StatementRenderer r, int total) {
        int pageNo = 1;
        PdfDocument.Page page = doc == null ? null : doc.startPage(new PdfDocument.PageInfo.Builder(595, pageH, 1).create());
        float y = margin;
        float limit = pageH - margin - footer;
        for (StatementRenderer.Block b : blocks) {
            if (y + b.height() > limit && y > margin) {
                if (page != null) {
                    r.footer(page.getCanvas(), pageH - margin + 6, pageNo, total);
                    doc.finishPage(page);
                }
                pageNo++;
                if (doc != null) page = doc.startPage(new PdfDocument.PageInfo.Builder(595, pageH, pageNo).create());
                y = margin;
                if (b.tableRow) {
                    if (page != null) tableHeader.draw(page.getCanvas(), y);
                    y += tableHeader.height();
                }
            }
            if (page != null) b.draw(page.getCanvas(), y);
            y += b.height();
        }
        if (page != null) {
            r.footer(page.getCanvas(), pageH - margin + 6, pageNo, total);
            doc.finishPage(page);
        }
        return pageNo;
    }

    private String fileName() {
        return String.format(Locale.ROOT, "releve-activites_%s_%s.pdf", from, to);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "PDF").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menu.add(0, 2, 1, "Imprimer ou partager").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(0, 3, 2, "Mon nom").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case 1: {
                Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("application/pdf");
                i.putExtra(Intent.EXTRA_TITLE, fileName());
                startActivityForResult(i, REQ_PDF);
                return true;
            }
            case 2:
                print();
                return true;
            case 3:
                MainActivity.askName(this, new Runnable() {
                    @Override
                    public void run() {
                        refresh();
                    }
                });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PDF || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            OutputStream out = getContentResolver().openOutputStream(uri, "w");
            if (out == null) throw new IOException("fichier introuvable");
            int pages;
            try {
                pages = writePdf(out);
            } finally {
                out.close();
            }
            Toast.makeText(this, "Relevé enregistré en PDF (" + Fmt.plural(pages, "page") + ")", Toast.LENGTH_LONG).show();
        } catch (IOException | RuntimeException e) {
            Toast.makeText(this, "Enregistrement impossible : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /** Impression (ou « Enregistrer au format PDF » / partage, selon les services du téléphone). */
    private void print() {
        PrintManager pm = getSystemService(PrintManager.class);
        final String name = fileName();
        pm.print(name, new PrintDocumentAdapter() {
            @Override
            public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes, CancellationSignal cancel,
                                 LayoutResultCallback callback, Bundle extras) {
                if (cancel.isCanceled()) {
                    callback.onLayoutCancelled();
                    return;
                }
                callback.onLayoutFinished(new PrintDocumentInfo.Builder(name)
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build(), true);
            }

            @Override
            public void onWrite(PageRange[] pages, ParcelFileDescriptor destination, CancellationSignal cancel,
                                WriteResultCallback callback) {
                try {
                    FileOutputStream out = new FileOutputStream(destination.getFileDescriptor());
                    try {
                        writePdf(out);
                    } finally {
                        out.close();
                    }
                    callback.onWriteFinished(new PageRange[] {PageRange.ALL_PAGES});
                } catch (IOException e) {
                    callback.onWriteFailed(e.getMessage());
                }
            }
        }, new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build());
    }
}
