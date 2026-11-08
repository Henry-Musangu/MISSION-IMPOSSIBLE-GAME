package agenda;

import java.time.LocalDateTime;

/** Un rappel à afficher : l'occurrence concernée et l'heure à laquelle prévenir. */
public final class Reminder implements Comparable<Reminder> {

    public final Occurrence occurrence;
    public final LocalDateTime time;

    public Reminder(Occurrence occurrence, LocalDateTime time) {
        this.occurrence = occurrence;
        this.time = time;
    }

    /** Identifiant stable (pour ne pas notifier deux fois, ou numéroter une notification). */
    public String key() {
        return occurrence.event.uid + "|" + occurrence.start;
    }

    @Override
    public int compareTo(Reminder o) {
        int c = time.compareTo(o.time);
        return c != 0 ? c : occurrence.compareTo(o.occurrence);
    }
}
