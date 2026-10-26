package henry.desktop;

import henry.Game;
import henry.Input;
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

/** Composant Swing : boucle de jeu à pas fixe (60 images/s), clavier et affichage mis à l'échelle. */
final class GamePanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final double STEP = 1.0 / 60.0;

    /**
     * Touches associées à chaque bouton du jeu : flèches, ZQSD (AZERTY) et WASD (QWERTY).
     * Saut : Espace, X ou K.
     */
    private static final int[][] KEYS = new int[Input.COUNT][];

    static {
        KEYS[Input.LEFT] = new int[] {KeyEvent.VK_LEFT, KeyEvent.VK_Q, KeyEvent.VK_A};
        KEYS[Input.RIGHT] = new int[] {KeyEvent.VK_RIGHT, KeyEvent.VK_D};
        KEYS[Input.UP] = new int[] {KeyEvent.VK_UP, KeyEvent.VK_Z, KeyEvent.VK_W};
        KEYS[Input.DOWN] = new int[] {KeyEvent.VK_DOWN, KeyEvent.VK_S};
        KEYS[Input.JUMP] = new int[] {KeyEvent.VK_SPACE, KeyEvent.VK_X, KeyEvent.VK_K};
        KEYS[Input.START] = new int[] {KeyEvent.VK_ENTER};
        KEYS[Input.PAUSE] = new int[] {KeyEvent.VK_P, KeyEvent.VK_ESCAPE};
        KEYS[Input.MUTE] = new int[] {KeyEvent.VK_M};
    }

    private final transient Game game = new Game(false, new PrefsStorage());
    private final transient BufferedImage frame = new BufferedImage(Game.W, Game.H, BufferedImage.TYPE_INT_RGB);
    private final boolean[] keys = new boolean[1024];
    private long last;
    private double accumulator;

    GamePanel() {
        setPreferredSize(new Dimension(Game.W * 6 / 5, Game.H * 6 / 5));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                key(e.getKeyCode(), true);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                key(e.getKeyCode(), false);
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                java.util.Arrays.fill(keys, false);
                game.input.releaseAll();
            }
        });
    }

    private void key(int code, boolean pressed) {
        if (code < 0 || code >= keys.length) {
            return;
        }
        keys[code] = pressed;
        for (int b = 0; b < KEYS.length; b++) {
            boolean any = false;
            for (int k : KEYS[b]) {
                any |= keys[k];
            }
            game.input.set(b, any);
        }
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
            game.render(new AwtGfx(g));
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
