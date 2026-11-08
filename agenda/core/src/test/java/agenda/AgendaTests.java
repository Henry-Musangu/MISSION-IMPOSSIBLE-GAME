package agenda;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Tests du cœur de l'agenda, sans bibliothèque : lancer avec
 * {@code java -cp out agenda.AgendaTests} (code de sortie 1 en cas d'échec).
 */
public final class AgendaTests {

    private static int run;
    private static int failed;

    public static void main(String[] args) {
        recurrenceDaily();
        recurrenceWeeklyDays();
        recurrenceMonthly31();
        recurrenceMonthlyNthWeekday();
        recurrenceYearlyLeap();
        recurrenceUntilAndCount();
        occurrencesAndExdates();
        multiDay();
        byDay();
        editThis();
        editFollowing();
        editAll();
        deleteScopes();
        reminders();
        search();
        icsRoundTrip();
        icsFolding();
        icsImportForeign();
        dayLayout();
        describe();
        tasks();
        statement();
        System.out.println(run + " vérifications, " + failed + " échec(s)");
        if (failed > 0) System.exit(1);
    }

    // ------------------------------------------------------------------ outils

    private static void check(boolean ok, String what) {
        run++;
        if (!ok) {
            failed++;
            System.out.println("ÉCHEC : " + what);
        }
    }

    private static void eq(Object expected, Object actual, String what) {
        check(expected == null ? actual == null : expected.equals(actual), what + " — attendu <" + expected + "> obtenu <" + actual + ">");
    }

    private static LocalDate d(int y, int m, int day) {
        return LocalDate.of(y, m, day);
    }

    private static LocalDateTime t(int y, int m, int day, int h, int min) {
        return LocalDateTime.of(y, m, day, h, min);
    }

    private static Event weekly(String title, LocalDateTime start, int minutes) {
        Event e = new Event(title, start, start.plusMinutes(minutes));
        e.recurrence = new Recurrence(Recurrence.Freq.WEEKLY);
        return e;
    }

    // ------------------------------------------------------------------ répétitions

    private static void recurrenceDaily() {
        Recurrence r = new Recurrence(Recurrence.Freq.DAILY);
        r.interval = 3;
        eq(Arrays.asList(d(2026, 1, 30), d(2026, 2, 2), d(2026, 2, 5)), r.dates(d(2026, 1, 30), d(2026, 2, 7)), "tous les 3 jours");
    }

    private static void recurrenceWeeklyDays() {
        Recurrence r = new Recurrence(Recurrence.Freq.WEEKLY);
        r.days.add(DayOfWeek.MONDAY);
        r.days.add(DayOfWeek.THURSDAY);
        r.interval = 2;
        // départ un mercredi 4 novembre 2026 : le lundi 2 est avant le début, on commence au jeudi 5
        eq(Arrays.asList(d(2026, 11, 5), d(2026, 11, 16), d(2026, 11, 19), d(2026, 11, 30)),
                r.dates(d(2026, 11, 4), d(2026, 12, 1)), "lundi et jeudi toutes les 2 semaines");
    }

    private static void recurrenceMonthly31() {
        Recurrence r = new Recurrence(Recurrence.Freq.MONTHLY);
        eq(Arrays.asList(d(2027, 1, 31), d(2027, 3, 31), d(2027, 5, 31)), r.dates(d(2027, 1, 31), d(2027, 6, 1)),
                "le 31 : les mois trop courts sont sautés");
    }

    private static void recurrenceMonthlyNthWeekday() {
        Recurrence r = new Recurrence(Recurrence.Freq.MONTHLY);
        r.weekOfMonth = -1;   // dernier vendredi
        eq(Arrays.asList(d(2026, 11, 27), d(2026, 12, 25), d(2027, 1, 29)), r.dates(d(2026, 11, 27), d(2027, 2, 1)), "dernier vendredi");
        Recurrence r2 = new Recurrence(Recurrence.Freq.MONTHLY);
        r2.weekOfMonth = 2;   // 2e mardi
        eq(Arrays.asList(d(2026, 11, 10), d(2026, 12, 8), d(2027, 1, 12)), r2.dates(d(2026, 11, 10), d(2027, 2, 1)), "2e mardi");
        eq(Integer.valueOf(2), Integer.valueOf(Recurrence.weekOfMonthOf(d(2026, 11, 10))), "rang du 10 novembre");
        check(Recurrence.isLastOfMonth(d(2026, 11, 27)), "27 novembre 2026 = dernier vendredi");
    }

    private static void recurrenceYearlyLeap() {
        Recurrence r = new Recurrence(Recurrence.Freq.YEARLY);
        eq(Arrays.asList(d(2024, 2, 29), d(2028, 2, 29)), r.dates(d(2024, 2, 29), d(2029, 1, 1)), "29 février");
    }

    private static void recurrenceUntilAndCount() {
        Recurrence r = new Recurrence(Recurrence.Freq.DAILY);
        r.until = d(2026, 3, 3);
        eq(3, r.dates(d(2026, 3, 1), null).size(), "jusqu'au 3 mars inclus");
        Recurrence c = new Recurrence(Recurrence.Freq.WEEKLY);
        c.count = 4;
        eq(4, c.dates(d(2026, 3, 1), null).size(), "4 fois");
        eq(d(2026, 3, 22), c.dates(d(2026, 3, 1), null).get(3), "4e fois");
    }

    private static void occurrencesAndExdates() {
        Agenda a = new Agenda();
        Event e = weekly("Piscine", t(2026, 11, 2, 18, 0), 60);
        e.exdates.add(d(2026, 11, 16));
        a.add(e);
        List<Occurrence> list = a.occurrences(t(2026, 11, 1, 0, 0), t(2026, 12, 1, 0, 0));
        eq(4, list.size(), "5 lundis en novembre moins une exception");
        eq(t(2026, 11, 23, 18, 0), list.get(2).start, "3e occurrence");
        eq(t(2026, 11, 23, 19, 0), list.get(2).end, "fin de la 3e occurrence");
        check(a.on(d(2026, 11, 16)).isEmpty(), "rien le jour de l'exception");
        // fenêtre qui commence au milieu d'une occurrence
        eq(1, a.occurrences(t(2026, 11, 9, 18, 30), t(2026, 11, 9, 18, 45)).size(), "occurrence en cours");
        eq(0, a.occurrences(t(2026, 11, 9, 19, 0), t(2026, 11, 9, 20, 0)).size(), "fin exclue");
    }

    private static void multiDay() {
        Agenda a = new Agenda();
        Event trip = Event.allDay("Voyage", d(2026, 11, 20), d(2026, 11, 22));
        a.add(trip);
        Event night = new Event("Garde", t(2026, 11, 24, 22, 0), t(2026, 11, 25, 6, 0));
        a.add(night);
        check(a.on(d(2026, 11, 19)).isEmpty(), "pas la veille");
        eq(1, a.on(d(2026, 11, 22)).size(), "dernier jour inclus");
        check(a.on(d(2026, 11, 23)).isEmpty(), "fin exclue (DTEND)");
        Occurrence o = a.on(d(2026, 11, 21)).get(0);
        check(o.isMultiDay() && o.fillsDay(d(2026, 11, 21)), "voyage sur plusieurs jours");
        eq("Du 20 au 22 nov.", Fmt.when(o), "libellé du voyage");
        Occurrence g = a.on(d(2026, 11, 25)).get(0);
        eq("Jusqu'à 06:00", Fmt.whenOn(g, d(2026, 11, 25)), "garde vue le lendemain");
        eq("À partir de 22:00", Fmt.whenOn(g, d(2026, 11, 24)), "garde vue le premier jour");
        eq(d(2026, 11, 25), g.lastDay(), "dernier jour de la garde");
    }

    private static void byDay() {
        Agenda a = new Agenda();
        a.add(Event.allDay("Salon", d(2026, 11, 7), d(2026, 11, 9)));
        a.add(new Event("Dentiste", t(2026, 11, 8, 10, 0), t(2026, 11, 8, 10, 30)));
        Map<LocalDate, List<Occurrence>> map = a.byDay(d(2026, 11, 8), 3);
        eq(3, map.size(), "trois jours");
        eq(2, map.get(d(2026, 11, 8)).size(), "deux choses le 8");
        eq("Salon", map.get(d(2026, 11, 8)).get(0).event.title, "journée entière d'abord");
        eq(1, map.get(d(2026, 11, 9)).size(), "le salon continue le 9");
        eq(0, map.get(d(2026, 11, 10)).size(), "rien le 10");
    }

    // ------------------------------------------------------------------ modifications

    private static void editThis() {
        Agenda a = new Agenda();
        Event e = weekly("Cours", t(2026, 11, 3, 9, 0), 60);
        a.add(e);
        Event edited = e.copy();
        edited.start = t(2026, 11, 10, 14, 0);
        edited.end = t(2026, 11, 10, 15, 0);
        edited.title = "Cours déplacé";
        a.applyEdit(e, d(2026, 11, 10), edited, Agenda.Scope.THIS);
        eq(2, a.size(), "une copie détachée");
        List<Occurrence> on10 = a.on(d(2026, 11, 10));
        eq(1, on10.size(), "une seule occurrence le 10");
        eq("Cours déplacé", on10.get(0).event.title, "c'est la version modifiée");
        check(!on10.get(0).event.isRecurring(), "la copie ne se répète pas");
        eq(1, a.on(d(2026, 11, 17)).size(), "la série continue");
    }

    private static void editFollowing() {
        Agenda a = new Agenda();
        Event e = weekly("Yoga", t(2026, 11, 2, 19, 0), 60);
        e.recurrence.count = 10;
        a.add(e);
        Event edited = e.copy();
        edited.start = t(2026, 11, 16, 20, 0);
        edited.end = t(2026, 11, 16, 21, 0);
        a.applyEdit(e, d(2026, 11, 16), edited, Agenda.Scope.FOLLOWING);
        eq(2, a.size(), "la série est coupée en deux");
        eq(d(2026, 11, 15), e.recurrence.until, "l'ancienne s'arrête la veille");
        eq(8, edited.recurrence.count, "la nouvelle reprend le décompte (10 - 2)");
        eq(t(2026, 11, 9, 19, 0), a.on(d(2026, 11, 9)).get(0).start, "avant : inchangé");
        eq(t(2026, 11, 23, 20, 0), a.on(d(2026, 11, 23)).get(0).start, "après : nouvelle heure");
        eq(10, a.occurrences(t(2026, 1, 1, 0, 0), t(2028, 1, 1, 0, 0)).size(), "toujours 10 au total");
    }

    private static void editAll() {
        Agenda a = new Agenda();
        Event e = weekly("Réunion", t(2026, 11, 2, 10, 0), 60);
        a.add(e);
        Event edited = e.copy();
        // on avance l'occurrence du 16 d'une heure et on l'allonge
        edited.start = t(2026, 11, 16, 9, 0);
        edited.end = t(2026, 11, 16, 11, 0);
        Event saved = a.applyEdit(e, d(2026, 11, 16), edited, Agenda.Scope.ALL);
        eq(1, a.size(), "toujours un seul événement");
        eq(t(2026, 11, 2, 9, 0), saved.start, "toute la série avancée d'une heure");
        eq(t(2026, 11, 2, 11, 0), saved.end, "et allongée");
        eq(e.uid, saved.uid, "même identifiant");
    }

    private static void deleteScopes() {
        Agenda a = new Agenda();
        Event e = weekly("Sport", t(2026, 11, 2, 7, 0), 30);
        a.add(e);
        a.delete(e, d(2026, 11, 9), Agenda.Scope.THIS);
        check(a.on(d(2026, 11, 9)).isEmpty(), "occurrence supprimée");
        eq(1, a.on(d(2026, 11, 16)).size(), "les autres restent");
        a.delete(e, d(2026, 11, 23), Agenda.Scope.FOLLOWING);
        check(a.on(d(2026, 11, 30)).isEmpty(), "plus rien après");
        eq(1, a.on(d(2026, 11, 16)).size(), "avant : intact");
        a.delete(e, d(2026, 11, 2), Agenda.Scope.FOLLOWING);
        eq(0, a.size(), "« et les suivantes » depuis la première = tout");

        Event f = weekly("Court", t(2026, 11, 2, 7, 0), 30);
        f.recurrence.count = 1;
        a.add(f);
        a.delete(f, d(2026, 11, 2), Agenda.Scope.THIS);
        eq(0, a.size(), "plus aucune occurrence : l'événement disparaît");
    }

    // ------------------------------------------------------------------ rappels et recherche

    private static void reminders() {
        Agenda a = new Agenda();
        Event e = new Event("Médecin", t(2026, 11, 10, 9, 0), t(2026, 11, 10, 9, 30));
        e.reminder = 30;
        a.add(e);
        Event b = Event.allDay("Anniversaire de Léa", d(2026, 11, 12), d(2026, 11, 12));
        b.recurrence = new Recurrence(Recurrence.Freq.YEARLY);
        b.reminder = 24 * 60;
        a.add(b);
        a.add(new Event("Sans rappel", t(2026, 11, 10, 8, 0), t(2026, 11, 10, 8, 30)));
        eq(t(2026, 11, 10, 8, 30), a.nextReminderTime(t(2026, 11, 1, 0, 0)), "prochain rappel");
        eq(t(2026, 11, 11, 0, 0), a.nextReminderTime(t(2026, 11, 10, 8, 30)), "puis l'anniversaire, la veille à minuit");
        eq(t(2027, 11, 11, 0, 0), a.nextReminderTime(t(2026, 11, 11, 0, 0)), "l'année suivante");
        List<Reminder> due = a.remindersBetween(t(2026, 11, 10, 8, 0), t(2026, 11, 10, 8, 30));
        eq(1, due.size(), "un rappel dans la fenêtre");
        eq("Médecin", due.get(0).occurrence.event.title, "le bon");
        check(a.remindersBetween(t(2026, 11, 10, 8, 30), t(2026, 11, 10, 8, 59)).isEmpty(), "pas deux fois");
    }

    private static void search() {
        Agenda a = new Agenda();
        Event e = weekly("Répétition chorale", t(2020, 1, 6, 20, 0), 90);
        e.location = "Église Saint-Étienne";
        a.add(e);
        a.add(new Event("Réunion passée", t(2025, 3, 1, 10, 0), t(2025, 3, 1, 11, 0)));
        a.add(new Event("Dîner", t(2026, 12, 24, 20, 0), t(2026, 12, 24, 23, 0)));
        LocalDateTime now = t(2026, 11, 8, 12, 0);
        List<Occurrence> r = a.search("eglise", now);
        eq(1, r.size(), "recherche sans accents dans le lieu");
        eq(t(2026, 11, 9, 20, 0), r.get(0).start, "prochaine occurrence");
        List<Occurrence> all = a.search("é", now);
        check(all.size() >= 2, "plusieurs résultats");
        eq("Réunion passée", a.search("REUNION", now).get(0).event.title, "événement passé trouvé");
        eq(3, a.search("", now).size(), "recherche vide = tout");
        List<Occurrence> order = a.search("", now);
        eq("Répétition chorale", order.get(0).event.title, "à venir d'abord");
        eq("Réunion passée", order.get(order.size() - 1).event.title, "le passé à la fin");
    }

    // ------------------------------------------------------------------ iCalendar

    private static void icsRoundTrip() {
        Event e = weekly("Cours d'anglais; niveau 2, salle B", t(2026, 11, 3, 18, 30), 90);
        e.recurrence.days.add(DayOfWeek.TUESDAY);
        e.recurrence.days.add(DayOfWeek.THURSDAY);
        e.recurrence.until = d(2027, 6, 30);
        e.exdates.add(d(2026, 12, 24));
        e.location = "Centre culturel";
        e.notes = "Apporter le livre\nPage 42 \\ exercices";
        e.category = Category.LOISIRS;
        e.reminder = 15;
        Event b = Event.allDay("Anniversaire de maman", d(2026, 3, 14), d(2026, 3, 14));
        b.recurrence = new Recurrence(Recurrence.Freq.YEARLY);
        b.category = Category.ANNIVERSAIRE;
        b.reminder = 24 * 60;
        Event m = new Event("Réunion", t(2026, 11, 27, 9, 0), t(2026, 11, 27, 10, 0));
        m.recurrence = new Recurrence(Recurrence.Freq.MONTHLY);
        m.recurrence.weekOfMonth = -1;
        m.recurrence.count = 6;
        m.category = Category.TRAVAIL;
        Agenda a = new Agenda();
        a.add(e);
        a.add(b);
        a.add(m);

        String text = Ics.write(a.events());
        check(text.startsWith("BEGIN:VCALENDAR\r\n"), "en-tête");
        check(text.contains("RRULE:FREQ=WEEKLY;BYDAY=TU,TH;UNTIL=20270630T235959"), "règle hebdomadaire : " + text);
        check(text.contains("RRULE:FREQ=MONTHLY;BYDAY=-1FR;COUNT=6"), "règle mensuelle");
        check(text.contains("SUMMARY:Cours d'anglais\\; niveau 2\\, salle B"), "échappement");

        List<Event> back = Ics.read(text);
        eq(3, back.size(), "trois événements relus");
        Event e2 = back.get(0);
        eq(e.uid, e2.uid, "uid");
        eq(e.title, e2.title, "titre");
        eq(e.notes, e2.notes, "notes sur plusieurs lignes");
        eq(e.location, e2.location, "lieu");
        eq(e.start, e2.start, "début");
        eq(e.end, e2.end, "fin");
        eq(Category.LOISIRS, e2.category, "catégorie");
        eq(15, e2.reminder, "rappel");
        eq(e.recurrence.days, e2.recurrence.days, "jours");
        eq(d(2027, 6, 30), e2.recurrence.until, "jusqu'au");
        eq(e.exdates, e2.exdates, "exceptions");
        Event b2 = back.get(1);
        check(b2.allDay, "journée entière");
        eq(d(2026, 3, 14).atStartOfDay(), b2.start, "anniversaire début");
        eq(d(2026, 3, 15).atStartOfDay(), b2.end, "anniversaire fin exclue");
        eq(1440, b2.reminder, "rappel la veille");
        Event m2 = back.get(2);
        eq(-1, m2.recurrence.weekOfMonth, "dernier vendredi relu");
        eq(6, m2.recurrence.count, "6 fois relu");
        // le même contenu, réécrit, donne les mêmes occurrences
        Agenda again = new Agenda();
        again.setAll(back);
        eq(a.occurrences(t(2026, 1, 1, 0, 0), t(2028, 1, 1, 0, 0)).size(),
                again.occurrences(t(2026, 1, 1, 0, 0), t(2028, 1, 1, 0, 0)).size(), "mêmes occurrences");
    }

    private static void icsFolding() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 40; i++) sb.append("Événement très long é ");
        Event e = new Event(sb.toString(), t(2026, 1, 1, 10, 0), t(2026, 1, 1, 11, 0));
        String text = Ics.write(Arrays.asList(e));
        for (String line : text.split("\r\n")) {
            check(line.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 75, "ligne de plus de 75 octets : " + line);
        }
        eq(e.title, Ics.read(text).get(0).title, "titre replié puis recollé");
    }

    private static void icsImportForeign() {
        String ics = "BEGIN:VCALENDAR\n"
                + "VERSION:2.0\n"
                + "BEGIN:VTIMEZONE\nTZID:Europe/Paris\nBEGIN:STANDARD\nDTSTART:19701025T030000\nEND:STANDARD\nEND:VTIMEZONE\n"
                + "BEGIN:VEVENT\n"
                + "UID:abc@google.com\n"
                + "DTSTART;TZID=Europe/Paris:20261102T090000\n"
                + "DURATION:PT1H30M\n"
                + "RRULE:FREQ=WEEKLY;WKST=MO;BYDAY=MO\n"
                + "SUMMARY:Point d'équipe\n"
                + "DESCRIPTION:Ligne 1\\nLigne 2 avec un texte qui est replié sur plusieurs l\n"
                + " ignes\n"
                + "CATEGORIES:Work,Important\n"
                + "BEGIN:VALARM\nTRIGGER;RELATED=START:-P1D\nACTION:DISPLAY\nEND:VALARM\n"
                + "END:VEVENT\n"
                + "BEGIN:VEVENT\n"
                + "UID:abc@google.com\n"
                + "RECURRENCE-ID;TZID=Europe/Paris:20261109T090000\n"
                + "DTSTART;TZID=Europe/Paris:20261110T150000\n"
                + "DTEND;TZID=Europe/Paris:20261110T160000\n"
                + "SUMMARY:Point d'équipe (décalé)\n"
                + "END:VEVENT\n"
                + "BEGIN:VEVENT\n"
                + "UID:abc@google.com\n"
                + "RECURRENCE-ID;TZID=Europe/Paris:20261116T090000\n"
                + "STATUS:CANCELLED\n"
                + "DTSTART;TZID=Europe/Paris:20261116T090000\n"
                + "END:VEVENT\n"
                + "BEGIN:VEVENT\n"
                + "DTSTART;VALUE=DATE:20261225\n"
                + "SUMMARY:Noël\n"
                + "END:VEVENT\n"
                + "BEGIN:VTODO\nSUMMARY:pas un événement\nEND:VTODO\n"
                + "END:VCALENDAR\n";
        List<Event> list = Ics.read(ics);
        eq(3, list.size(), "série + occurrence modifiée + Noël");
        Event serie = list.get(0);
        eq(Category.TRAVAIL, serie.category, "catégorie Work → Travail");
        eq(1440, serie.reminder, "rappel -P1D");
        eq("Ligne 1\nLigne 2 avec un texte qui est replié sur plusieurs lignes", serie.notes, "description repliée");
        eq(90L, serie.duration().toMinutes(), "DURATION");
        check(serie.exdates.contains(d(2026, 11, 9)) && serie.exdates.contains(d(2026, 11, 16)), "occurrences modifiée et annulée exclues");
        Event moved = list.get(2);
        check(!moved.isRecurring(), "occurrence modifiée = événement à part");
        check(!moved.uid.equals(serie.uid), "identifiant distinct");
        Event noel = list.get(1);
        check(noel.allDay, "date seule = journée entière");
        eq(d(2026, 12, 26).atStartOfDay(), noel.end, "fin par défaut = lendemain");
        check(list.get(0).uid != null, "uid présent");

        String utc = "BEGIN:VCALENDAR\nBEGIN:VEVENT\nDTSTART:20260701T120000Z\nDTEND:20260701T130000Z\nSUMMARY:UTC\nEND:VEVENT\nEND:VCALENDAR";
        Event u = Ics.read(utc).get(0);
        LocalDateTime expected = LocalDateTime.of(2026, 7, 1, 12, 0).atZone(java.time.ZoneOffset.UTC)
                .withZoneSameInstant(java.time.ZoneId.systemDefault()).toLocalDateTime();
        eq(expected, u.start, "heure UTC convertie en heure locale");
        check(u.uid != null && !u.uid.isEmpty(), "uid créé s'il manque");
    }

    private static void dayLayout() {
        LocalDate day = d(2026, 11, 9);
        Agenda a = new Agenda();
        a.add(new Event("A", t(2026, 11, 9, 9, 0), t(2026, 11, 9, 11, 0)));
        a.add(new Event("B", t(2026, 11, 9, 10, 0), t(2026, 11, 9, 12, 0)));
        a.add(new Event("C", t(2026, 11, 9, 11, 0), t(2026, 11, 9, 11, 30)));
        a.add(new Event("D", t(2026, 11, 9, 14, 0), t(2026, 11, 9, 15, 0)));
        a.add(Event.allDay("Toute la journée", day, day));
        List<DayLayout.Slot> slots = DayLayout.layout(a.on(day), day, 20);
        eq(4, slots.size(), "la journée entière n'est pas sur la grille");
        eq("A", slots.get(0).occurrence.event.title, "ordre");
        eq(0, slots.get(0).column, "A colonne 0");
        eq(1, slots.get(1).column, "B colonne 1");
        eq(0, slots.get(2).column, "C reprend la colonne de A");
        eq(2, slots.get(0).columns, "groupe de 2 colonnes");
        eq(1, slots.get(3).columns, "D seul");
        eq(9 * 60, slots.get(0).startMinute(day), "minutes depuis minuit");
    }

    private static void tasks() {
        Agenda a = new Agenda();
        Event t = Event.task("Payer la facture", t(2026, 11, 10, 9, 0));
        a.add(t);
        Event loyer = Event.task("Loyer", t(2026, 11, 1, 8, 0));
        loyer.recurrence = new Recurrence(Recurrence.Freq.MONTHLY);
        loyer.reminder = 60;
        a.add(loyer);
        eq(t(2026, 11, 10, 9, 0), a.nextReminderTime(t(2026, 11, 2, 0, 0)), "rappel à l'heure de la tâche");
        a.setDone(t, d(2026, 11, 10), true);
        eq(t(2026, 12, 1, 7, 0), a.nextReminderTime(t(2026, 11, 2, 0, 0)), "plus de rappel pour une tâche faite");
        check(a.on(d(2026, 11, 10)).get(0).isDone(), "tâche faite");
        // en retard : le loyer de novembre n'est pas payé
        List<Occurrence> late = a.overdue(t(2026, 11, 20, 12, 0), 60);
        eq(1, late.size(), "une tâche en retard");
        eq(d(2026, 11, 1), late.get(0).date(), "celle du 1er novembre");
        a.setDone(loyer, d(2026, 11, 1), true);
        check(a.overdue(t(2026, 11, 20, 12, 0), 60).isEmpty(), "plus rien en retard");
        check(!a.on(d(2026, 12, 1)).get(0).isDone(), "décembre reste à faire");
        // modifier « celle-ci uniquement » garde l'état fait
        Event moved = loyer.copy();
        moved.start = t(2026, 11, 2, 8, 0);
        moved.end = moved.start;
        Event saved = a.applyEdit(loyer, d(2026, 11, 1), moved, Agenda.Scope.THIS);
        check(saved.isDone(d(2026, 11, 2)), "la copie détachée reste faite");
        check(!loyer.done.contains(d(2026, 11, 1)), "la série oublie cette date");
        // aller-retour .ics
        List<Event> back = Ics.read(Ics.write(a.events()));
        Event t2 = null;
        for (Event e : back) if (e.title.equals("Payer la facture")) t2 = e;
        check(t2 != null && t2.task, "tâche relue");
        check(t2 != null && t2.isDone(d(2026, 11, 10)), "état fait relu");
    }

    private static void statement() {
        Agenda a = new Agenda();
        Event r = new Event("Réunion", t(2026, 11, 2, 9, 0), t(2026, 11, 2, 10, 30));
        r.category = Category.TRAVAIL;
        r.recurrence = new Recurrence(Recurrence.Freq.WEEKLY);
        a.add(r);
        Event t1 = Event.task("Rapport", t(2026, 11, 5, 17, 0));
        t1.category = Category.TRAVAIL;
        a.add(t1);
        a.setDone(t1, d(2026, 11, 5), true);
        a.add(Event.task("Banque", t(2026, 11, 6, 10, 0)));
        a.add(Event.task("Courses", t(2026, 11, 20, 10, 0)));
        a.add(Event.allDay("Congé", d(2026, 11, 11), d(2026, 11, 11)));
        a.add(new Event("Hors période", t(2026, 12, 2, 9, 0), t(2026, 12, 2, 10, 0)));
        Statement st = Statement.build(a, d(2026, 11, 1), d(2026, 11, 30), t(2026, 11, 8, 12, 0));
        eq(5 + 3 + 1, st.lines.size(), "5 réunions + 3 tâches + 1 congé");
        eq(6, st.appointments, "rendez-vous");
        eq(3, st.tasks, "tâches");
        eq(1, st.done, "faites");
        eq(1, st.overdue, "en retard");
        eq(1, st.todo, "à faire");
        eq(5 * 90L, st.minutes, "7 h 30 de réunions");
        eq("7 h 30", Statement.duration(st.minutes), "durée affichée");
        eq(50, st.completionRate(), "taux de réalisation");
        eq(Integer.valueOf(6), st.countByCategory.get(Category.TRAVAIL), "par catégorie");
        eq(Statement.Status.PAST, st.lines.get(0).status, "première réunion passée");
        eq(Statement.Status.DONE, st.lines.get(1).status, "rapport fait");
        eq(Statement.Status.OVERDUE, st.lines.get(2).status, "banque en retard");
        eq(Statement.Status.UPCOMING, st.lines.get(3).status, "réunion du 9 à venir");
        eq(9, st.lines.get(8).number, "numérotation");
        eq("du 1er au 30 novembre 2026", st.periodLabel(), "période");
    }

    private static void describe() {
        Recurrence r = new Recurrence(Recurrence.Freq.WEEKLY);
        r.days.add(DayOfWeek.MONDAY);
        r.days.add(DayOfWeek.THURSDAY);
        r.interval = 2;
        r.until = d(2027, 3, 1);
        eq("Toutes les 2 semaines le lundi et le jeudi, jusqu'au 1er mars 2027", r.describe(d(2026, 11, 2)), "description hebdo");
        Recurrence m = new Recurrence(Recurrence.Freq.MONTHLY);
        m.weekOfMonth = 2;
        eq("Tous les mois le 2e mardi", m.describe(d(2026, 11, 10)), "description mensuelle");
        Recurrence y = new Recurrence(Recurrence.Freq.YEARLY);
        y.count = 5;
        eq("Tous les ans le 14 mars, 5 fois", y.describe(d(2026, 3, 14)), "description annuelle");
        eq("15 minutes avant", Fmt.reminder(15), "rappel 15 min");
        eq("1 jour avant", Fmt.reminder(1440), "rappel 1 jour");
        eq("2 heures avant", Fmt.reminder(120), "rappel 2 h");
        eq("Dimanche 8 novembre 2026", Fmt.capitalize(Fmt.longDate(d(2026, 11, 8))), "date longue");
        eq("30 nov. – 6 déc. 2026", Fmt.weekTitle(d(2026, 11, 30)), "semaine à cheval");
        eq("9 – 15 novembre 2026", Fmt.weekTitle(d(2026, 11, 9)), "semaine");
        eq(Category.ANNIVERSAIRE, Category.fromLabel("anniversaire"), "catégorie par nom");
        eq(Category.SANTE, Category.fromLabel("Sante"), "catégorie sans accent");
    }
}
