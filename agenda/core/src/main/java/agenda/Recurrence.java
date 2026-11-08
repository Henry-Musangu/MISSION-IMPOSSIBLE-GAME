package agenda;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Règle de répétition d'un événement : un sous-ensemble de la règle RRULE de la norme iCalendar
 * (tous les jours / semaines / mois / ans, avec intervalle, jours de la semaine, fin à une date
 * ou après un nombre de fois).
 */
public final class Recurrence {

    public enum Freq { DAILY, WEEKLY, MONTHLY, YEARLY }

    public Freq freq = Freq.WEEKLY;
    /** Tous les « interval » jours / semaines / mois / ans (1 = chaque fois). */
    public int interval = 1;
    /**
     * WEEKLY : jours de la semaine concernés (vide = le jour de la semaine du début).
     * MONTHLY avec {@link #weekOfMonth} : le jour de la semaine (vide = celui du début).
     */
    public final EnumSet<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
    /** MONTHLY : 0 = même numéro de jour que le début ; 1 à 5 = n-ième jour de la semaine du mois ; -1 = le dernier. */
    public int weekOfMonth;
    /** Dernier jour possible (inclus), ou null. */
    public LocalDate until;
    /** Nombre total de répétitions (0 = illimité). */
    public int count;

    public Recurrence() {
    }

    public Recurrence(Freq freq) {
        this.freq = freq;
    }

    public Recurrence copy() {
        Recurrence r = new Recurrence(freq);
        r.interval = interval;
        r.days.addAll(days);
        r.weekOfMonth = weekOfMonth;
        r.until = until;
        r.count = count;
        return r;
    }

    /** Reçoit les dates une à une ; renvoie false pour arrêter le parcours. */
    public interface Visitor {
        boolean visit(LocalDate date);
    }

    /**
     * Parcourt dans l'ordre les dates de la répétition à partir de {@code start} (la date du premier
     * événement), jusqu'à {@code limit} exclue (null = jusqu'à la fin de la règle : il faut alors une
     * date de fin ou un nombre de fois, ou que le visiteur s'arrête lui-même).
     */
    public void forEach(LocalDate start, LocalDate limit, Visitor visitor) {
        new Walk(start, limit, visitor).run();
    }

    /** Les dates de la répétition entre start et limit exclue (pratique pour les tests). */
    public List<LocalDate> dates(LocalDate start, LocalDate limit) {
        final List<LocalDate> list = new ArrayList<LocalDate>();
        forEach(start, limit, new Visitor() {
            @Override
            public boolean visit(LocalDate date) {
                list.add(date);
                return true;
            }
        });
        return list;
    }

    /** La règle a-t-elle une fin (date ou nombre de fois) ? */
    public boolean isFinite() {
        return until != null || count > 0;
    }

    private final class Walk {
        final LocalDate start;
        final LocalDate limit;
        final Visitor visitor;
        int emitted;

        Walk(LocalDate start, LocalDate limit, Visitor visitor) {
            this.start = start;
            this.limit = limit;
            this.visitor = visitor;
        }

        /** Faux si la date est au-delà de la fin de la règle ou de la limite. */
        boolean inRange(LocalDate d) {
            return (limit == null || d.isBefore(limit)) && (until == null || !d.isAfter(until));
        }

        boolean emit(LocalDate d) {
            if (!inRange(d) || (count > 0 && emitted >= count)) {
                return false;
            }
            emitted++;
            return visitor.visit(d);
        }

        void run() {
            int step = Math.max(1, interval);
            int guard = 0;
            switch (freq) {
                case DAILY:
                    for (LocalDate d = start; guard++ < 500000; d = d.plusDays(step)) {
                        if (!emit(d)) return;
                    }
                    return;
                case WEEKLY: {
                    EnumSet<DayOfWeek> set = days.isEmpty() ? EnumSet.of(start.getDayOfWeek()) : days;
                    LocalDate week = start.minusDays(start.getDayOfWeek().getValue() - 1);
                    for (; guard++ < 100000; week = week.plusWeeks(step)) {
                        for (DayOfWeek dow : set) {   // EnumSet : du lundi au dimanche
                            LocalDate d = week.plusDays(dow.getValue() - 1);
                            if (d.isBefore(start)) continue;
                            if (!emit(d)) return;
                        }
                    }
                    return;
                }
                case MONTHLY: {
                    YearMonth first = YearMonth.from(start);
                    DayOfWeek dow = days.isEmpty() ? start.getDayOfWeek() : days.iterator().next();
                    for (int k = 0; guard++ < 50000; k += step) {
                        YearMonth ym = first.plusMonths(k);
                        if (!inRange(ym.atDay(1))) return;
                        LocalDate d;
                        if (weekOfMonth == 0) {
                            d = start.getDayOfMonth() <= ym.lengthOfMonth() ? ym.atDay(start.getDayOfMonth()) : null;
                        } else {
                            d = nthWeekday(ym, dow, weekOfMonth);
                        }
                        if (d == null || d.isBefore(start)) continue;
                        if (!emit(d)) return;
                    }
                    return;
                }
                case YEARLY:
                default: {
                    Month month = start.getMonth();
                    int day = start.getDayOfMonth();
                    for (int k = 0; guard++ < 20000; k += step) {
                        YearMonth ym = YearMonth.of(start.getYear() + k, month);
                        if (!inRange(ym.atDay(1))) return;
                        if (day > ym.lengthOfMonth()) continue;   // 29 février hors année bissextile
                        if (!emit(ym.atDay(day))) return;
                    }
                }
            }
        }
    }

    /** Le n-ième (1 à 5, ou -1 = dernier) jour de la semaine donné dans un mois, ou null s'il n'existe pas. */
    public static LocalDate nthWeekday(YearMonth ym, DayOfWeek dow, int nth) {
        if (nth < 0) {
            LocalDate d = ym.atEndOfMonth();
            while (d.getDayOfWeek() != dow) d = d.minusDays(1);
            return d;
        }
        LocalDate d = ym.atDay(1);
        while (d.getDayOfWeek() != dow) d = d.plusDays(1);
        d = d.plusWeeks(nth - 1);
        return d.getMonth() == ym.getMonth() ? d : null;
    }

    /** Rang du jour dans le mois pour son jour de semaine (le 2e mardi = 2). */
    public static int weekOfMonthOf(LocalDate d) {
        return (d.getDayOfMonth() - 1) / 7 + 1;
    }

    /** Vrai si c'est le dernier jour de ce nom dans le mois (le dernier vendredi...). */
    public static boolean isLastOfMonth(LocalDate d) {
        return d.plusWeeks(1).getMonth() != d.getMonth();
    }

    /** Description en français, par exemple « Toutes les 2 semaines le lundi et le jeudi, jusqu'au 3 mars 2027 ». */
    public String describe(LocalDate start) {
        int n = Math.max(1, interval);
        StringBuilder sb = new StringBuilder();
        switch (freq) {
            case DAILY:
                sb.append(n == 1 ? "Tous les jours" : "Tous les " + n + " jours");
                break;
            case WEEKLY: {
                EnumSet<DayOfWeek> set = days.isEmpty() ? EnumSet.of(start.getDayOfWeek()) : days;
                if (n == 1 && set.equals(EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))) {
                    sb.append("Du lundi au vendredi");
                    break;
                }
                if (n == 1 && set.size() == 7) {
                    sb.append("Tous les jours");
                    break;
                }
                sb.append(n == 1 ? "Toutes les semaines" : "Toutes les " + n + " semaines");
                List<String> names = new ArrayList<String>();
                for (DayOfWeek d : set) names.add(Fmt.DAYS[d.getValue() - 1]);
                sb.append(" le ").append(Fmt.joinFr(names, " et le "));
                break;
            }
            case MONTHLY:
                sb.append(n == 1 ? "Tous les mois" : "Tous les " + n + " mois");
                if (weekOfMonth == 0) {
                    sb.append(" le ").append(start.getDayOfMonth() == 1 ? "1er" : String.valueOf(start.getDayOfMonth()));
                } else {
                    DayOfWeek dow = days.isEmpty() ? start.getDayOfWeek() : days.iterator().next();
                    sb.append(" le ").append(Fmt.rank(weekOfMonth)).append(' ').append(Fmt.DAYS[dow.getValue() - 1]);
                }
                break;
            case YEARLY:
            default:
                sb.append(n == 1 ? "Tous les ans" : "Tous les " + n + " ans");
                sb.append(" le ").append(Fmt.dayMonth(start));
                break;
        }
        if (until != null) {
            sb.append(", jusqu'au ").append(Fmt.dayMonthYear(until));
        } else if (count > 0) {
            sb.append(", ").append(count).append(" fois");
        }
        return sb.toString();
    }
}
