package agenda.android;

import agenda.Category;
import agenda.Fmt;
import agenda.Occurrence;
import agenda.Statement;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Dessine le relevé d'activités, à la manière d'un relevé bancaire. Le même dessin sert à l'écran
 * et au fichier PDF : tout est exprimé en « unités » (dp à l'écran, points typographiques dans le PDF).
 */
final class StatementRenderer {

    /** Un morceau du relevé, dessiné d'un bloc (pour découper le PDF en pages). */
    abstract static class Block {
        /** Ligne du tableau : on répète l'en-tête du tableau en haut d'une nouvelle page. */
        boolean tableRow;

        abstract float height();

        abstract void draw(Canvas c, float top);
    }

    private final Context context;
    private final Statement st;
    private final String owner;
    private final float width;
    private final float s;
    private final float left;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Typeface regular = Typeface.create("sans-serif", Typeface.NORMAL);
    private final Typeface medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private final Typeface bold = Typeface.create("sans-serif", Typeface.BOLD);

    /**
     * @param width largeur disponible en pixels
     * @param s     pixels par unité
     * @param left  marge gauche en pixels
     */
    StatementRenderer(Context context, Statement st, String owner, float width, float s, float left) {
        this.context = context;
        this.st = st;
        this.owner = owner;
        this.width = width;
        this.s = s;
        this.left = left;
    }

    private float u(float v) {
        return v * s;
    }

    private void text(Canvas c, String t, float x, float y, float size, Typeface tf, int color, Paint.Align align) {
        paint.setTypeface(tf);
        paint.setTextSize(u(size));
        paint.setColor(color);
        paint.setTextAlign(align);
        c.drawText(t, x, y, paint);
    }

    private String fit(String t, float size, Typeface tf, float max) {
        paint.setTypeface(tf);
        paint.setTextSize(u(size));
        if (paint.measureText(t) <= max) return t;
        int n = paint.breakText(t, true, Math.max(0, max - paint.measureText("…")), null);
        return n <= 0 ? "" : t.substring(0, n).trim() + "…";
    }

    static int statusColor(Statement.Status status) {
        switch (status) {
            case DONE: return Ui.SUCCESS;
            case OVERDUE: return Ui.DANGER;
            case TODO: return 0xFFD97706;
            case UPCOMING: return Ui.ACCENT;
            default: return Ui.MUTED;
        }
    }

    // ------------------------------------------------------------------ blocs

    /** Le bandeau : logo, titre, nom et période. */
    Block header() {
        return new Block() {
            @Override
            float height() {
                return u(112);
            }

            @Override
            void draw(Canvas c, float top) {
                // fond crème du logo, filet or
                paint.setColor(Ui.CREAM);
                rect.set(left, top, left + width, top + u(100));
                c.drawRoundRect(rect, u(10), u(10), paint);
                paint.setColor(Ui.GOLD);
                c.drawRect(left, top + u(96), left + width, top + u(100), paint);
                Drawable logo = context.getDrawable(R.drawable.logo);
                float ls = u(84);
                if (logo != null) {
                    logo.setBounds((int) (left + u(6)), (int) (top + u(6)), (int) (left + u(6) + ls), (int) (top + u(6) + ls));
                    logo.draw(c);
                }
                float x = left + u(6) + ls + u(10);
                float right = left + width - u(14);
                text(c, "RELEVÉ D'ACTIVITÉS", x, top + u(32), 15, bold, Ui.GOLD_DARK, Paint.Align.LEFT);
                text(c, fit(owner.isEmpty() ? "Mon agenda" : owner, 14, medium, right - x), x, top + u(53), 14, medium, Ui.BRAND, Paint.Align.LEFT);
                text(c, fit(Fmt.capitalize(st.periodLabel()), 11.5f, regular, right - x), x, top + u(71), 11.5f, regular, 0xFF475569, Paint.Align.LEFT);
                text(c, fit("Établi le " + Fmt.compactDate(st.issued.toLocalDate()) + " à " + Fmt.time(st.issued), 10, regular, right - x),
                        x, top + u(87), 10, regular, Ui.MUTED, Paint.Align.LEFT);
            }
        };
    }

    /** Les chiffres clés, en tuiles. */
    Block summary() {
        return new Block() {
            @Override
            float height() {
                return u(76);
            }

            @Override
            void draw(Canvas c, float top) {
                String[] values = {
                        String.valueOf(st.lines.size()),
                        String.valueOf(st.appointments),
                        st.done + "/" + st.tasks,
                        String.valueOf(st.overdue)};
                String[] labels = {"Activités", "Rendez-vous", "Tâches faites", "En retard"};
                String[] subs = {
                        Statement.duration(st.minutes) + " planifiées",
                        st.appointments == 0 ? "—" : Statement.duration(st.minutes),
                        st.completionRate() < 0 ? (st.todo > 0 ? st.todo + " à venir" : "—") : st.completionRate() + " % réalisées",
                        st.overdue == 0 ? "Rien en retard" : "à rattraper"};
                int[] colors = {Ui.BRAND, Ui.ACCENT, Ui.SUCCESS, st.overdue > 0 ? Ui.DANGER : Ui.MUTED};
                float gap = u(8);
                float w = (width - 3 * gap) / 4f;
                for (int i = 0; i < 4; i++) {
                    float x = left + i * (w + gap);
                    paint.setColor(0xFFF8FAFC);
                    rect.set(x, top, x + w, top + u(66));
                    c.drawRoundRect(rect, u(8), u(8), paint);
                    paint.setColor(colors[i]);
                    c.drawRect(x, top + u(8), x + u(3), top + u(58), paint);
                    text(c, values[i], x + u(11), top + u(28), 19, bold, colors[i], Paint.Align.LEFT);
                    text(c, fit(labels[i], 10.5f, medium, w - u(14)), x + u(11), top + u(44), 10.5f, medium, Ui.TEXT, Paint.Align.LEFT);
                    text(c, fit(subs[i], 9.5f, regular, w - u(14)), x + u(11), top + u(58), 9.5f, regular, Ui.MUTED, Paint.Align.LEFT);
                }
            }
        };
    }

    /** Un titre de section. */
    Block title(final String t) {
        return new Block() {
            @Override
            float height() {
                return u(30);
            }

            @Override
            void draw(Canvas c, float top) {
                text(c, t.toUpperCase(Locale.FRENCH), left, top + u(21), 11, bold, Ui.BRAND, Paint.Align.LEFT);
                paint.setColor(Ui.GOLD);
                c.drawRect(left, top + u(25), left + u(28), top + u(27), paint);
            }
        };
    }

    /** Répartition par catégorie, avec une barre proportionnelle. */
    List<Block> categories() {
        List<Block> out = new ArrayList<Block>();
        int max = 1;
        for (Category cat : Category.values()) max = Math.max(max, st.countByCategory.get(cat));
        final int fmax = max;
        for (final Category cat : Category.values()) {
            final int n = st.countByCategory.get(cat);
            if (n == 0) continue;
            final long min = st.minutesByCategory.get(cat);
            out.add(new Block() {
                @Override
                float height() {
                    return u(22);
                }

                @Override
                void draw(Canvas c, float top) {
                    float cy = top + u(11);
                    paint.setColor(cat.color);
                    c.drawCircle(left + u(5), cy, u(4), paint);
                    text(c, cat.label, left + u(16), cy + u(4), 11, medium, Ui.TEXT, Paint.Align.LEFT);
                    float barX = left + width * 0.32f;
                    float barW = width * 0.40f;
                    paint.setColor(0xFFF1F5F9);
                    rect.set(barX, cy - u(4), barX + barW, cy + u(4));
                    c.drawRoundRect(rect, u(4), u(4), paint);
                    paint.setColor(cat.color);
                    rect.set(barX, cy - u(4), barX + Math.max(u(8), barW * n / fmax), cy + u(4));
                    c.drawRoundRect(rect, u(4), u(4), paint);
                    String right = n + (min > 0 ? " · " + Statement.duration(min) : "");
                    text(c, right, left + width, cy + u(4), 11, regular, Ui.MUTED, Paint.Align.RIGHT);
                }
            });
        }
        return out;
    }

    // colonnes du tableau (fractions de la largeur)
    private float colDate() {
        return left + u(6);
    }

    private float colLabel() {
        return left + Math.max(u(62), width * 0.16f);
    }

    private float colDuration() {
        return left + width * 0.62f;
    }

    private float colStatus() {
        return left + width - u(6);
    }

    /** En-tête du tableau. */
    Block tableHeader() {
        return new Block() {
            @Override
            float height() {
                return u(26);
            }

            @Override
            void draw(Canvas c, float top) {
                paint.setColor(Ui.BRAND);
                rect.set(left, top, left + width, top + u(24));
                c.drawRoundRect(rect, u(5), u(5), paint);
                float y = top + u(16);
                text(c, "DATE", colDate(), y, 9.5f, bold, 0xFFFFFFFF, Paint.Align.LEFT);
                text(c, "LIBELLÉ", colLabel(), y, 9.5f, bold, 0xFFFFFFFF, Paint.Align.LEFT);
                text(c, "DURÉE", colDuration(), y, 9.5f, bold, 0xFFFFFFFF, Paint.Align.LEFT);
                text(c, "ÉTAT", colStatus(), y, 9.5f, bold, 0xFFFFFFFF, Paint.Align.RIGHT);
            }
        };
    }

    /** Séparateur de mois dans le tableau. */
    private Block month(final YearMonth ym) {
        Block b = new Block() {
            @Override
            float height() {
                return u(24);
            }

            @Override
            void draw(Canvas c, float top) {
                text(c, Fmt.monthTitle(ym).toUpperCase(Locale.FRENCH), colDate(), top + u(17), 10, bold, Ui.GOLD_DARK, Paint.Align.LEFT);
                paint.setColor(0xFFE2E8F0);
                c.drawRect(left, top + u(22), left + width, top + u(23), paint);
            }
        };
        b.tableRow = true;
        return b;
    }

    /** Une ligne du relevé. */
    private Block row(final Statement.Line line) {
        Block b = new Block() {
            @Override
            float height() {
                return u(38);
            }

            @Override
            void draw(Canvas c, float top) {
                Occurrence o = line.occurrence;
                if (line.number % 2 == 0) {
                    paint.setColor(0xFFF8FAFC);
                    c.drawRect(left, top, left + width, top + u(38), paint);
                }
                LocalDate d = o.date();
                String date = String.format(Locale.ROOT, "%02d/%02d", d.getDayOfMonth(), d.getMonthValue());
                text(c, date, colDate(), top + u(16), 11, medium, Ui.TEXT, Paint.Align.LEFT);
                String hour = o.event.allDay ? "journée" : Fmt.time(o.start);
                text(c, Fmt.DAYS_SHORT[d.getDayOfWeek().getValue() - 1] + " " + hour, colDate(), top + u(30), 9, regular, Ui.MUTED, Paint.Align.LEFT);

                float lx = colLabel();
                float lw = colDuration() - lx - u(8);
                String titleText = (o.event.task ? "✓ " : "") + o.event.displayTitle();
                text(c, fit(titleText, 11, medium, lw), lx, top + u(16), 11, medium, Ui.TEXT, Paint.Align.LEFT);
                String sub = o.event.category.label + (o.event.location.trim().isEmpty() ? "" : " · " + o.event.location.trim());
                paint.setColor(o.event.category.color);
                c.drawCircle(lx + u(3), top + u(27), u(3), paint);
                text(c, fit(sub, 9, regular, lw - u(10)), lx + u(10), top + u(30), 9, regular, Ui.MUTED, Paint.Align.LEFT);

                String dur = o.event.task ? "tâche" : o.event.allDay ? "journée" : Statement.duration(line.minutes);
                text(c, dur, colDuration(), top + u(16), 10.5f, regular, Ui.TEXT, Paint.Align.LEFT);

                int col = statusColor(line.status);
                String label = line.status.label;
                paint.setTypeface(bold);
                paint.setTextSize(u(9));
                float tw = paint.measureText(label) + u(12);
                float sx = colStatus() - tw;
                paint.setColor((col & 0x00FFFFFF) | 0x22000000);
                rect.set(sx, top + u(7), colStatus(), top + u(21));
                c.drawRoundRect(rect, u(7), u(7), paint);
                text(c, label, colStatus() - u(6), top + u(17.5f), 9, bold, col, Paint.Align.RIGHT);
                text(c, "N° " + line.number, colStatus(), top + u(31), 8.5f, regular, Ui.FAINT, Paint.Align.RIGHT);
            }
        };
        b.tableRow = true;
        return b;
    }

    /** Totaux en bas du tableau. */
    Block totals() {
        return new Block() {
            @Override
            float height() {
                return u(58);
            }

            @Override
            void draw(Canvas c, float top) {
                paint.setColor(Ui.BRAND);
                c.drawRect(left, top + u(4), left + width, top + u(6), paint);
                text(c, "TOTAL DE LA PÉRIODE", left + u(6), top + u(24), 10, bold, Ui.BRAND, Paint.Align.LEFT);
                text(c, Fmt.plural(st.lines.size(), "ligne"), left + width - u(6), top + u(24), 10, bold, Ui.BRAND, Paint.Align.RIGHT);
                String detail = st.appointments + " rendez-vous" + " (" + Statement.duration(st.minutes) + ")"
                        + " · " + Fmt.plural(st.tasks, "tâche") + " : " + st.done + " faite" + (st.done > 1 ? "s" : "")
                        + ", " + st.todo + " à faire, " + st.overdue + " en retard";
                text(c, fit(detail, 9.5f, regular, width - u(12)), left + u(6), top + u(42), 9.5f, regular, Ui.MUTED, Paint.Align.LEFT);
            }
        };
    }

    Block message(final String t) {
        return new Block() {
            @Override
            float height() {
                return u(48);
            }

            @Override
            void draw(Canvas c, float top) {
                text(c, t, left + width / 2, top + u(28), 12, regular, Ui.FAINT, Paint.Align.CENTER);
            }
        };
    }

    Block space(final float h) {
        return new Block() {
            @Override
            float height() {
                return u(h);
            }

            @Override
            void draw(Canvas c, float top) {
            }
        };
    }

    /** Tout le relevé, dans l'ordre. */
    List<Block> blocks() {
        List<Block> out = new ArrayList<Block>();
        out.add(header());
        out.add(summary());
        List<Block> cats = categories();
        if (!cats.isEmpty()) {
            out.add(title("Par catégorie"));
            out.addAll(cats);
            out.add(space(6));
        }
        out.add(title("Détail des opérations"));
        if (st.lines.isEmpty()) {
            out.add(message("Aucune activité sur cette période."));
            return out;
        }
        out.add(tableHeader());
        YearMonth current = null;
        boolean several = !YearMonth.from(st.from).equals(YearMonth.from(st.to));
        for (Statement.Line l : st.lines) {
            YearMonth ym = YearMonth.from(l.occurrence.date());
            if (several && !ym.equals(current)) out.add(month(ym));
            current = ym;
            out.add(row(l));
        }
        out.add(totals());
        return out;
    }

    /** Pied de page du PDF. */
    void footer(Canvas c, float bottom, int page, int pages) {
        text(c, "Henry Agenda — relevé d'activités " + st.periodLabel(), left, bottom, 8, regular, Ui.FAINT, Paint.Align.LEFT);
        text(c, "Page " + page + " / " + pages, left + width, bottom, 8, regular, Ui.FAINT, Paint.Align.RIGHT);
    }
}
