package henry.desktop;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Point d'entrée de « Henry le Hérisson » sur PC.
 *
 * Compilation : voir jouer.sh / jouer.bat
 * Lancement   : java -cp out henry.desktop.Main
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        JavaSound.install();
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Henry le Hérisson");
            GamePanel panel = new GamePanel();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            panel.start();
        });
    }
}
