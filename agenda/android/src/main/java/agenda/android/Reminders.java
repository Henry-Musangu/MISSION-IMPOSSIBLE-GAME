package agenda.android;

import agenda.Agenda;
import agenda.Fmt;
import agenda.Reminder;
import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Les rappels sur Android : une seule alarme est programmée, pour le prochain rappel. Quand elle
 * sonne, on affiche les notifications dues puis on programme la suivante.
 */
final class Reminders {

    private Reminders() {
    }

    static final String CHANNEL = "rappels";
    private static final String PREFS = "rappels";
    private static final String LAST = "dernier";

    static void createChannel(Context c) {
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Rappels d'événements", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Prévient avant le début de vos événements");
        ch.enableVibration(true);
        c.getSystemService(NotificationManager.class).createNotificationChannel(ch);
    }

    private static long millis(LocalDateTime t) {
        return t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private static LocalDateTime time(long millis) {
        return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), ZoneId.systemDefault());
    }

    /** Heure à partir de laquelle chercher : le dernier contrôle, sans remonter à plus de 2 heures. */
    private static LocalDateTime after(Context c, LocalDateTime now) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long last = p.getLong(LAST, 0);
        if (last == 0) {
            p.edit().putLong(LAST, millis(now)).apply();
            return now;
        }
        LocalDateTime t = time(last);
        LocalDateTime floor = now.minusHours(2);
        return t.isBefore(floor) ? floor : t;
    }

    private static PendingIntent alarmIntent(Context c) {
        Intent i = new Intent(c, ReminderReceiver.class);
        return PendingIntent.getBroadcast(c, 0, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /** Programme (ou annule) l'alarme du prochain rappel. */
    static void schedule(Context c) {
        Agenda agenda = AgendaApp.agenda(c);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = agenda.nextReminderTime(after(c, now));
        AlarmManager am = c.getSystemService(AlarmManager.class);
        PendingIntent pi = alarmIntent(c);
        if (next == null) {
            am.cancel(pi);
            return;
        }
        long at = Math.max(millis(next), System.currentTimeMillis() + 1000);
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            }
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }

    /** L'alarme a sonné : notifier ce qui est dû, puis programmer la suite. */
    static void fire(Context c) {
        Agenda agenda = AgendaApp.agenda(c);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime upTo = now.plusSeconds(30);
        List<Reminder> due = agenda.remindersBetween(after(c, now), upTo);
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(LAST, millis(upTo)).apply();
        for (Reminder r : due) {
            boolean finished = r.occurrence.end.isBefore(now) && r.occurrence.end.isAfter(r.occurrence.start);
            if (!finished) notify(c, r);
        }
        schedule(c);
    }

    private static void notify(Context c, Reminder r) {
        if (Build.VERSION.SDK_INT >= 33
                && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocalDate today = LocalDate.now();
        LocalDate d = r.occurrence.date();
        String rel = Fmt.relativeDay(d, today);
        String text = (rel != null ? rel : Fmt.capitalize(Fmt.shortDate(d))) + " · " + Fmt.when(r.occurrence);
        String place = r.occurrence.event.location.trim();
        if (!place.isEmpty()) text += " · " + place;

        Intent open = new Intent(c, MainActivity.class);
        open.putExtra(MainActivity.EXTRA_DATE, d.toEpochDay());
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int id = r.key().hashCode();
        PendingIntent pi = PendingIntent.getActivity(c, id, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        boolean task = r.occurrence.event.task;
        if (task) text = "Tâche à faire · " + text;
        Notification.Builder b = new Notification.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(android.graphics.drawable.Icon.createWithResource(c, R.drawable.logo))
                .setColor(Ui.BRAND)
                .setContentTitle((task ? "✓ " : "") + r.occurrence.event.displayTitle())
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(
                        r.occurrence.event.notes.trim().isEmpty() ? text : text + "\n" + r.occurrence.event.notes.trim()))
                .setCategory(Notification.CATEGORY_EVENT)
                .setWhen(millis(r.occurrence.start))
                .setShowWhen(true)
                .setAutoCancel(true)
                .setContentIntent(pi);
        if (task) {
            Intent doneIntent = new Intent(c, DoneReceiver.class);
            doneIntent.putExtra(DoneReceiver.EXTRA_UID, r.occurrence.event.uid);
            doneIntent.putExtra(DoneReceiver.EXTRA_DATE, d.toEpochDay());
            doneIntent.putExtra(DoneReceiver.EXTRA_NOTIFICATION, id);
            PendingIntent donePi = PendingIntent.getBroadcast(c, id, doneIntent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            b.addAction(new Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(c, R.drawable.ic_notification), "Fait ✓", donePi).build());
        }
        c.getSystemService(NotificationManager.class).notify(id, b.build());
    }
}
