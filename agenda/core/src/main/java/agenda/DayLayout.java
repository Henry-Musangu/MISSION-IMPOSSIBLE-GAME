package agenda;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Place les événements d'une journée sur la grille horaire : ceux qui se chevauchent sont mis
 * côte à côte, en colonnes.
 */
public final class DayLayout {

    private DayLayout() {
    }

    /** Un bloc à dessiner. */
    public static final class Slot {
        public final Occurrence occurrence;
        /** Début et fin ramenés à la journée affichée. */
        public final LocalDateTime start;
        public final LocalDateTime end;
        public int column;
        public int columns = 1;
        final LocalDateTime visualEnd;

        Slot(Occurrence o, LocalDateTime start, LocalDateTime end, Duration minLength) {
            this.occurrence = o;
            this.start = start;
            this.end = end;
            LocalDateTime min = start.plus(minLength);
            this.visualEnd = end.isBefore(min) ? min : end;
        }

        /** Minutes depuis minuit. */
        public int startMinute(LocalDate day) {
            return (int) Duration.between(day.atStartOfDay(), start).toMinutes();
        }

        public int endMinute(LocalDate day) {
            return (int) Duration.between(day.atStartOfDay(), end).toMinutes();
        }
    }

    /**
     * @param occurrences les occurrences du jour (celles qui remplissent la journée entière sont ignorées)
     * @param minMinutes  hauteur minimale d'un bloc, en minutes (pour qu'un événement court reste lisible)
     */
    public static List<Slot> layout(List<Occurrence> occurrences, LocalDate day, int minMinutes) {
        LocalDateTime dayStart = day.atStartOfDay();
        LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();
        Duration min = Duration.ofMinutes(minMinutes);
        List<Slot> slots = new ArrayList<Slot>();
        for (Occurrence o : occurrences) {
            if (o.fillsDay(day) || !o.covers(day)) continue;
            LocalDateTime s = o.start.isBefore(dayStart) ? dayStart : o.start;
            LocalDateTime e = o.end.isAfter(dayEnd) ? dayEnd : o.end;
            if (s.plus(min).isAfter(dayEnd)) s = dayEnd.minus(min);   // bloc qui commence tard : on le remonte
            slots.add(new Slot(o, s, e, min));
        }
        Collections.sort(slots, new Comparator<Slot>() {
            @Override
            public int compare(Slot a, Slot b) {
                int c = a.start.compareTo(b.start);
                return c != 0 ? c : b.visualEnd.compareTo(a.visualEnd);
            }
        });
        List<Slot> cluster = new ArrayList<Slot>();
        List<LocalDateTime> columnEnds = new ArrayList<LocalDateTime>();
        LocalDateTime clusterEnd = null;
        for (Slot s : slots) {
            if (clusterEnd != null && !s.start.isBefore(clusterEnd)) {
                finish(cluster, columnEnds.size());
                cluster.clear();
                columnEnds.clear();
            }
            int col = -1;
            for (int i = 0; i < columnEnds.size(); i++) {
                if (!columnEnds.get(i).isAfter(s.start)) {
                    col = i;
                    break;
                }
            }
            if (col < 0) {
                col = columnEnds.size();
                columnEnds.add(s.visualEnd);
            } else {
                columnEnds.set(col, s.visualEnd);
            }
            s.column = col;
            cluster.add(s);
            clusterEnd = clusterEnd == null || cluster.size() == 1 || s.visualEnd.isAfter(clusterEnd) ? s.visualEnd : clusterEnd;
        }
        finish(cluster, columnEnds.size());
        return slots;
    }

    private static void finish(List<Slot> cluster, int columns) {
        for (Slot s : cluster) s.columns = Math.max(1, columns);
    }
}
