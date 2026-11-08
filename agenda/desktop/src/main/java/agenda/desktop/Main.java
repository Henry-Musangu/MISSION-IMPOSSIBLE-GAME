package agenda.desktop;

import java.io.File;
import java.util.Locale;
import javax.swing.UIManager;

/** Lance l'agenda sur PC. Le fichier de l'agenda peut être choisi avec -Dagenda.file=chemin.ics */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.FRANCE);
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        try {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (os.contains("win") || os.contains("mac")) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } else {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) UIManager.setLookAndFeel(info.getClassName());
                }
            }
        } catch (Exception ignored) {
            // apparence par défaut
        }
        UIManager.put("ToolTip.background", new java.awt.Color(0x1E293B));
        UIManager.put("ToolTip.foreground", java.awt.Color.WHITE);
        UIManager.put("ToolTip.border", javax.swing.BorderFactory.createEmptyBorder(6, 8, 6, 8));
        AgendaFrame.launch(dataFile());
    }

    /** Emplacement du fichier : %APPDATA%\Agenda sous Windows, ~/.agenda ailleurs. */
    static File dataFile() {
        String custom = System.getProperty("agenda.file");
        if (custom != null && !custom.trim().isEmpty()) return new File(custom);
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String appData = System.getenv("APPDATA");
        File dir = os.contains("win") && appData != null ? new File(appData, "Agenda")
                : new File(System.getProperty("user.home"), ".agenda");
        return new File(dir, "agenda.ics");
    }
}
