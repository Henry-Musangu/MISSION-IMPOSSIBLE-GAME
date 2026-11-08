package agenda.android;

import agenda.Agenda;
import agenda.Event;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import java.time.LocalDate;

/** Bouton « Fait ✓ » d'une notification de tâche : coche la tâche et retire la notification. */
public class DoneReceiver extends BroadcastReceiver {

    static final String EXTRA_UID = "uid";
    static final String EXTRA_DATE = "date";
    static final String EXTRA_NOTIFICATION = "notification";

    @Override
    public void onReceive(Context context, Intent intent) {
        Agenda agenda = AgendaApp.agenda(context);
        Event e = agenda.find(intent.getStringExtra(EXTRA_UID));
        if (e != null) {
            agenda.setDone(e, LocalDate.ofEpochDay(intent.getLongExtra(EXTRA_DATE, 0)), true);
            Toast.makeText(context, "« " + e.displayTitle() + " » : fait ✓", Toast.LENGTH_SHORT).show();
        }
        context.getSystemService(NotificationManager.class).cancel(intent.getIntExtra(EXTRA_NOTIFICATION, 0));
    }
}
