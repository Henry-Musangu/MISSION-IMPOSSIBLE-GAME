package agenda;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Un événement de l'agenda (rendez-vous, réunion, anniversaire...), éventuellement répété.
 * Les heures sont des heures locales « murales » (celles affichées sur l'horloge).
 */
public final class Event {

    /** Identifiant unique, conservé à l'export / import .ics. */
    public String uid;
    public String title = "";
    public String location = "";
    public String notes = "";
    public Category category = Category.PERSONNEL;
    /** Toute la journée : start et end sont alors à minuit, end exclu (comme DTEND en iCalendar). */
    public boolean allDay;
    public LocalDateTime start;
    /** Fin exclue ; égale à start pour un événement sans durée. */
    public LocalDateTime end;
    /** Règle de répétition, ou null pour un événement unique. */
    public Recurrence recurrence;
    /** Dates de répétitions supprimées. */
    public final TreeSet<LocalDate> exdates = new TreeSet<LocalDate>();
    /** Rappel : nombre de minutes avant le début, ou -1 pour aucun rappel. */
    public int reminder = -1;

    public Event() {
    }

    /** Événement minuté. */
    public Event(String title, LocalDateTime start, LocalDateTime end) {
        this.title = title;
        this.start = start;
        this.end = end;
    }

    /** Événement sur la journée entière, du jour {@code first} au jour {@code last} inclus. */
    public static Event allDay(String title, LocalDate first, LocalDate last) {
        Event e = new Event(title, first.atStartOfDay(), last.plusDays(1).atStartOfDay());
        e.allDay = true;
        return e;
    }

    public static String newUid() {
        return UUID.randomUUID().toString() + "@agenda";
    }

    public Event copy() {
        Event e = new Event();
        e.uid = uid;
        e.title = title;
        e.location = location;
        e.notes = notes;
        e.category = category;
        e.allDay = allDay;
        e.start = start;
        e.end = end;
        e.recurrence = recurrence == null ? null : recurrence.copy();
        e.exdates.addAll(exdates);
        e.reminder = reminder;
        return e;
    }

    public Duration duration() {
        return end.isBefore(start) ? Duration.ZERO : Duration.between(start, end);
    }

    public boolean isRecurring() {
        return recurrence != null;
    }

    /** Titre à afficher (jamais vide). */
    public String displayTitle() {
        String t = title == null ? "" : title.trim();
        return t.isEmpty() ? "(Sans titre)" : t;
    }

    /** Dernier jour touché par une occurrence qui commence le jour {@code date}. */
    public LocalDate lastDayFrom(LocalDate date) {
        LocalDateTime s = date.atTime(start.toLocalTime());
        LocalDateTime e = s.plus(duration());
        if (e.isAfter(s) && e.toLocalTime().equals(java.time.LocalTime.MIDNIGHT)) {
            return e.toLocalDate().minusDays(1);
        }
        return e.toLocalDate();
    }

    @Override
    public String toString() {
        return displayTitle() + " @ " + start;
    }
}
