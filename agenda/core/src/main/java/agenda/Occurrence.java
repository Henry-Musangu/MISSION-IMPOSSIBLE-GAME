package agenda;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Une apparition concrète d'un événement dans le calendrier (un événement répété en a plusieurs). */
public final class Occurrence implements Comparable<Occurrence> {

    public final Event event;
    public final LocalDateTime start;
    public final LocalDateTime end;

    public Occurrence(Event event, LocalDateTime start, LocalDateTime end) {
        this.event = event;
        this.start = start;
        this.end = end;
    }

    /** Date de début : sert à désigner cette occurrence (pour la modifier ou la supprimer). */
    public LocalDate date() {
        return start.toLocalDate();
    }

    /** L'occurrence touche-t-elle ce jour ? */
    public boolean covers(LocalDate day) {
        return Agenda.overlaps(start, end, day.atStartOfDay(), day.plusDays(1).atStartOfDay());
    }

    /** Occupe-t-elle ce jour en entier (journée entière, ou jour du milieu d'un événement sur plusieurs jours) ? */
    public boolean fillsDay(LocalDate day) {
        return event.allDay || (!start.isAfter(day.atStartOfDay()) && !end.isBefore(day.plusDays(1).atStartOfDay()));
    }

    /** Une tâche cochée comme faite ? */
    public boolean isDone() {
        return event.isDone(date());
    }

    /** Une tâche pas faite dont l'heure est passée ? */
    public boolean isOverdue(LocalDateTime now) {
        return event.task && !isDone() && start.isBefore(now);
    }

    /** S'étend-elle sur plusieurs jours ? */
    public boolean isMultiDay() {
        return event.lastDayFrom(date()).isAfter(date());
    }

    /** Dernier jour touché. */
    public LocalDate lastDay() {
        return event.lastDayFrom(date());
    }

    @Override
    public int compareTo(Occurrence o) {
        if (event.allDay != o.event.allDay) {
            return event.allDay ? -1 : 1;
        }
        int c = start.compareTo(o.start);
        if (c != 0) return c;
        c = o.end.compareTo(end);   // le plus long d'abord
        if (c != 0) return c;
        return event.displayTitle().compareToIgnoreCase(o.event.displayTitle());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Occurrence)) return false;
        Occurrence o = (Occurrence) obj;
        return o.event == event && o.start.equals(start);
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(event) * 31 + start.hashCode();
    }

    @Override
    public String toString() {
        return event.displayTitle() + " " + start + " → " + end;
    }
}
