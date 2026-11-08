package agenda.android;

import agenda.Fmt;
import agenda.Occurrence;
import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Les lignes de la liste : titres de jours, événements et messages. */
final class RowAdapter extends BaseAdapter {

    /** Une ligne. */
    static final class Row {
        final String heading;
        /** Couleur du titre de jour (0 : gris). */
        final int headingColor;
        final boolean highlight;
        final Occurrence occurrence;
        /** Jour depuis lequel on regarde l'événement (null : afficher sa date). */
        final LocalDate day;
        final String message;

        private Row(String heading, boolean highlight, Occurrence occurrence, LocalDate day, String message) {
            this(heading, highlight ? Ui.ACCENT : 0, highlight, occurrence, day, message);
        }

        private Row(String heading, int headingColor, boolean highlight, Occurrence occurrence, LocalDate day, String message) {
            this.heading = heading;
            this.headingColor = headingColor;
            this.highlight = highlight;
            this.occurrence = occurrence;
            this.day = day;
            this.message = message;
        }

        static Row heading(String text, boolean highlight) {
            return new Row(text, highlight, null, null, null);
        }

        static Row heading(String text, int color) {
            return new Row(text, color, color != 0, null, null, null);
        }

        static Row item(Occurrence o, LocalDate day) {
            return new Row(null, false, o, day, null);
        }

        static Row message(String text) {
            return new Row(null, false, null, null, text);
        }
    }

    /** Quand on coche ou décoche une tâche. */
    interface OnToggle {
        void toggle(Occurrence o);
    }

    private final Context context;
    private final OnToggle onToggle;
    private List<Row> rows = new ArrayList<Row>();

    RowAdapter(Context context, OnToggle onToggle) {
        this.context = context;
        this.onToggle = onToggle;
    }

    void setRows(List<Row> rows) {
        this.rows = rows;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return rows.size();
    }

    @Override
    public Row getItem(int position) {
        return rows.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public int getItemViewType(int position) {
        Row r = rows.get(position);
        return r.heading != null ? 0 : r.occurrence != null ? 1 : 2;
    }

    @Override
    public boolean isEnabled(int position) {
        return rows.get(position).occurrence != null;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public View getView(int position, View convert, ViewGroup parent) {
        Row r = rows.get(position);
        switch (getItemViewType(position)) {
            case 0: {
                TextView t = convert instanceof TextView ? (TextView) convert : headingView();
                t.setText(r.heading);
                t.setTextColor(r.headingColor != 0 ? r.headingColor : Ui.MUTED);
                return t;
            }
            case 2: {
                TextView t = convert instanceof TextView ? (TextView) convert : messageView();
                t.setText(r.message);
                return t;
            }
            default: {
                ItemHolder h;
                View v = convert;
                if (v == null || !(v.getTag() instanceof ItemHolder)) {
                    h = new ItemHolder(context, onToggle);
                    v = h.root;
                    v.setTag(h);
                } else {
                    h = (ItemHolder) v.getTag();
                }
                h.bind(r);
                return v;
            }
        }
    }

    private TextView headingView() {
        TextView t = Ui.text(context, "", 14, Ui.MUTED, true);
        t.setPadding(Ui.dp(context, 18), Ui.dp(context, 16), Ui.dp(context, 18), Ui.dp(context, 6));
        t.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return t;
    }

    private TextView messageView() {
        TextView t = Ui.text(context, "", 15, Ui.FAINT, false);
        t.setPadding(Ui.dp(context, 18), Ui.dp(context, 14), Ui.dp(context, 18), Ui.dp(context, 14));
        t.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return t;
    }

    /** Une carte d'événement : barre de couleur, titre, heure, lieu. */
    private static final class ItemHolder {
        final LinearLayout root;
        final TextView check;
        final View bar;
        final TextView title;
        final TextView when;
        final TextView place;
        final Context c;
        Occurrence current;

        ItemHolder(Context c, final OnToggle onToggle) {
            this.c = c;
            root = new LinearLayout(c);
            root.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            root.setPadding(Ui.dp(c, 12), Ui.dp(c, 4), Ui.dp(c, 12), Ui.dp(c, 4));
            LinearLayout card = Ui.row(c);
            card.setBackground(Ui.round(Ui.SURFACE, Ui.dp(c, 12), Ui.BORDER, Math.max(1, Ui.dp(c, 1))));
            card.setPadding(Ui.dp(c, 12), Ui.dp(c, 10), Ui.dp(c, 12), Ui.dp(c, 10));
            card.setGravity(Gravity.CENTER_VERTICAL);
            root.addView(card, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            check = new TextView(c);
            check.setGravity(Gravity.CENTER);
            check.setTextColor(0xFFFFFFFF);
            check.setTextSize(16);
            check.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            int cs = Ui.dp(c, 28);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(cs, cs);
            clp.rightMargin = Ui.dp(c, 12);
            card.addView(check, clp);
            check.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (current != null && onToggle != null) {
                        v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                        onToggle.toggle(current);
                    }
                }
            });
            bar = new View(c);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(c, 4), ViewGroup.LayoutParams.MATCH_PARENT);
            lp.rightMargin = Ui.dp(c, 12);
            card.addView(bar, lp);
            LinearLayout texts = Ui.column(c);
            card.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            title = Ui.text(c, "", 16, Ui.TEXT, true);
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);
            when = Ui.text(c, "", 14, Ui.MUTED, false);
            when.setSingleLine(true);
            when.setEllipsize(TextUtils.TruncateAt.END);
            place = Ui.text(c, "", 14, Ui.MUTED, false);
            place.setSingleLine(true);
            place.setEllipsize(TextUtils.TruncateAt.END);
            texts.addView(title);
            texts.addView(when);
            texts.addView(place);
        }

        void bind(Row r) {
            Occurrence o = r.occurrence;
            current = o;
            LocalDateTime now = LocalDateTime.now();
            boolean isTask = o.event.task;
            boolean done = o.isDone();
            boolean late = o.isOverdue(now);
            boolean past = isTask ? done : o.end.isBefore(now);
            check.setVisibility(isTask ? View.VISIBLE : View.GONE);
            bar.setVisibility(isTask ? View.GONE : View.VISIBLE);
            if (isTask) {
                int col = late ? Ui.DANGER : o.event.category.color;
                check.setBackground(done ? oval(Ui.SUCCESS, 0, 0) : oval(0x00000000, col, Ui.dp(c, 2)));
                check.setText(done ? "✓" : "");
                check.setContentDescription(done ? "Fait — toucher pour décocher" : "Toucher pour marquer comme fait");
            }
            bar.setBackground(Ui.round(past ? o.event.category.light(0.55) : o.event.category.color, Ui.dp(c, 2), 0, 0));
            title.setText(o.event.displayTitle() + (o.event.isRecurring() ? "  ↻" : ""));
            title.setTextColor(past ? Ui.MUTED : late ? Ui.DANGER : Ui.TEXT);
            if (done) title.setPaintFlags(title.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            else title.setPaintFlags(title.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            String w;
            if (r.day != null) {
                w = Fmt.whenOn(o, r.day);
            } else {
                LocalDate today = LocalDate.now();
                w = Fmt.capitalize(Fmt.shortDate(o.date())) + (o.date().getYear() != today.getYear() ? " " + o.date().getYear() : "")
                        + " · " + Fmt.when(o);
            }
            String state = isTask ? (done ? "Fait" : late ? "En retard" : "Tâche") : o.event.category.label;
            when.setText(w + "  ·  " + state);
            String loc = o.event.location.trim();
            place.setText(loc);
            place.setVisibility(loc.isEmpty() ? View.GONE : View.VISIBLE);
            root.setContentDescription(o.event.displayTitle() + ", " + w + (loc.isEmpty() ? "" : ", " + loc));
        }

        private static android.graphics.drawable.GradientDrawable oval(int fill, int stroke, int strokePx) {
            android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
            d.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            d.setColor(fill);
            if (strokePx > 0) d.setStroke(strokePx, stroke);
            return d;
        }
    }
}
