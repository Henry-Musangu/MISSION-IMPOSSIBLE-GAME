package agenda.android;

import agenda.Agenda;
import agenda.AgendaFile;
import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

/**
 * L'application : charge l'agenda au démarrage, l'enregistre à chaque modification
 * et reprogramme le prochain rappel.
 */
public class AgendaApp extends Application {

    private final Agenda agenda = new Agenda();
    private File file;

    @Override
    public void onCreate() {
        super.onCreate();
        file = new File(getFilesDir(), "agenda.ics");
        try {
            agenda.setAll(AgendaFile.load(file));
        } catch (IOException e) {
            Log.e("Agenda", "Lecture impossible", e);
            Toast.makeText(this, "Impossible de lire l'agenda : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        agenda.addListener(new Runnable() {
            @Override
            public void run() {
                save();
                Reminders.schedule(AgendaApp.this);
            }
        });
        Reminders.createChannel(this);
        Reminders.schedule(this);
    }

    private void save() {
        try {
            AgendaFile.save(file, agenda.events());
        } catch (IOException e) {
            Log.e("Agenda", "Enregistrement impossible", e);
            Toast.makeText(this, "Impossible d'enregistrer l'agenda : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /** L'agenda partagé par tous les écrans. */
    static Agenda agenda(Context context) {
        return ((AgendaApp) context.getApplicationContext()).agenda;
    }
}
