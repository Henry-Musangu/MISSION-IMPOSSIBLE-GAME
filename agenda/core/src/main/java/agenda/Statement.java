package agenda;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Relevé d'activités sur une période, à la manière d'un relevé bancaire : la liste chronologique
 * de tout ce qui était prévu (rendez-vous et tâches), avec l'état de chaque ligne et des totaux.
 */
public final class Statement {

    /** État d'une ligne du relevé. */
    public enum Status {
        DONE("Fait"),
        OVERDUE("En retard"),
        TODO("À faire"),
        PAST("Passé"),
        UPCOMING("À venir");

        public final String label;

        Status(String label) {
            this.label = label;
        }
    }

    /** Une ligne : une occurrence, son état et sa durée comptée dans la période. */
    public static final class Line {
        public final Occurrence occurrence;
        public final Status status;
        /** Minutes comptées dans la période (0 pour une journée entière ou une tâche). */
        public final long minutes;
        /** Numéro de la ligne (1, 2, 3...). */
        public final int number;

        Line(Occurrence occurrence, Status status, long minutes, int number) {
            this.occurrence = occurrence;
            this.status = status;
            this.minutes = minutes;
            this.number = number;
        }
    }

    /** Premier et dernier jour (inclus). */
    public final LocalDate from;
    public final LocalDate to;
    /** Moment où le relevé a été établi. */
    public final LocalDateTime issued;
    public final List<Line> lines;

    public int appointments;
    public int tasks;
    public int done;
    public int overdue;
    public int todo;
    /** Durée totale des rendez-vous à heure fixe, en minutes. */
    public long minutes;
    public final Map<Category, Integer> countByCategory = new EnumMap<Category, Integer>(Category.class);
    public final Map<Category, Long> minutesByCategory = new EnumMap<Category, Long>(Category.class);

    private Statement(LocalDate from, LocalDate to, LocalDateTime issued, List<Line> lines) {
        this.from = from;
        this.to = to;
        this.issued = issued;
        this.lines = lines;
    }

    /** Établit le relevé de la période [from, to] (jours inclus). */
    public static Statement build(Agenda agenda, LocalDate from, LocalDate to, LocalDateTime now) {
        if (to.isBefore(from)) {
            LocalDate t = from;
            from = to;
            to = t;
        }
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        List<Occurrence> list = agenda.occurrences(start, end);
        Collections.sort(list, new Comparator<Occurrence>() {
            @Override
            public int compare(Occurrence a, Occurrence b) {
                int c = a.start.compareTo(b.start);
                return c != 0 ? c : a.compareTo(b);
            }
        });
        List<Line> lines = new ArrayList<Line>();
        Statement st = new Statement(from, to, now, lines);
        for (Category c : Category.values()) {
            st.countByCategory.put(c, 0);
            st.minutesByCategory.put(c, 0L);
        }
        int n = 0;
        for (Occurrence o : list) {
            Status status;
            long min = 0;
            if (o.event.task) {
                st.tasks++;
                if (o.isDone()) {
                    status = Status.DONE;
                    st.done++;
                } else if (o.start.isBefore(now)) {
                    status = Status.OVERDUE;
                    st.overdue++;
                } else {
                    status = Status.TODO;
                    st.todo++;
                }
            } else {
                st.appointments++;
                status = o.end.isAfter(now) || (o.end.equals(o.start) && !o.start.isBefore(now)) ? Status.UPCOMING : Status.PAST;
                if (!o.event.allDay) {
                    LocalDateTime s = o.start.isBefore(start) ? start : o.start;
                    LocalDateTime e = o.end.isAfter(end) ? end : o.end;
                    min = Math.max(0, Duration.between(s, e).toMinutes());
                }
            }
            st.minutes += min;
            st.countByCategory.put(o.event.category, st.countByCategory.get(o.event.category) + 1);
            st.minutesByCategory.put(o.event.category, st.minutesByCategory.get(o.event.category) + min);
            lines.add(new Line(o, status, min, ++n));
        }
        return st;
    }

    /** Pourcentage de tâches faites parmi celles dont l'heure est passée ou qui sont faites (0 à 100), ou -1 s'il n'y en a pas. */
    public int completionRate() {
        int base = done + overdue;
        return base == 0 ? -1 : Math.round(done * 100f / base);
    }

    /** « 3 h 15 », « 45 min », « 0 min ». */
    public static String duration(long minutes) {
        if (minutes < 60) return minutes + " min";
        long h = minutes / 60;
        long m = minutes % 60;
        return m == 0 ? h + " h" : h + " h " + (m < 10 ? "0" : "") + m;
    }

    /** « du 1er au 30 novembre 2026 », « du 28 déc. 2026 au 3 janv. 2027 », « le 8 novembre 2026 ». */
    public String periodLabel() {
        if (from.equals(to)) return "le " + Fmt.dayMonthYear(from);
        if (from.getYear() != to.getYear()) return "du " + Fmt.compactDate(from) + " au " + Fmt.compactDate(to);
        if (from.getMonth() == to.getMonth()) {
            return "du " + (from.getDayOfMonth() == 1 ? "1er" : String.valueOf(from.getDayOfMonth())) + " au " + Fmt.dayMonthYear(to);
        }
        return "du " + Fmt.dayMonth(from) + " au " + Fmt.dayMonthYear(to);
    }
}
