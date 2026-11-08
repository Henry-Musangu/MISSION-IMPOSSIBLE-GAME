package agenda;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Enregistrement de l'agenda dans un fichier .ics. L'écriture passe par un fichier temporaire
 * (pour ne jamais laisser un fichier à moitié écrit) et garde la version précédente en « .bak ».
 */
public final class AgendaFile {

    private AgendaFile() {
    }

    public static List<Event> load(File file) throws IOException {
        if (!file.exists()) {
            File backup = new File(file.getPath() + ".bak");
            if (!backup.exists()) return new ArrayList<Event>();
            file = backup;
        }
        Reader r = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
        try {
            return Ics.read(r);
        } finally {
            r.close();
        }
    }

    public static synchronized void save(File file, List<Event> events) throws IOException {
        File dir = file.getAbsoluteFile().getParentFile();
        if (dir != null && !dir.exists() && !dir.mkdirs()) {
            throw new IOException("Impossible de créer le dossier " + dir);
        }
        File tmp = new File(file.getPath() + ".tmp");
        Writer w = new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8);
        try {
            Ics.write(events, w);
        } finally {
            w.close();
        }
        if (file.exists()) {
            Files.copy(file.toPath(), new File(file.getPath() + ".bak").toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        try {
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
