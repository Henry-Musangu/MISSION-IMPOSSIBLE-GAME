package henry;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Point d'entrée de « Henry le Hérisson ».
 *
 * Compilation : javac -encoding UTF-8 -d out src/henry/*.java
 * Lancement   : java -cp out henry.Main
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Sound.init();
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
