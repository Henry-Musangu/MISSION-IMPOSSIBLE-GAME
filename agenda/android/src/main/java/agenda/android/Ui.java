package agenda.android;

import agenda.Category;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Couleurs et petits outils pour construire l'interface par le code. */
final class Ui {

    private Ui() {
    }

    static final int BG = 0xFFF4F6FA;
    static final int SURFACE = 0xFFFFFFFF;
    static final int BORDER = 0xFFE2E8F0;
    static final int GRID = 0xFFE9EDF3;
    static final int TEXT = 0xFF0F172A;
    static final int MUTED = 0xFF64748B;
    static final int FAINT = 0xFFA7B1C2;
    static final int ACCENT = 0xFF2563EB;
    static final int ACCENT_SOFT = 0xFFEAF1FE;
    static final int OUTSIDE = 0xFFF7F8FB;
    static final int DANGER = 0xFFDC2626;
    static final int SUCCESS = 0xFF16A34A;
    /** Couleurs du logo HM : noir, or, et le fond crème du logo. */
    static final int BRAND = 0xFF1A1A1A;
    static final int GOLD = 0xFFC9962E;
    static final int GOLD_DARK = 0xFF9A7020;
    static final int CREAM = 0xFFF4F3F1;

    static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    static float sp(Context c, float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, c.getResources().getDisplayMetrics());
    }

    static TextView text(Context c, String s, float sizeSp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return t;
    }

    /** Fond arrondi. */
    static GradientDrawable round(int fill, float radiusPx, int strokeColor, int strokePx) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(radiusPx);
        if (strokePx > 0) d.setStroke(strokePx, strokeColor);
        return d;
    }

    /** Pastille ronde de la couleur d'une catégorie. */
    static GradientDrawable dot(Context c, Category cat, int sizeDp) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(cat.color);
        int s = dp(c, sizeDp);
        d.setSize(s, s);
        d.setBounds(0, 0, s, s);
        return d;
    }

    static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static View space(Context c, int wDp, int hDp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(c, wDp), dp(c, hDp)));
        return v;
    }

    /** Le logo aux coins arrondis, à la taille voulue (pour la barre du haut). */
    static android.graphics.drawable.Drawable roundedLogo(Context c, int sizeDp, int marginRightDp) {
        int size = dp(c, sizeDp);
        int margin = dp(c, marginRightDp);
        android.graphics.Bitmap src = android.graphics.BitmapFactory.decodeResource(c.getResources(), R.drawable.logo);
        android.graphics.Bitmap out = android.graphics.Bitmap.createBitmap(size + margin, size, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(out);
        android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG | android.graphics.Paint.FILTER_BITMAP_FLAG);
        android.graphics.RectF r = new android.graphics.RectF(0, 0, size, size);
        canvas.drawRoundRect(r, size * 0.22f, size * 0.22f, p);
        p.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(src, null, r, p);
        return new android.graphics.drawable.BitmapDrawable(c.getResources(), out);
    }

    static int darker(int color, double amount) {
        return Category.mix(color, Color.BLACK, amount);
    }
}
