package agenda.android;

import agenda.Fmt;
import agenda.Occurrence;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Le mois en grille : toucher un jour le sélectionne, un appui long crée un événement,
 * glisser vers la gauche ou la droite change de mois.
 */
final class MonthView extends View {

    interface Listener {
        void onDayTapped(LocalDate day);

        void onDayLongPressed(LocalDate day);

        void onSwipe(int direction);
    }

    private final Listener listener;
    private final GestureDetector gestures;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float header;
    private final Typeface medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);

    private YearMonth month = YearMonth.now();
    private LocalDate selected = LocalDate.now();
    private LocalDate today = LocalDate.now();
    private Map<LocalDate, List<Occurrence>> days = Collections.emptyMap();

    MonthView(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        header = Ui.dp(context, 26);
        setBackgroundColor(Ui.SURFACE);
        gestures = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                LocalDate d = dayAt(e.getX(), e.getY());
                if (d != null) {
                    playSoundEffect(android.view.SoundEffectConstants.CLICK);
                    MonthView.this.listener.onDayTapped(d);
                }
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                LocalDate d = dayAt(e.getX(), e.getY());
                if (d != null) {
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    MonthView.this.listener.onDayLongPressed(d);
                }
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                if (Math.abs(vx) > Math.abs(vy) * 1.3f && Math.abs(vx) > Ui.dp(getContext(), 300)) {
                    MonthView.this.listener.onSwipe(vx < 0 ? 1 : -1);
                    return true;
                }
                return false;
            }
        });
    }

    void set(YearMonth month, LocalDate selected, LocalDate today, Map<LocalDate, List<Occurrence>> days) {
        this.month = month;
        this.selected = selected;
        this.today = today;
        this.days = days;
        setContentDescription(Fmt.monthTitle(month) + ", " + Fmt.longDate(selected) + " sélectionné");
        invalidate();
    }

    /** Premier jour affiché (un lundi). */
    LocalDate firstShown() {
        LocalDate f = month.atDay(1);
        return f.minusDays(f.getDayOfWeek().getValue() - 1);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        getParent().requestDisallowInterceptTouchEvent(event.getAction() == MotionEvent.ACTION_MOVE);
        return gestures.onTouchEvent(event) || super.onTouchEvent(event);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        int h;
        if (MeasureSpec.getMode(heightSpec) == MeasureSpec.EXACTLY) {
            h = MeasureSpec.getSize(heightSpec);
        } else {
            float cell = Math.min(w / 7f * 0.92f, Ui.dp(getContext(), 92));
            h = (int) (header + 6 * cell);
            if (MeasureSpec.getMode(heightSpec) == MeasureSpec.AT_MOST) {
                h = Math.min(h, (int) (MeasureSpec.getSize(heightSpec) * 0.62f));
            }
        }
        setMeasuredDimension(w, h);
    }

    private LocalDate dayAt(float x, float y) {
        if (y < header) return null;
        float cw = getWidth() / 7f;
        float ch = (getHeight() - header) / 6f;
        int col = Math.min(6, Math.max(0, (int) (x / cw)));
        int row = Math.min(5, Math.max(0, (int) ((y - header) / ch)));
        return firstShown().plusDays(row * 7 + col);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Context c = getContext();
        float w = getWidth();
        float cw = w / 7f;
        float ch = (getHeight() - header) / 6f;
        boolean chips = ch >= Ui.dp(c, 64) && cw >= Ui.dp(c, 56);

        // noms des jours
        paint.setTypeface(medium);
        paint.setTextSize(Ui.sp(c, 11));
        paint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < 7; i++) {
            paint.setColor(i >= 5 ? Ui.FAINT : Ui.MUTED);
            String name = chips ? Fmt.DAYS_SHORT[i].replace(".", "").toUpperCase(Locale.FRENCH) : Fmt.DAYS_LETTER[i];
            canvas.drawText(name, cw * i + cw / 2, header - Ui.dp(c, 8), paint);
        }

        LocalDate first = firstShown();
        for (int i = 0; i < 42; i++) {
            LocalDate d = first.plusDays(i);
            int col = i % 7;
            int row = i / 7;
            float x = col * cw;
            float y = header + row * ch;
            boolean in = YearMonth.from(d).equals(month);
            boolean isToday = d.equals(today);
            boolean isSel = d.equals(selected);
            if (!in) {
                paint.setColor(Ui.OUTSIDE);
                canvas.drawRect(x, y, x + cw, y + ch, paint);
            }
            List<Occurrence> list = days.get(d);
            if (chips) {
                drawChipsCell(canvas, d, list, x, y, cw, ch, in, isToday, isSel);
            } else {
                drawDotsCell(canvas, d, list, x, y, cw, ch, in, isToday, isSel);
            }
        }

        // quadrillage
        paint.setColor(Ui.GRID);
        paint.setStrokeWidth(Math.max(1, Ui.dp(c, 0.7f)));
        canvas.drawLine(0, header, w, header, paint);
        for (int r = 1; r < 6; r++) canvas.drawLine(0, header + r * ch, w, header + r * ch, paint);
        if (chips) {
            for (int k = 1; k < 7; k++) canvas.drawLine(k * cw, header, k * cw, getHeight(), paint);
        }
    }

    /** Petit écran : numéro du jour entouré et points de couleur. */
    private void drawDotsCell(Canvas canvas, LocalDate d, List<Occurrence> list, float x, float y, float cw, float ch,
                              boolean in, boolean isToday, boolean isSel) {
        Context c = getContext();
        float cx = x + cw / 2;
        float r = Math.min(cw, ch) * 0.34f;
        float cy = y + Math.min(ch * 0.42f, r + Ui.dp(c, 6));
        if (isSel || isToday) {
            paint.setColor(isToday ? Ui.ACCENT : Ui.ACCENT_SOFT);
            canvas.drawCircle(cx, cy, r, paint);
        }
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(Ui.sp(c, 15));
        paint.setTypeface(isToday || isSel ? medium : Typeface.DEFAULT);
        paint.setColor(isToday ? 0xFFFFFFFF : isSel ? Ui.ACCENT : in ? Ui.TEXT : Ui.FAINT);
        canvas.drawText(String.valueOf(d.getDayOfMonth()), cx, cy - (paint.descent() + paint.ascent()) / 2, paint);
        if (list == null || list.isEmpty()) return;
        int n = Math.min(3, list.size());
        float dotR = Ui.dp(c, 2.6f);
        float gap = Ui.dp(c, 7);
        float dy = cy + r + Ui.dp(c, 5);
        if (dy > y + ch - dotR) dy = y + ch - dotR - 1;
        float startX = cx - (n - 1) * gap / 2;
        for (int k = 0; k < n; k++) {
            paint.setColor(in ? list.get(k).event.category.color : Ui.FAINT);
            canvas.drawCircle(startX + k * gap, dy, dotR, paint);
        }
        if (list.size() > 3) {
            paint.setColor(Ui.MUTED);
            paint.setTextSize(Ui.sp(c, 9));
            paint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("+", startX + n * gap - dotR, dy + Ui.dp(c, 3), paint);
        }
    }

    /** Grand écran (tablette, paysage) : titres des événements dans les cases. */
    private void drawChipsCell(Canvas canvas, LocalDate d, List<Occurrence> list, float x, float y, float cw, float ch,
                               boolean in, boolean isToday, boolean isSel) {
        Context c = getContext();
        if (isSel) {
            paint.setColor(Ui.ACCENT_SOFT);
            canvas.drawRect(x, y, x + cw, y + ch, paint);
        }
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(Ui.sp(c, 13));
        paint.setTypeface(isToday ? medium : Typeface.DEFAULT);
        String num = String.valueOf(d.getDayOfMonth());
        float nx = x + Ui.dp(c, 6);
        float ny = y + Ui.dp(c, 17);
        if (isToday) {
            float tw = Math.max(Ui.dp(c, 22), paint.measureText(num) + Ui.dp(c, 10));
            rect.set(nx - Ui.dp(c, 3), y + Ui.dp(c, 3), nx - Ui.dp(c, 3) + tw, y + Ui.dp(c, 22));
            paint.setColor(Ui.ACCENT);
            canvas.drawRoundRect(rect, rect.height() / 2, rect.height() / 2, paint);
            paint.setColor(0xFFFFFFFF);
            canvas.drawText(num, rect.centerX() - paint.measureText(num) / 2, ny, paint);
        } else {
            paint.setColor(in ? Ui.TEXT : Ui.FAINT);
            canvas.drawText(num, nx, ny, paint);
        }
        if (list == null || list.isEmpty()) return;
        float line = Ui.dp(c, 17);
        float top = y + Ui.dp(c, 26);
        int max = (int) ((y + ch - Ui.dp(c, 3) - top) / line);
        int shown = list.size() <= max ? list.size() : Math.max(0, max - 1);
        paint.setTextSize(Ui.sp(c, 11));
        for (int k = 0; k < shown; k++) {
            Occurrence o = list.get(k);
            float ty = top + k * line;
            rect.set(x + Ui.dp(c, 3), ty, x + cw - Ui.dp(c, 3), ty + line - Ui.dp(c, 2));
            boolean filled = o.event.allDay || o.isMultiDay();
            String label = o.event.displayTitle();
            if (filled) {
                paint.setColor(o.event.category.color);
                canvas.drawRoundRect(rect, Ui.dp(c, 4), Ui.dp(c, 4), paint);
                paint.setColor(0xFFFFFFFF);
            } else {
                paint.setColor(o.event.category.color);
                canvas.drawCircle(rect.left + Ui.dp(c, 4), rect.centerY(), Ui.dp(c, 3), paint);
                paint.setColor(Ui.TEXT);
                label = Fmt.shortTime(o.start.toLocalTime()) + " " + label;
            }
            float tx = rect.left + Ui.dp(c, filled ? 4 : 10);
            canvas.drawText(fit(label, rect.right - tx - Ui.dp(c, 2)), tx, rect.bottom - Ui.dp(c, 4), paint);
        }
        if (shown < list.size()) {
            paint.setColor(Ui.MUTED);
            canvas.drawText("+" + (list.size() - shown), x + Ui.dp(c, 6), top + shown * line + line - Ui.dp(c, 5), paint);
        }
    }

    private String fit(String s, float width) {
        if (paint.measureText(s) <= width) return s;
        int n = paint.breakText(s, true, width - paint.measureText("…"), null);
        return n <= 0 ? "" : s.substring(0, n).trim() + "…";
    }
}
