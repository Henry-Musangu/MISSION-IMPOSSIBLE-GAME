package agenda.desktop;

import agenda.Fmt;
import agenda.Occurrence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Petites fonctions communes aux vues de la version PC. */
final class Desk {

    private Desk() {
    }

    /** Heure proposée pour un nouvel événement ce jour-là : la prochaine heure pleine aujourd'hui, 9 h sinon. */
    static LocalDateTime defaultStart(LocalDate day, LocalDate today) {
        if (day.equals(today)) {
            LocalTime now = LocalTime.now();
            if (now.getHour() < 23) return day.atTime(now.getHour() + 1, 0);
        }
        return day.atTime(9, 0);
    }

    /** Info-bulle d'une occurrence. */
    static String tooltip(Occurrence o, LocalDate day) {
        StringBuilder sb = new StringBuilder("<html><b>").append(html(o.event.displayTitle())).append("</b><br>");
        sb.append(html(day == null ? Fmt.when(o) : Fmt.whenOn(o, day)));
        if (!o.event.location.trim().isEmpty()) sb.append("<br>").append(html(o.event.location));
        sb.append("<br><font color='#64748b'>").append(html(o.event.category.label));
        if (o.event.isRecurring()) sb.append(" · ").append(html(o.event.recurrence.describe(o.event.start.toLocalDate())));
        sb.append("</font></html>");
        return sb.toString();
    }

    static String html(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
    }
}
