package agenda;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * L'agenda : la liste des événements, et tout ce qu'on en tire (ce qui se passe tel jour,
 * les prochains rappels, la recherche) et les modifications (y compris d'une seule occurrence
 * d'un événement répété).
 */
public final class Agenda {

    /** Portée d'une modification ou d'une suppression sur un événement répété. */
    public enum Scope {
        /** Cette occurrence uniquement. */
        THIS,
        /** Cette occurrence et les suivantes. */
        FOLLOWING,
        /** Toutes les occurrences. */
        ALL
    }

    /** Le plus long rappel proposé : une semaine (en minutes). */
    public static final int MAX_REMINDER = 7 * 24 * 60;

    private final List<Event> events = new ArrayList<Event>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<Runnable>();

    // ------------------------------------------------------------------ événements

    public synchronized List<Event> events() {
        return new ArrayList<Event>(events);
    }

    public synchronized int size() {
        return events.size();
    }

    public synchronized Event find(String uid) {
        if (uid == null) return null;
        for (Event e : events) {
            if (uid.equals(e.uid)) return e;
        }
        return null;
    }

    /** Ajoute un événement (lui donne un identifiant s'il n'en a pas). */
    public void add(Event e) {
        synchronized (this) {
            if (e.uid == null || e.uid.isEmpty() || findIndex(e.uid) >= 0) {
                e.uid = Event.newUid();
            }
            events.add(e);
        }
        changed();
    }

    /** Remplace l'événement de même identifiant (ou l'ajoute s'il n'existe pas). */
    public void update(Event e) {
        synchronized (this) {
            int i = findIndex(e.uid);
            if (i >= 0) {
                events.set(i, e);
            } else {
                if (e.uid == null || e.uid.isEmpty()) e.uid = Event.newUid();
                events.add(e);
            }
        }
        changed();
    }

    public void remove(Event e) {
        synchronized (this) {
            int i = findIndex(e.uid);
            if (i >= 0) events.remove(i);
        }
        changed();
    }

    /** Remplace tout le contenu (chargement d'un fichier). */
    public void setAll(Collection<Event> list) {
        synchronized (this) {
            events.clear();
            events.addAll(list);
        }
        changed();
    }

    /**
     * Importe des événements : ceux dont l'identifiant existe déjà remplacent l'ancienne version.
     * Renvoie le nombre d'événements importés.
     */
    public int importAll(Collection<Event> list) {
        synchronized (this) {
            for (Event e : list) {
                int i = e.uid == null ? -1 : findIndex(e.uid);
                if (i >= 0) {
                    events.set(i, e);
                } else {
                    if (e.uid == null || e.uid.isEmpty()) e.uid = Event.newUid();
                    events.add(e);
                }
            }
        }
        changed();
        return list.size();
    }

    private int findIndex(String uid) {
        if (uid == null) return -1;
        for (int i = 0; i < events.size(); i++) {
            if (uid.equals(events.get(i).uid)) return i;
        }
        return -1;
    }

    // ------------------------------------------------------------------ écoute des changements

    /** Prévient à chaque modification (pour enregistrer le fichier, redessiner, reprogrammer les rappels). */
    public void addListener(Runnable r) {
        listeners.add(r);
    }

    public void removeListener(Runnable r) {
        listeners.remove(r);
    }

    private void changed() {
        for (Runnable r : listeners) r.run();
    }

    // ------------------------------------------------------------------ occurrences

    /** Les deux intervalles [s, e[ et [from, to[ se chevauchent-ils ? (un événement sans durée compte s'il commence dedans) */
    public static boolean overlaps(LocalDateTime s, LocalDateTime e, LocalDateTime from, LocalDateTime to) {
        if (!s.isBefore(to)) return false;
        if (e.isAfter(from)) return true;
        return !e.isAfter(s) && !s.isBefore(from);
    }

    /** Toutes les occurrences qui touchent l'intervalle [from, to[, triées. */
    public List<Occurrence> occurrences(LocalDateTime from, LocalDateTime to) {
        List<Occurrence> out = new ArrayList<Occurrence>();
        for (Event e : events()) {
            expand(e, from, to, out);
        }
        Collections.sort(out);
        return out;
    }

    /** Les occurrences d'un seul événement qui touchent [from, to[. */
    public static void expand(final Event e, final LocalDateTime from, final LocalDateTime to, final List<Occurrence> out) {
        if (e.start == null || e.end == null) return;
        if (e.recurrence == null) {
            if (overlaps(e.start, e.end, from, to)) out.add(new Occurrence(e, e.start, e.end));
            return;
        }
        final Duration duration = e.duration();
        final LocalTime time = e.start.toLocalTime();
        final LocalDate earliest = from.minus(duration).toLocalDate().minusDays(1);
        e.recurrence.forEach(e.start.toLocalDate(), to.toLocalDate().plusDays(1), new Recurrence.Visitor() {
            @Override
            public boolean visit(LocalDate d) {
                if (d.isBefore(earliest) || e.exdates.contains(d)) return true;
                LocalDateTime s = d.atTime(time);
                LocalDateTime en = s.plus(duration);
                if (overlaps(s, en, from, to)) out.add(new Occurrence(e, s, en));
                return true;
            }
        });
    }

    /** Ce qui se passe un jour donné, trié (journées entières d'abord, puis par heure). */
    public List<Occurrence> on(LocalDate day) {
        return occurrences(day.atStartOfDay(), day.plusDays(1).atStartOfDay());
    }

    /**
     * Le programme de {@code days} jours à partir de {@code first} : pour chaque jour, ce qui s'y passe
     * (un événement sur plusieurs jours apparaît chaque jour). Les jours vides sont présents, avec une liste vide.
     */
    public Map<LocalDate, List<Occurrence>> byDay(LocalDate first, int days) {
        Map<LocalDate, List<Occurrence>> map = new LinkedHashMap<LocalDate, List<Occurrence>>();
        for (int i = 0; i < days; i++) {
            map.put(first.plusDays(i), new ArrayList<Occurrence>());
        }
        for (Occurrence o : occurrences(first.atStartOfDay(), first.plusDays(days).atStartOfDay())) {
            LocalDate d = o.date().isBefore(first) ? first : o.date();
            LocalDate last = o.lastDay();
            for (; !d.isAfter(last); d = d.plusDays(1)) {
                List<Occurrence> list = map.get(d);
                if (list == null) break;
                list.add(o);
            }
        }
        return map;
    }

    /** L'occurrence d'un événement qui commence le jour {@code date}, ou null. */
    public static Occurrence occurrenceOn(Event e, LocalDate date) {
        List<Occurrence> list = new ArrayList<Occurrence>();
        expand(e, date.atStartOfDay(), date.plusDays(1).atStartOfDay(), list);
        for (Occurrence o : list) {
            if (o.date().equals(date)) return o;
        }
        return null;
    }

    /** La prochaine occurrence qui n'est pas terminée à l'instant {@code now} (cherchée sur 5 ans), ou null. */
    public static Occurrence next(Event e, LocalDateTime now) {
        List<Occurrence> list = new ArrayList<Occurrence>();
        // fenêtres de plus en plus grandes, pour ne pas tout dérouler quand c'est bientôt
        for (int days : new int[] {1, 40, 400, 5 * 366}) {
            LocalDateTime to = now.plusDays(days);
            list.clear();
            expand(e, now, to, list);
            if (!list.isEmpty()) {
                Collections.sort(list);
                for (Occurrence o : list) {
                    if (o.end.isAfter(now) || !o.start.isBefore(now)) return o;
                }
            }
        }
        return null;
    }

    /** La dernière occurrence d'un événement terminé (ou null si la répétition est sans fin). */
    public static Occurrence last(final Event e) {
        if (e.recurrence == null) return new Occurrence(e, e.start, e.end);
        if (!e.recurrence.isFinite()) return null;
        final LocalDate[] found = new LocalDate[1];
        e.recurrence.forEach(e.start.toLocalDate(), null, new Recurrence.Visitor() {
            @Override
            public boolean visit(LocalDate d) {
                if (!e.exdates.contains(d)) found[0] = d;
                return true;
            }
        });
        if (found[0] == null) return null;
        LocalDateTime s = found[0].atTime(e.start.toLocalTime());
        return new Occurrence(e, s, s.plus(e.duration()));
    }

    /** Reste-t-il au moins une occurrence ? */
    public static boolean hasOccurrences(final Event e) {
        if (e.recurrence == null) return true;
        if (!e.recurrence.isFinite()) return true;
        return last(e) != null;
    }

    // ------------------------------------------------------------------ recherche

    /**
     * Les événements dont le titre, le lieu, les notes ou la catégorie contiennent tous les mots de la
     * recherche (sans tenir compte des majuscules ni des accents). Pour chacun : sa prochaine occurrence,
     * ou la dernière s'il est passé. À venir d'abord (du plus proche au plus lointain), puis le passé
     * (du plus récent au plus ancien).
     */
    public List<Occurrence> search(String query, LocalDateTime now) {
        String[] words = Fmt.normalize(query).trim().split("\\s+");
        List<Occurrence> future = new ArrayList<Occurrence>();
        List<Occurrence> past = new ArrayList<Occurrence>();
        for (Event e : events()) {
            String text = Fmt.normalize(e.title + " " + e.location + " " + e.notes + " " + e.category.label);
            boolean ok = true;
            for (String w : words) {
                if (!w.isEmpty() && !text.contains(w)) {
                    ok = false;
                    break;
                }
            }
            if (!ok) continue;
            Occurrence o = next(e, now);
            if (o != null) {
                future.add(o);
            } else {
                o = last(e);
                if (o != null) past.add(o);
            }
        }
        Collections.sort(future, START_ORDER);
        Collections.sort(past, START_ORDER);
        Collections.reverse(past);
        future.addAll(past);
        return future;
    }

    private static final java.util.Comparator<Occurrence> START_ORDER = new java.util.Comparator<Occurrence>() {
        @Override
        public int compare(Occurrence a, Occurrence b) {
            int c = a.start.compareTo(b.start);
            return c != 0 ? c : a.compareTo(b);
        }
    };

    // ------------------------------------------------------------------ rappels

    /** Les rappels dont l'heure est dans ]after, upTo], triés. */
    public List<Reminder> remindersBetween(LocalDateTime after, LocalDateTime upTo) {
        List<Reminder> out = new ArrayList<Reminder>();
        if (!upTo.isAfter(after)) return out;
        for (Occurrence o : occurrences(after, upTo.plusMinutes(MAX_REMINDER + 1))) {
            if (o.event.reminder < 0 || o.isDone()) continue;
            LocalDateTime t = o.start.minusMinutes(o.event.reminder);
            if (t.isAfter(after) && !t.isAfter(upTo)) out.add(new Reminder(o, t));
        }
        Collections.sort(out);
        return out;
    }

    /** L'heure du prochain rappel strictement après {@code after} (cherché sur un an), ou null. */
    public LocalDateTime nextReminderTime(LocalDateTime after) {
        LocalDateTime best = null;
        for (int days : new int[] {2, 32, 370}) {
            for (Occurrence o : occurrences(after, after.plusDays(days))) {
                if (o.event.reminder < 0 || o.isDone()) continue;
                LocalDateTime t = o.start.minusMinutes(o.event.reminder);
                if (t.isAfter(after) && (best == null || t.isBefore(best))) best = t;
            }
            // un rappel trouvé dans cette fenêtre ne peut pas être battu par une occurrence plus lointaine
            // que la fenêtre moins le plus long rappel
            if (best != null && best.isBefore(after.plusDays(days).minusMinutes(MAX_REMINDER))) return best;
        }
        return best;
    }

    // ------------------------------------------------------------------ tâches

    /** Coche ou décoche une tâche (l'occurrence du jour {@code date}). */
    public void setDone(Event e, LocalDate date, boolean value) {
        synchronized (this) {
            if (value) e.done.add(date); else e.done.remove(date);
        }
        changed();
    }

    /** Les tâches pas faites des {@code days} derniers jours dont l'heure est passée, de la plus ancienne à la plus récente. */
    public List<Occurrence> overdue(LocalDateTime now, int days) {
        List<Occurrence> out = new ArrayList<Occurrence>();
        for (Event e : events()) {
            if (e.task) expand(e, now.minusDays(days), now, out);
        }
        for (Iterator<Occurrence> it = out.iterator(); it.hasNext(); ) {
            if (!it.next().isOverdue(now)) it.remove();
        }
        Collections.sort(out, START_ORDER);
        return out;
    }

    // ------------------------------------------------------------------ modifications d'événements répétés

    /**
     * Enregistre la modification d'une occurrence.
     *
     * @param original l'événement tel qu'il est dans l'agenda
     * @param date     la date de l'occurrence modifiée (sa date de début d'origine)
     * @param edited   le résultat du formulaire : une copie de l'original, avec les dates de cette occurrence
     * @param scope    pour un événement répété : cette occurrence, celle-ci et les suivantes, ou toutes
     * @return l'événement enregistré
     */
    public Event applyEdit(Event original, LocalDate date, Event edited, Scope scope) {
        if (!original.isRecurring() || scope == null) scope = Scope.ALL;
        if (scope == Scope.FOLLOWING && !date.isAfter(original.start.toLocalDate())) scope = Scope.ALL;
        Duration length = edited.duration();
        switch (scope) {
            case THIS: {
                original.exdates.add(date);
                original.done.remove(date);
                boolean wasDone = edited.done.contains(date);
                edited.uid = Event.newUid();
                edited.recurrence = null;
                edited.exdates.clear();
                edited.done.clear();
                if (wasDone && edited.task) edited.done.add(edited.start.toLocalDate());
                synchronized (this) {
                    int i = findIndex(original.uid);
                    if (i >= 0 && !hasOccurrences(original)) events.remove(i);
                    events.add(edited);
                }
                changed();
                return edited;
            }
            case FOLLOWING: {
                int before = countBefore(original, date);
                int originalCount = original.recurrence.count;
                truncate(original, date);
                edited.uid = Event.newUid();
                if (edited.recurrence != null && originalCount > 0 && edited.recurrence.count == originalCount) {
                    edited.recurrence.count = Math.max(1, originalCount - before);
                }
                for (Iterator<LocalDate> it = edited.exdates.iterator(); it.hasNext(); ) {
                    if (it.next().isBefore(date)) it.remove();
                }
                for (Iterator<LocalDate> it = edited.done.iterator(); it.hasNext(); ) {
                    if (it.next().isBefore(date)) it.remove();
                }
                synchronized (this) {
                    events.add(edited);
                }
                changed();
                return edited;
            }
            case ALL:
            default: {
                if (original.isRecurring() && edited.isRecurring()) {
                    // on déplace toute la série du même décalage que cette occurrence
                    // (si la répétition est retirée, l'événement reste à la date de cette occurrence)
                    LocalDateTime occStart = date.atTime(original.start.toLocalTime());
                    Duration shift = Duration.between(occStart, edited.start);
                    edited.start = original.start.plus(shift);
                    edited.end = edited.start.plus(length);
                    long days = edited.start.toLocalDate().toEpochDay() - original.start.toLocalDate().toEpochDay();
                    if (days != 0) {
                        java.util.TreeSet<LocalDate> moved = new java.util.TreeSet<LocalDate>();
                        for (LocalDate d : edited.done) moved.add(d.plusDays(days));
                        edited.done.clear();
                        edited.done.addAll(moved);
                    }
                }
                edited.uid = original.uid;
                update(edited);
                return edited;
            }
        }
    }

    /** Supprime une occurrence, celle-ci et les suivantes, ou tout l'événement. */
    public void delete(Event e, LocalDate date, Scope scope) {
        if (!e.isRecurring() || scope == null || scope == Scope.ALL
                || (scope == Scope.FOLLOWING && !date.isAfter(e.start.toLocalDate()))) {
            remove(e);
            return;
        }
        synchronized (this) {
            if (scope == Scope.THIS) {
                e.exdates.add(date);
            } else {
                truncate(e, date);
            }
            if (!hasOccurrences(e)) {
                int i = findIndex(e.uid);
                if (i >= 0) events.remove(i);
            }
        }
        changed();
    }

    /** Arrête la répétition la veille de {@code date}. */
    private static void truncate(Event e, LocalDate date) {
        e.recurrence.until = date.minusDays(1);
        e.recurrence.count = 0;
        for (Iterator<LocalDate> it = e.exdates.iterator(); it.hasNext(); ) {
            if (!it.next().isBefore(date)) it.remove();
        }
        for (Iterator<LocalDate> it = e.done.iterator(); it.hasNext(); ) {
            if (!it.next().isBefore(date)) it.remove();
        }
    }

    /** Nombre de dates générées par la règle avant {@code date} (y compris les dates supprimées, comme COUNT). */
    private static int countBefore(Event e, final LocalDate date) {
        final int[] n = new int[1];
        e.recurrence.forEach(e.start.toLocalDate(), date, new Recurrence.Visitor() {
            @Override
            public boolean visit(LocalDate d) {
                n[0]++;
                return true;
            }
        });
        return n[0];
    }
}
