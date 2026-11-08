package agenda.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Après un redémarrage, une mise à jour ou un changement d'heure : reprogrammer le prochain rappel. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Reminders.schedule(context);
    }
}
