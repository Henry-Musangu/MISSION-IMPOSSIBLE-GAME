package agenda;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lecture et écriture au format iCalendar (.ics, RFC 5545) : c'est le format du fichier de l'agenda,
 * et celui qu'acceptent Google Agenda, Outlook, Apple Calendrier, Thunderbird...
 *
 * <p>Les heures sont écrites en heure locale « flottante » (sans fuseau). À la lecture, les heures
 * en UTC ou avec un fuseau (TZID) sont converties dans le fuseau de l'appareil.
 */
public final class Ics {

    private Ics() {
    }

    private static final String[] WEEKDAYS = {"MO", "TU", "WE", "TH", "FR", "SA", "SU"};

    // ================================================================== écriture

    public static String write(Collection<Event> events) {
        StringWriter w = new StringWriter();
        try {
            write(events, w);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return w.toString();
    }

    public static void write(Collection<Event> events, Writer out) throws IOException {
        Lines w = new Lines(out);
        w.line("BEGIN:VCALENDAR");
        w.line("VERSION:2.0");
        w.line("PRODID:-//Henry Musangu//Agenda personnel//FR");
        w.line("CALSCALE:GREGORIAN");
        w.line("X-WR-CALNAME:Agenda");
        String stamp = stamp(ZonedDateTime.now(ZoneOffset.UTC).toLocalDateTime()) + "Z";
        for (Event e : events) {
            if (e.start == null || e.end == null) continue;
            w.line("BEGIN:VEVENT");
            w.line("UID:" + text(e.uid == null ? Event.newUid() : e.uid));
            w.line("DTSTAMP:" + stamp);
            if (e.allDay) {
                w.line("DTSTART;VALUE=DATE:" + date(e.start.toLocalDate()));
                LocalDate endDate = e.end.toLocalDate();
                if (!endDate.isAfter(e.start.toLocalDate())) endDate = e.start.toLocalDate().plusDays(1);
                w.line("DTEND;VALUE=DATE:" + date(endDate));
            } else {
                w.line("DTSTART:" + stamp(e.start));
                w.line("DTEND:" + stamp(e.end));
            }
            w.line("SUMMARY:" + text(e.title));
            if (!isBlank(e.location)) w.line("LOCATION:" + text(e.location));
            if (!isBlank(e.notes)) w.line("DESCRIPTION:" + text(e.notes));
            w.line("CATEGORIES:" + text(e.category.label));
            if (e.task) {
                w.line("X-HENRY-TASK:TRUE");
                if (!e.done.isEmpty()) {
                    StringBuilder sb = new StringBuilder("X-HENRY-DONE:");
                    boolean first = true;
                    for (LocalDate d : e.done) {
                        if (!first) sb.append(',');
                        sb.append(date(d));
                        first = false;
                    }
                    w.line(sb.toString());
                }
            }
            if (e.recurrence != null) {
                w.line("RRULE:" + rrule(e));
                for (LocalDate d : e.exdates) {
                    w.line(e.allDay ? "EXDATE;VALUE=DATE:" + date(d) : "EXDATE:" + stamp(d.atTime(e.start.toLocalTime())));
                }
            }
            if (e.reminder >= 0) {
                w.line("BEGIN:VALARM");
                w.line("ACTION:DISPLAY");
                w.line("DESCRIPTION:" + text(e.displayTitle()));
                w.line("TRIGGER:" + (e.reminder == 0 ? "PT0S" : "-PT" + e.reminder + "M"));
                w.line("END:VALARM");
            }
            w.line("END:VEVENT");
        }
        w.line("END:VCALENDAR");
        out.flush();
    }

    static String rrule(Event e) {
        Recurrence r = e.recurrence;
        StringBuilder sb = new StringBuilder("FREQ=").append(r.freq.name());
        if (r.interval > 1) sb.append(";INTERVAL=").append(r.interval);
        if (r.freq == Recurrence.Freq.WEEKLY && !r.days.isEmpty()) {
            sb.append(";BYDAY=");
            boolean first = true;
            for (DayOfWeek d : r.days) {
                if (!first) sb.append(',');
                sb.append(WEEKDAYS[d.getValue() - 1]);
                first = false;
            }
        }
        if (r.freq == Recurrence.Freq.MONTHLY && r.weekOfMonth != 0) {
            DayOfWeek d = r.days.isEmpty() ? e.start.getDayOfWeek() : r.days.iterator().next();
            sb.append(";BYDAY=").append(r.weekOfMonth).append(WEEKDAYS[d.getValue() - 1]);
        }
        if (r.until != null) {
            sb.append(";UNTIL=").append(e.allDay ? date(r.until) : stamp(r.until.atTime(23, 59, 59)));
        } else if (r.count > 0) {
            sb.append(";COUNT=").append(r.count);
        }
        return sb.toString();
    }

    private static String date(LocalDate d) {
        return String.format(Locale.ROOT, "%04d%02d%02d", d.getYear(), d.getMonthValue(), d.getDayOfMonth());
    }

    private static String stamp(LocalDateTime t) {
        return date(t.toLocalDate()) + String.format(Locale.ROOT, "T%02d%02d%02d", t.getHour(), t.getMinute(), t.getSecond());
    }

    /** Échappe un texte (\ ; , et retours à la ligne). */
    static String text(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case ';': sb.append("\\;"); break;
                case ',': sb.append("\\,"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** Écrit des lignes terminées par CRLF, repliées à 75 octets UTF-8 comme l'exige la norme. */
    private static final class Lines {
        final Writer out;

        Lines(Writer out) {
            this.out = out;
        }

        void line(String s) throws IOException {
            int bytes = 0;
            int limit = 75;
            for (int i = 0; i < s.length(); ) {
                int cp = s.codePointAt(i);
                int n = new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8).length;
                if (bytes + n > limit) {
                    out.write("\r\n ");
                    bytes = 1;
                    limit = 75;
                }
                out.write(Character.toChars(cp));
                bytes += n;
                i += Character.charCount(cp);
            }
            out.write("\r\n");
        }
    }

    // ================================================================== lecture

    public static List<Event> read(String text) {
        try {
            return read(new StringReader(text));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Lit les événements (VEVENT) d'un fichier iCalendar. Les éléments non gérés sont ignorés. */
    public static List<Event> read(Reader in) throws IOException {
        List<Prop> props = unfold(in);
        List<Event> events = new ArrayList<Event>();
        List<Event> overrides = new ArrayList<Event>();
        Map<Event, LocalDate> overrideDates = new HashMap<Event, LocalDate>();
        List<Event> cancelled = new ArrayList<Event>();

        Event cur = null;
        boolean inAlarm = false;
        int depthOther = 0;         // composants ignorés (VTIMEZONE, VTODO...)
        Recurrence rule = null;
        String rruleText = null;
        boolean hasEnd = false;
        long durationSeconds = -1;
        LocalDate recurrenceId = null;
        boolean isCancelled = false;
        int alarmMinutes = Integer.MIN_VALUE;

        for (Prop p : props) {
            if (p.name.equals("BEGIN")) {
                String v = p.value.trim().toUpperCase(Locale.ROOT);
                if (v.equals("VCALENDAR")) continue;
                if (cur == null && depthOther == 0 && v.equals("VEVENT")) {
                    cur = new Event();
                    cur.reminder = -1;
                    rule = null;
                    rruleText = null;
                    hasEnd = false;
                    durationSeconds = -1;
                    recurrenceId = null;
                    isCancelled = false;
                    alarmMinutes = Integer.MIN_VALUE;
                } else if (cur != null && v.equals("VALARM")) {
                    inAlarm = true;
                } else {
                    depthOther++;
                }
                continue;
            }
            if (p.name.equals("END")) {
                String v = p.value.trim().toUpperCase(Locale.ROOT);
                if (v.equals("VCALENDAR")) continue;
                if (inAlarm && v.equals("VALARM")) {
                    inAlarm = false;
                } else if (depthOther > 0) {
                    depthOther--;
                } else if (cur != null && v.equals("VEVENT")) {
                    if (cur.start != null) {
                        if (!hasEnd) {
                            if (durationSeconds >= 0) {
                                cur.end = cur.start.plusSeconds(durationSeconds);
                            } else {
                                cur.end = cur.allDay ? cur.start.plusDays(1) : cur.start;
                            }
                        }
                        if (cur.end.isBefore(cur.start)) cur.end = cur.start;
                        if (cur.allDay && !cur.end.isAfter(cur.start)) cur.end = cur.start.plusDays(1);
                        if (rruleText != null) {
                            rule = parseRule(rruleText, cur);
                            cur.recurrence = rule;
                        }
                        if (cur.recurrence == null) cur.exdates.clear();
                        if (!cur.task) cur.done.clear();
                        if (alarmMinutes != Integer.MIN_VALUE) cur.reminder = Math.max(0, alarmMinutes);
                        if (recurrenceId != null) {
                            overrideDates.put(cur, recurrenceId);
                            if (isCancelled) cancelled.add(cur); else overrides.add(cur);
                        } else if (!isCancelled) {
                            events.add(cur);
                        }
                    }
                    cur = null;
                }
                continue;
            }
            if (cur == null || depthOther > 0) continue;

            if (inAlarm) {
                if (p.name.equals("TRIGGER") && alarmMinutes == Integer.MIN_VALUE) {
                    alarmMinutes = parseTrigger(p, cur);
                }
                continue;
            }

            switch (p.name) {
                case "UID": cur.uid = unescape(p.value); break;
                case "SUMMARY": cur.title = unescape(p.value); break;
                case "LOCATION": cur.location = unescape(p.value); break;
                case "DESCRIPTION": cur.notes = unescape(p.value); break;
                case "CATEGORIES": {
                    String first = splitList(p.value).isEmpty() ? "" : splitList(p.value).get(0);
                    cur.category = Category.fromLabel(unescape(first));
                    break;
                }
                case "DTSTART": {
                    Stamp s = parseStamp(p.value, p.params);
                    if (s != null) {
                        cur.start = s.time;
                        cur.allDay = s.dateOnly;
                    }
                    break;
                }
                case "DTEND": {
                    Stamp s = parseStamp(p.value, p.params);
                    if (s != null) {
                        cur.end = s.time;
                        hasEnd = true;
                    }
                    break;
                }
                case "DURATION": durationSeconds = Math.max(0, parseDuration(p.value)); break;
                case "RRULE": rruleText = p.value; break;
                case "EXDATE":
                    for (String part : p.value.split(",")) {
                        Stamp s = parseStamp(part, p.params);
                        if (s != null) cur.exdates.add(s.time.toLocalDate());
                    }
                    break;
                case "RECURRENCE-ID": {
                    Stamp s = parseStamp(p.value, p.params);
                    if (s != null) recurrenceId = s.time.toLocalDate();
                    break;
                }
                case "STATUS": isCancelled = p.value.trim().equalsIgnoreCase("CANCELLED"); break;
                case "X-HENRY-TASK": cur.task = p.value.trim().equalsIgnoreCase("TRUE"); break;
                case "X-HENRY-DONE":
                    for (String part : p.value.split(",")) {
                        Stamp s = parseStamp(part, p.params);
                        if (s != null) cur.done.add(s.time.toLocalDate());
                    }
                    break;
                default: break;
            }
        }

        // Occurrences modifiées (RECURRENCE-ID) : supprimées de la série et ajoutées à part.
        for (Event o : cancelled) {
            Event master = findUid(events, o.uid);
            if (master != null) master.exdates.add(overrideDates.get(o));
        }
        for (Event o : overrides) {
            LocalDate d = overrideDates.get(o);
            Event master = findUid(events, o.uid);
            if (master != null) master.exdates.add(d);
            o.uid = (o.uid == null ? Event.newUid() : o.uid) + "/" + date(d);
            o.recurrence = null;
            o.exdates.clear();
            events.add(o);
        }
        for (Event e : events) {
            if (e.uid == null || e.uid.trim().isEmpty()) e.uid = Event.newUid();
        }
        return events;
    }

    private static Event findUid(List<Event> events, String uid) {
        if (uid == null) return null;
        for (Event e : events) {
            if (uid.equals(e.uid) && e.recurrence != null) return e;
        }
        return null;
    }

    /** Une ligne de contenu : NOM;PARAM=valeur:valeur */
    static final class Prop {
        final String name;
        final Map<String, String> params = new HashMap<String, String>();
        final String value;

        Prop(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    /** Recolle les lignes repliées et découpe chaque ligne en nom, paramètres et valeur. */
    static List<Prop> unfold(Reader in) throws IOException {
        BufferedReader r = new BufferedReader(in);
        List<String> lines = new ArrayList<String>();
        StringBuilder cur = null;
        String line;
        boolean firstLine = true;
        while ((line = r.readLine()) != null) {
            if (firstLine && line.startsWith("﻿")) line = line.substring(1);
            firstLine = false;
            if (!line.isEmpty() && (line.charAt(0) == ' ' || line.charAt(0) == '\t')) {
                if (cur != null) cur.append(line, 1, line.length());
                continue;
            }
            if (cur != null) lines.add(cur.toString());
            cur = new StringBuilder(line);
        }
        if (cur != null) lines.add(cur.toString());

        List<Prop> props = new ArrayList<Prop>();
        for (String l : lines) {
            Prop p = parseLine(l);
            if (p != null) props.add(p);
        }
        return props;
    }

    static Prop parseLine(String line) {
        boolean quoted = false;
        int colon = -1;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') quoted = !quoted;
            else if (c == ':' && !quoted) {
                colon = i;
                break;
            }
        }
        if (colon <= 0) return null;
        String head = line.substring(0, colon);
        String value = line.substring(colon + 1);
        List<String> parts = new ArrayList<String>();
        quoted = false;
        int startPart = 0;
        for (int i = 0; i < head.length(); i++) {
            char c = head.charAt(i);
            if (c == '"') quoted = !quoted;
            else if (c == ';' && !quoted) {
                parts.add(head.substring(startPart, i));
                startPart = i + 1;
            }
        }
        parts.add(head.substring(startPart));
        Prop p = new Prop(parts.get(0).trim().toUpperCase(Locale.ROOT), value);
        for (int i = 1; i < parts.size(); i++) {
            String s = parts.get(i);
            int eq = s.indexOf('=');
            if (eq > 0) {
                String v = s.substring(eq + 1);
                if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) v = v.substring(1, v.length() - 1);
                p.params.put(s.substring(0, eq).trim().toUpperCase(Locale.ROOT), v);
            }
        }
        return p;
    }

    static String unescape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                if (n == 'n' || n == 'N') sb.append('\n');
                else sb.append(n);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Découpe une liste séparée par des virgules non échappées. */
    private static List<String> splitList(String s) {
        List<String> out = new ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                cur.append(c).append(s.charAt(++i));
            } else if (c == ',') {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    /** Une date ou une date-heure lue, ramenée à l'heure locale de l'appareil. */
    static final class Stamp {
        final LocalDateTime time;
        final boolean dateOnly;

        Stamp(LocalDateTime time, boolean dateOnly) {
            this.time = time;
            this.dateOnly = dateOnly;
        }
    }

    static Stamp parseStamp(String raw, Map<String, String> params) {
        String v = raw.trim();
        try {
            if ("DATE".equalsIgnoreCase(params.get("VALUE")) || v.length() == 8) {
                return new Stamp(parseDate(v).atStartOfDay(), true);
            }
            LocalDateTime t = parseDateTime(v);
            if (v.endsWith("Z") || v.endsWith("z")) {
                t = t.atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
            } else if (params.get("TZID") != null) {
                ZoneId zone = zone(params.get("TZID"));
                if (zone != null) t = t.atZone(zone).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
            }
            return new Stamp(t, false);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static ZoneId zone(String tzid) {
        String id = tzid.trim();
        while (id.startsWith("/")) id = id.substring(1);
        try {
            return ZoneId.of(id);
        } catch (RuntimeException e) {
            return null;   // fuseau inconnu (noms Windows...) : on garde l'heure telle quelle
        }
    }

    private static LocalDate parseDate(String v) {
        return LocalDate.of(Integer.parseInt(v.substring(0, 4)), Integer.parseInt(v.substring(4, 6)),
                Integer.parseInt(v.substring(6, 8)));
    }

    private static LocalDateTime parseDateTime(String v) {
        LocalDate d = parseDate(v);
        int t = v.indexOf('T') >= 0 ? v.indexOf('T') : v.indexOf('t');
        if (t < 0) return d.atStartOfDay();
        String hms = v.substring(t + 1).replace("Z", "").replace("z", "");
        int h = Integer.parseInt(hms.substring(0, 2));
        int m = hms.length() >= 4 ? Integer.parseInt(hms.substring(2, 4)) : 0;
        int s = hms.length() >= 6 ? Integer.parseInt(hms.substring(4, 6)) : 0;
        if (h == 24) return d.plusDays(1).atStartOfDay();
        return d.atTime(h, m, Math.min(s, 59));
    }

    /** Durée iCalendar (« -PT15M », « P1DT2H », « P1W ») en secondes. */
    static long parseDuration(String raw) {
        String v = raw.trim().toUpperCase(Locale.ROOT);
        int sign = 1;
        int i = 0;
        if (v.startsWith("-")) {
            sign = -1;
            i = 1;
        } else if (v.startsWith("+")) {
            i = 1;
        }
        if (i >= v.length() || v.charAt(i) != 'P') throw new IllegalArgumentException(raw);
        i++;
        long total = 0;
        long num = 0;
        boolean timePart = false;
        for (; i < v.length(); i++) {
            char c = v.charAt(i);
            if (c >= '0' && c <= '9') {
                num = num * 10 + (c - '0');
            } else if (c == 'T') {
                timePart = true;
            } else {
                switch (c) {
                    case 'W': total += num * 7 * 86400; break;
                    case 'D': total += num * 86400; break;
                    case 'H': total += num * 3600; break;
                    case 'M': total += timePart ? num * 60 : num * 30 * 86400; break;
                    case 'S': total += num; break;
                    default: throw new IllegalArgumentException(raw);
                }
                num = 0;
            }
        }
        return sign * total;
    }

    /** Minutes avant le début pour un TRIGGER de VALARM (Integer.MIN_VALUE si illisible). */
    private static int parseTrigger(Prop p, Event e) {
        try {
            if ("DATE-TIME".equalsIgnoreCase(p.params.get("VALUE"))) {
                Stamp s = parseStamp(p.value, p.params);
                if (s == null || e.start == null) return Integer.MIN_VALUE;
                return (int) java.time.Duration.between(s.time, e.start).toMinutes();
            }
            long seconds = parseDuration(p.value);
            return (int) (-seconds / 60);
        } catch (RuntimeException ex) {
            return Integer.MIN_VALUE;
        }
    }

    /** Lit une règle RRULE ; null si elle n'est pas gérée (l'événement est alors gardé une seule fois). */
    static Recurrence parseRule(String text, Event e) {
        Map<String, String> m = new HashMap<String, String>();
        for (String part : text.split(";")) {
            int eq = part.indexOf('=');
            if (eq > 0) m.put(part.substring(0, eq).trim().toUpperCase(Locale.ROOT), part.substring(eq + 1).trim());
        }
        String freq = m.get("FREQ");
        if (freq == null) return null;
        Recurrence r;
        try {
            r = new Recurrence(Recurrence.Freq.valueOf(freq.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return null;   // HOURLY, MINUTELY...
        }
        try {
            if (m.containsKey("INTERVAL")) r.interval = Math.max(1, Integer.parseInt(m.get("INTERVAL")));
            if (m.containsKey("COUNT")) r.count = Math.max(0, Integer.parseInt(m.get("COUNT")));
            if (m.containsKey("UNTIL")) {
                Stamp s = parseStamp(m.get("UNTIL"), new HashMap<String, String>());
                if (s != null) r.until = s.time.toLocalDate();
            }
        } catch (RuntimeException ex) {
            return null;
        }
        String byDay = m.get("BYDAY");
        if (byDay != null && !byDay.isEmpty()) {
            for (String d : byDay.split(",")) {
                String s = d.trim().toUpperCase(Locale.ROOT);
                if (s.length() < 2) continue;
                String code = s.substring(s.length() - 2);
                String num = s.substring(0, s.length() - 2);
                int idx = -1;
                for (int k = 0; k < 7; k++) if (WEEKDAYS[k].equals(code)) idx = k;
                if (idx < 0) continue;
                DayOfWeek dow = DayOfWeek.of(idx + 1);
                if (r.freq == Recurrence.Freq.WEEKLY) {
                    r.days.add(dow);
                } else if (r.freq == Recurrence.Freq.MONTHLY && r.days.isEmpty()) {
                    int n;
                    try {
                        n = num.isEmpty() || num.equals("+") ? 0 : Integer.parseInt(num.replace("+", ""));
                    } catch (NumberFormatException ex) {
                        n = 0;
                    }
                    if (n == 0 && m.containsKey("BYSETPOS")) {
                        try {
                            n = Integer.parseInt(m.get("BYSETPOS").split(",")[0].trim());
                        } catch (NumberFormatException ex) {
                            n = 0;
                        }
                    }
                    if (n < 0) n = -1;
                    if (n > 5) n = 0;
                    if (n != 0) {
                        r.days.add(dow);
                        r.weekOfMonth = n;
                    }
                } else if (r.freq == Recurrence.Freq.DAILY) {
                    r.days.add(dow);   // « tous les jours sauf... » : traité comme une répétition hebdomadaire
                }
            }
            if (r.freq == Recurrence.Freq.DAILY && !r.days.isEmpty()) {
                if (r.days.size() == 7 || r.interval != 1) {
                    r.days.clear();
                } else {
                    r.freq = Recurrence.Freq.WEEKLY;
                }
            }
        }
        return r;
    }
}
