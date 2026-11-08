package agenda;

/** Catégorie d'un événement : donne sa couleur dans le calendrier. */
public enum Category {
    PERSONNEL("Personnel", 0xFF3B82F6),
    TRAVAIL("Travail", 0xFF7C3AED),
    FAMILLE("Famille", 0xFFF59E0B),
    SANTE("Santé", 0xFF10B981),
    LOISIRS("Loisirs", 0xFFEC4899),
    ANNIVERSAIRE("Anniversaire", 0xFFEF4444),
    AUTRE("Autre", 0xFF64748B);

    /** Nom affiché (et écrit dans CATEGORIES du fichier .ics). */
    public final String label;
    /** Couleur ARGB. */
    public final int color;

    Category(String label, int color) {
        this.label = label;
        this.color = color;
    }

    /** Retrouve une catégorie à partir de son nom (ou d'un synonyme courant, en français ou en anglais). */
    public static Category fromLabel(String text) {
        if (text == null || text.trim().isEmpty()) {
            return PERSONNEL;
        }
        String s = Fmt.normalize(text);
        for (Category c : values()) {
            if (Fmt.normalize(c.label).equals(s)) {
                return c;
            }
        }
        if (s.startsWith("perso")) return PERSONNEL;
        if (s.startsWith("work") || s.startsWith("bureau") || s.startsWith("pro") || s.startsWith("business")) return TRAVAIL;
        if (s.startsWith("famil")) return FAMILLE;
        if (s.startsWith("health") || s.startsWith("medic") || s.startsWith("sport")) return SANTE;
        if (s.startsWith("loisir") || s.startsWith("leisure") || s.startsWith("holiday") || s.startsWith("vacance")) return LOISIRS;
        if (s.startsWith("birthday") || s.startsWith("annif")) return ANNIVERSAIRE;
        return AUTRE;
    }

    /** Version plus claire de la couleur (fond des blocs) : mélange avec du blanc. */
    public int light(double amount) {
        return mix(color, 0xFFFFFFFF, amount);
    }

    /** Mélange deux couleurs ARGB opaques (t = 0 : a, t = 1 : b). */
    public static int mix(int a, int b, double t) {
        int r = (int) Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }
}
