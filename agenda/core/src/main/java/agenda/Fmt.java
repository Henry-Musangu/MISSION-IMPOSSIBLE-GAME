package agenda;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

/** Mise en forme des dates et heures en français (identique sur PC et sur Android). */
public final class Fmt {

    private Fmt() {
    }

    public static final String[] MONTHS = {"janvier", "février", "mars", "avril", "mai", "juin", "juillet",
            "août", "septembre", "octobre", "novembre", "décembre"};
    public static final String[] MONTHS_SHORT = {"janv.", "févr.", "mars", "avr.", "mai", "juin", "juil.",
            "août", "sept.", "oct.", "nov.", "déc."};
    public static final String[] DAYS = {"lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche"};
    public static final String[] DAYS_SHORT = {"lun.", "mar.", "mer.", "jeu.", "ven.", "sam.", "dim."};
    public static final String[] DAYS_LETTER = {"L", "M", "M", "J", "V", "S", "D"};

    /** Durées de rappel proposées (minutes avant le début ; -1 = aucun). */
    public static final int[] REMINDERS = {-1, 0, 5, 10, 15, 30, 60, 120, 24 * 60, 2 * 24 * 60, 7 * 24 * 60};

    public static String capitalize(String s) {
        return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** « dimanche 8 novembre 2026 » */
    public static String longDate(LocalDate d) {
        return DAYS[d.getDayOfWeek().getValue() - 1] + " " + dayMonthYear(d);
    }

    /** « dimanche 8 novembre » (l'année seulement si ce n'est pas l'année en cours). */
    public static String longDateNoYear(LocalDate d, LocalDate today) {
        String s = DAYS[d.getDayOfWeek().getValue() - 1] + " " + dayMonth(d);
        return d.getYear() == today.getYear() ? s : s + " " + d.getYear();
    }

    /** « dim. 8 nov. » */
    public static String shortDate(LocalDate d) {
        return DAYS_SHORT[d.getDayOfWeek().getValue() - 1] + " " + d.getDayOfMonth() + " " + MONTHS_SHORT[d.getMonthValue() - 1];
    }

    /** « 8 novembre » (« 1er novembre ») */
    public static String dayMonth(LocalDate d) {
        return dayNumber(d) + " " + MONTHS[d.getMonthValue() - 1];
    }

    /** « 8 novembre 2026 » */
    public static String dayMonthYear(LocalDate d) {
        return dayMonth(d) + " " + d.getYear();
    }

    /** « 8 nov. 2026 » */
    public static String compactDate(LocalDate d) {
        return d.getDayOfMonth() + " " + MONTHS_SHORT[d.getMonthValue() - 1] + " " + d.getYear();
    }

    private static String dayNumber(LocalDate d) {
        return d.getDayOfMonth() == 1 ? "1er" : String.valueOf(d.getDayOfMonth());
    }

    /** « Novembre 2026 » */
    public static String monthTitle(YearMonth ym) {
        return capitalize(MONTHS[ym.getMonthValue() - 1]) + " " + ym.getYear();
    }

    /** Titre d'une semaine : « 9 – 15 novembre 2026 », « 30 nov. – 6 déc. 2026 », « 28 déc. 2026 – 3 janv. 2027 ». */
    public static String weekTitle(LocalDate monday) {
        LocalDate sunday = monday.plusDays(6);
        if (monday.getMonth() == sunday.getMonth()) {
            return monday.getDayOfMonth() + " – " + dayMonthYear(sunday);
        }
        String a = monday.getDayOfMonth() + " " + MONTHS_SHORT[monday.getMonthValue() - 1];
        if (monday.getYear() != sunday.getYear()) a += " " + monday.getYear();
        return a + " – " + compactDate(sunday);
    }

    /** « Aujourd'hui », « Demain », « Hier », sinon null. */
    public static String relativeDay(LocalDate d, LocalDate today) {
        if (d.equals(today)) return "Aujourd'hui";
        if (d.equals(today.plusDays(1))) return "Demain";
        if (d.equals(today.minusDays(1))) return "Hier";
        return null;
    }

    /** Titre d'un jour dans une liste : « Aujourd'hui · dimanche 8 novembre ». */
    public static String dayHeading(LocalDate d, LocalDate today) {
        String rel = relativeDay(d, today);
        String date = longDateNoYear(d, today);
        return rel == null ? capitalize(date) : rel + " · " + date;
    }

    /** « 09:30 » */
    public static String time(LocalTime t) {
        return String.format(Locale.ROOT, "%02d:%02d", t.getHour(), t.getMinute());
    }

    public static String time(LocalDateTime t) {
        return time(t.toLocalTime());
    }

    /** Heure compacte pour les petites cases : « 9h », « 9h30 ». */
    public static String shortTime(LocalTime t) {
        return t.getMinute() == 0 ? t.getHour() + "h" : t.getHour() + "h" + String.format(Locale.ROOT, "%02d", t.getMinute());
    }

    /** Quand a lieu l'occurrence, de façon complète : « 14:00 – 15:30 », « Toute la journée », « Du 8 au 10 nov. »... */
    public static String when(Occurrence o) {
        LocalDate first = o.date();
        LocalDate last = o.lastDay();
        if (o.event.allDay) {
            if (last.equals(first)) return "Toute la journée";
            return "Du " + rangeDates(first, last);
        }
        if (o.end.equals(o.start)) return time(o.start);
        if (last.equals(first)) return time(o.start) + " – " + time(o.end);
        return shortDate(first) + " " + time(o.start) + " → " + shortDate(o.end.toLocalDate()) + " " + time(o.end);
    }

    /** « 8 au 10 nov. », « 30 nov. au 2 déc. » */
    private static String rangeDates(LocalDate a, LocalDate b) {
        if (a.getYear() != b.getYear()) return compactDate(a) + " au " + compactDate(b);
        if (a.getMonth() == b.getMonth()) return a.getDayOfMonth() + " au " + b.getDayOfMonth() + " " + MONTHS_SHORT[b.getMonthValue() - 1];
        return a.getDayOfMonth() + " " + MONTHS_SHORT[a.getMonthValue() - 1] + " au " + b.getDayOfMonth() + " " + MONTHS_SHORT[b.getMonthValue() - 1];
    }

    /** Quand a lieu l'occurrence, vu depuis un jour donné : « 14:00 – 15:30 », « Jusqu'à 10:00 », « À partir de 22:00 »... */
    public static String whenOn(Occurrence o, LocalDate day) {
        if (!o.isMultiDay()) return when(o);
        if (o.fillsDay(day)) {
            return o.event.allDay ? "Toute la journée (jour " + (day.toEpochDay() - o.date().toEpochDay() + 1)
                    + "/" + (o.lastDay().toEpochDay() - o.date().toEpochDay() + 1) + ")" : "Toute la journée";
        }
        if (o.date().equals(day)) return "À partir de " + time(o.start);
        return "Jusqu'à " + time(o.end);
    }

    /** Libellé d'un rappel : « 15 minutes avant », « 1 jour avant »... */
    public static String reminder(int minutes) {
        if (minutes < 0) return "Aucun rappel";
        if (minutes == 0) return "Au début de l'événement";
        if (minutes % (7 * 24 * 60) == 0) return plural(minutes / (7 * 24 * 60), "semaine") + " avant";
        if (minutes % (24 * 60) == 0) return plural(minutes / (24 * 60), "jour") + " avant";
        if (minutes % 60 == 0) return plural(minutes / 60, "heure") + " avant";
        return plural(minutes, "minute") + " avant";
    }

    public static String plural(int n, String word) {
        return n + " " + word + (n > 1 ? "s" : "");
    }

    /** « 1er », « 2e », « dernier » */
    public static String rank(int n) {
        if (n < 0) return "dernier";
        return n == 1 ? "1er" : n + "e";
    }

    /** « a, b et c » */
    public static String joinFr(List<String> items, String lastSeparator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(i == items.size() - 1 ? lastSeparator : ", ");
            sb.append(items.get(i));
        }
        return sb.toString();
    }

    /** Minuscules sans accents, pour comparer et chercher. */
    public static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD);
        StringBuilder sb = new StringBuilder(n.length());
        for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            if (Character.getType(c) != Character.NON_SPACING_MARK) sb.append(c);
        }
        return sb.toString().toLowerCase(Locale.ROOT);
    }
}
