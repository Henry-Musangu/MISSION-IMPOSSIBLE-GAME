package henry;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import javax.swing.Timer;

/** Composant Swing : boucle de jeu à pas fixe (60 images/s) et affichage mis à l'échelle. */
final class GamePanel extends JPanel {

    private static final double STEP = 1.0 / 60.0;

    private final Game game = new Game();
    private final BufferedImage frame = new BufferedImage(Game.W, Game.H, BufferedImage.TYPE_INT_RGB);
    private long last;
    private double accumulator;

    GamePanel() {
        setPreferredSize(new Dimension(Game.W * 6 / 5, Game.H * 6 / 5));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                game.input.set(e.getKeyCode(), true);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                game.input.set(e.getKeyCode(), false);
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                game.input.releaseAll();
            }
        });
    }

    void start() {
        requestFocusInWindow();
        last = System.nanoTime();
        Timer timer = new Timer(1000 / 120, e -> tick());
        timer.start();
    }

    private void tick() {
        long now = System.nanoTime();
        accumulator += (now - last) / 1e9;
        last = now;
        if (accumulator > 0.25) {
            accumulator = 0.25;
        }
        boolean updated = false;
        while (accumulator >= STEP) {
            game.update();
            accumulator -= STEP;
            updated = true;
        }
        if (updated) {
            Graphics2D g = frame.createGraphics();
            game.render(g);
            g.dispose();
            repaint();
            Toolkit.getDefaultToolkit().sync();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        double scale = Math.min(getWidth() / (double) Game.W, getHeight() / (double) Game.H);
        int w = (int) (Game.W * scale);
        int h = (int) (Game.H * scale);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(frame, (getWidth() - w) / 2, (getHeight() - h) / 2, w, h, null);
    }
}
