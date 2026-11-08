package agenda.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Reçoit l'alarme du prochain rappel. */
public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Reminders.fire(context);
    }
}
