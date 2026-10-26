package henry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** Logique et rendu du jeu, indépendants de la plateforme (PC ou Android). */
public final class Game {

    /** Taille de l'écran virtuel du jeu, mis à l'échelle par la plateforme. */
    public static final int W = 800;
    public static final int H = 480;
    static final int T = 32;

    private static final int TIME_LIMIT = 10 * 60 * 60;
    private static final int YELLOW = Col.rgb(0xffd43b);

    enum State { TITLE, PLAY, CLEAR, GAME_OVER, VICTORY }

    public final Input input = new Input();
    /** Sur téléphone, les textes parlent de toucher l'écran plutôt que du clavier. */
    private final boolean touchMode;
    private final Input noInput = new Input();
    private final Random random = new Random();

    State state = State.TITLE;
    private int levelIndex;
    Level level;
    Player player;
    private final List<Ring> lostRings = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<Animal> animals = new ArrayList<>();
    private final List<Bomb> bombs = new ArrayList<>();
    private Boss boss;
    private boolean bossDone;

    int score;
    int lives = 3;
    private int bestScore;
    int timeFrames;
    private int tick;
    private double camX;
    private double lead;
    private int introTimer;
    private int stateTimer;
    private boolean paused;
    private int nextLifeRings = 100;

    private boolean hasCheckpoint;
    private double checkpointX;
    private double checkpointY;
    private int checkpointTime;

    private int timeBonus;
    private int ringBonus;

    private final Player titleHenry = new Player(0, 0);

    public Game() {
        this(false);
    }

    public Game(boolean touchMode) {
        this.touchMode = touchMode;
    }

    /** Vrai pendant une partie (utile pour savoir quoi faire du bouton « retour » d'Android). */
    public boolean isPlaying() {
        return state == State.PLAY;
    }

    public boolean isPaused() {
        return state == State.PLAY && paused;
    }

    /** Met le jeu en pause, par exemple quand l'application passe en arrière-plan. */
    public void pause() {
        if (state == State.PLAY) {
            paused = true;
        }
    }

    // ================================================================= mise à jour

    public void update() {
        tick++;
        if (input.mutePressed()) {
            Sound.toggle();
        }
        switch (state) {
            case TITLE:
                updateTitle();
                break;
            case PLAY:
                updatePlay();
                break;
            case CLEAR:
                updateClear();
                break;
            case GAME_OVER:
            case VICTORY:
                stateTimer++;
                if (stateTimer > 60 && input.startPressed()) {
                    state = State.TITLE;
                    stateTimer = 0;
                }
                break;
            default:
                break;
        }
        input.endFrame();
    }

    private void updateTitle() {
        titleHenry.anim += 9;
        titleHenry.vx = 9;
        titleHenry.onGround = true;
        if (input.startPressed()) {
            score = 0;
            lives = 3;
            levelIndex = 0;
            startLevel(true);
        }
    }

    /** Démarre ou redémarre le niveau courant. */
    private void startLevel(boolean fresh) {
        level = Levels.create(levelIndex);
        if (fresh) {
            hasCheckpoint = false;
            timeFrames = 0;
        }
        double sx = level.startX;
        double sy = level.startY;
        if (hasCheckpoint) {
            sx = checkpointX;
            sy = checkpointY;
            timeFrames = checkpointTime;
            for (Checkpoint c : level.checkpoints) {
                if (c.x <= checkpointX) {
                    c.active = true;
                }
            }
        }
        player = new Player(sx, sy);
        lostRings.clear();
        particles.clear();
        animals.clear();
        bombs.clear();
        boss = null;
        bossDone = false;
        nextLifeRings = 100;
        introTimer = 160;
        paused = false;
        lead = 0;
        camX = clampCamera(player.centerX() - W / 2.0);
        state = State.PLAY;
    }

    private void updatePlay() {
        if (input.pausePressed()) {
            paused = !paused;
        }
        if (paused) {
            return;
        }
        if (introTimer > 0) {
            introTimer--;
        }
        Goal goal = level.goal;
        boolean finished = goal != null && goal.passed;
        if (!player.dead && !finished) {
            timeFrames++;
            if (timeFrames >= TIME_LIMIT) {
                player.die();
            }
        }

        player.update(finished ? noInput : input, level);
        if (bossActive()) {
            double left = level.bossArenaX * T;
            if (player.x < left) {
                player.x = left;
                player.vx = Math.max(0, player.vx);
            } else if (player.x > left + W - Player.W) {
                player.x = left + W - Player.W;
                player.vx = Math.min(0, player.vx);
            }
        }

        if (player.dead) {
            if (player.deadTimer > 110) {
                loseLife();
                return;
            }
        } else {
            if (player.y > H + 40) {
                player.die();
            }
            if (player.touchedSpikes) {
                hurtPlayer(player.centerX() + player.facing);
            }
            handleCollisions();
        }

        for (Enemy e : level.enemies) {
            if (e.alive) {
                e.update(level);
            }
        }
        for (Spring s : level.springs) {
            s.update();
        }
        for (Checkpoint c : level.checkpoints) {
            c.update();
        }
        for (Iterator<Ring> it = lostRings.iterator(); it.hasNext();) {
            Ring r = it.next();
            r.updateLost(level);
            if (r.collected || r.life <= 0) {
                it.remove();
            }
        }
        particles.removeIf(p -> !p.update());
        animals.removeIf(a -> !a.update(level));
        bombs.removeIf(b -> !b.update(this, level));
        updateBoss();

        if (goal != null) {
            goal.update();
            if (!goal.passed && !player.dead && player.centerX() >= goal.centerX()) {
                goal.passed = true;
                Sound.play(Sound.GOAL);
            }
            if (goal.passed && goal.spin >= 100 && player.onGround) {
                enterClear();
            }
        }
        updateCamera();
    }

    private void handleCollisions() {
        Box pb = player.bounds();

        for (Ring r : level.rings) {
            if (!r.collected && pb.intersects(r.bounds())) {
                r.collected = true;
                collectRing(r);
            }
        }
        for (Ring r : lostRings) {
            if (r.collectable() && pb.intersects(r.bounds())) {
                r.collected = true;
                collectRing(r);
            }
        }

        for (Enemy e : level.enemies) {
            if (!e.alive || !pb.intersects(e.bounds())) {
                continue;
            }
            boolean stomp = player.vy > 0 && player.bottom() - e.y < 16;
            if (player.attacking() || stomp) {
                e.alive = false;
                player.combo++;
                int[] values = {100, 200, 500, 1000};
                score += values[Math.min(player.combo, values.length) - 1];
                explosion(e.centerX(), e.y + e.h / 2.0, 1);
                animals.add(new Animal(e.centerX(), e.y + e.h, random.nextInt(100)));
                Sound.play(Sound.POP);
                player.bounce(input.jumpHeld());
            } else {
                hurtPlayer(e.centerX());
            }
        }

        for (Spring s : level.springs) {
            if (player.vy >= 0 && pb.intersects(s.bounds())) {
                player.y = s.top() - Player.H;
                player.launch(Spring.POWER);
                s.squash = 12;
                Sound.play(Sound.SPRING);
            }
        }

        for (Checkpoint c : level.checkpoints) {
            if (!c.active && pb.intersects(c.bounds())) {
                c.active = true;
                c.spin = 60;
                hasCheckpoint = true;
                checkpointX = c.x + 5;
                checkpointY = c.base - Player.H;
                checkpointTime = timeFrames;
                Sound.play(Sound.CHECKPOINT);
            }
        }

        for (Bomb b : bombs) {
            if (pb.intersects(b.bounds())) {
                hurtPlayer(b.x);
            }
        }

        if (boss != null && boss.dangerous() && pb.intersects(boss.bounds())) {
            if (player.attacking()) {
                if (boss.flash == 0) {
                    boss.hits++;
                    boss.flash = 40;
                    score += 100;
                    Sound.play(Sound.BOSS_HIT);
                    if (boss.hits >= Boss.MAX_HITS) {
                        boss.defeated = true;
                        score += 1000;
                    }
                }
                player.vx = player.centerX() < boss.centerX() ? -4 : 4;
                player.vy = -5;
            } else {
                hurtPlayer(boss.centerX());
            }
        }
    }

    private void collectRing(Ring r) {
        player.rings++;
        Sound.play(Sound.RING);
        for (int i = 0; i < 4; i++) {
            double a = Math.PI / 2 * i + Math.PI / 4;
            particles.add(new Particle(Particle.SPARK, r.x, r.y, Math.cos(a) * 1.5, Math.sin(a) * 1.5, 14,
                    Col.WHITE));
        }
        if (player.rings >= nextLifeRings) {
            nextLifeRings += 100;
            lives++;
            Sound.play(Sound.ONE_UP);
        }
    }

    private void hurtPlayer(double fromX) {
        if (player.invuln > 0 || player.dead) {
            return;
        }
        if (player.rings > 0) {
            scatterRings();
            player.rings = 0;
            player.knockBack(fromX);
            Sound.play(Sound.LOSE_RINGS);
        } else {
            player.die();
        }
    }

    /** Les anneaux s'échappent en cercle, comme dans les jeux de l'époque. */
    private void scatterRings() {
        int n = Math.min(player.rings, 32);
        double cx = player.centerX();
        double cy = player.y + Player.H / 2.0;
        for (int i = 0; i < n; i++) {
            double a = (i % 16) * Math.PI * 2 / 16 + Math.PI / 32;
            double s = i < 16 ? 4 : 2;
            lostRings.add(new Ring(cx, cy, Math.cos(a) * s, -Math.abs(Math.sin(a)) * s - 1));
        }
    }

    private void loseLife() {
        lives--;
        if (lives <= 0) {
            bestScore = Math.max(bestScore, score);
            state = State.GAME_OVER;
            stateTimer = 0;
        } else {
            startLevel(false);
        }
    }

    private boolean bossActive() {
        return boss != null && !bossDone;
    }

    private void updateBoss() {
        if (level.bossArenaX < 0) {
            return;
        }
        double arenaLeft = level.bossArenaX * T;
        if (boss == null && !bossDone && player.x > arenaLeft + 48) {
            boss = new Boss(arenaLeft);
        }
        if (boss == null) {
            return;
        }
        boss.update(this);
        if (boss.defeated && boss.deadTimer > 200 && !bossDone) {
            bossDone = true;
            level.goal = new Goal(arenaLeft + W - 6 * T, (level.h - 4) * T);
            Sound.play(Sound.CHECKPOINT);
        }
    }

    void dropBomb(double x, double y) {
        bombs.add(new Bomb(x, y));
    }

    void explosion(double x, double y, int size) {
        for (int i = 0; i < 2 * size; i++) {
            particles.add(new Particle(Particle.BOOM, x + random.nextInt(17) - 8, y + random.nextInt(17) - 8,
                    0, -0.5, 24 + random.nextInt(10), Col.ORANGE));
        }
        for (int i = 0; i < 5 * size; i++) {
            particles.add(new Particle(Particle.DEBRIS, x, y, random.nextDouble() * 6 - 3,
                    -random.nextDouble() * 5 - 1, 30, Col.rgb(0x868e96)));
        }
    }

    private void updateCamera() {
        if (bossActive() || bossDone && level.bossArenaX >= 0 && player.x > level.bossArenaX * T) {
            double target = level.bossArenaX * T;
            camX += (target - camX) * 0.08;
            return;
        }
        if (!player.dead) {
            lead += (Math.max(-100, Math.min(100, player.vx * 12)) - lead) * 0.04;
        }
        double target = player.centerX() - W / 2.0 + lead;
        camX += (target - camX) * 0.18;
        camX = clampCamera(camX);
    }

    private double clampCamera(double v) {
        return Math.max(0, Math.min(level.pixelWidth() - W, v));
    }

    private void enterClear() {
        state = State.CLEAR;
        stateTimer = 0;
        int seconds = timeFrames / 60;
        if (seconds < 30) {
            timeBonus = 50000;
        } else if (seconds < 45) {
            timeBonus = 10000;
        } else if (seconds < 60) {
            timeBonus = 5000;
        } else if (seconds < 90) {
            timeBonus = 4000;
        } else if (seconds < 120) {
            timeBonus = 3000;
        } else if (seconds < 180) {
            timeBonus = 1000;
        } else if (seconds < 300) {
            timeBonus = 500;
        } else {
            timeBonus = 0;
        }
        ringBonus = player.rings * 100;
    }

    private void updateClear() {
        stateTimer++;
        level.goal.update();
        if (stateTimer < 90) {
            return;
        }
        if (timeBonus > 0 || ringBonus > 0) {
            int t = Math.min(timeBonus, 200);
            int r = Math.min(ringBonus, 200);
            timeBonus -= t;
            ringBonus -= r;
            score += t + r;
            if (tick % 4 == 0) {
                Sound.play(Sound.TICK);
            }
            if (input.startPressed()) {
                score += timeBonus + ringBonus;
                timeBonus = 0;
                ringBonus = 0;
            }
            stateTimer = 90;
            return;
        }
        if (stateTimer > 200 || (stateTimer > 100 && input.startPressed())) {
            levelIndex++;
            if (levelIndex >= Levels.COUNT) {
                bestScore = Math.max(bestScore, score);
                state = State.VICTORY;
                stateTimer = 0;
                Sound.play(Sound.ONE_UP);
            } else {
                startLevel(true);
            }
        }
    }

    // ================================================================= rendu

    public void render(Gfx g) {
        switch (state) {
            case TITLE:
                renderTitle(g);
                break;
            case GAME_OVER:
                renderGameOver(g);
                break;
            case VICTORY:
                renderVictory(g);
                break;
            default:
                renderWorld(g);
                renderHud(g);
                if (state == State.CLEAR) {
                    renderClear(g);
                } else if (introTimer > 0) {
                    renderTitleCard(g);
                }
                if (paused) {
                    g.setColor(Col.argb(140, 0));
                    g.fillRect(0, 0, W, H);
                    centered(g, "PAUSE", 32, true, Col.WHITE, H / 2);
                    centered(g, touchMode ? "Touche l'écran pour reprendre" : "P ou Échap pour reprendre",
                            16, false, Col.LIGHT_GRAY, H / 2 + 32);
                }
                break;
        }
    }

    private void renderWorld(Gfx g) {
        renderBackground(g, level.theme, camX);
        renderTiles(g);
        for (Checkpoint c : level.checkpoints) {
            c.draw(g, camX);
        }
        if (level.goal != null) {
            level.goal.draw(g, camX);
        }
        for (Spring s : level.springs) {
            if (visible(s.x, T)) {
                s.draw(g, camX);
            }
        }
        for (Ring r : level.rings) {
            if (!r.collected && visible(r.x - Ring.SIZE, Ring.SIZE * 2)) {
                r.draw(g, camX, tick);
            }
        }
        for (Ring r : lostRings) {
            r.draw(g, camX, tick);
        }
        for (Enemy e : level.enemies) {
            if (e.alive && visible(e.x, e.w)) {
                e.draw(g, camX);
            }
        }
        for (Animal a : animals) {
            a.draw(g, camX);
        }
        for (Bomb b : bombs) {
            b.draw(g, camX, tick);
        }
        if (boss != null) {
            boss.draw(g, camX, tick);
        }
        player.draw(g, camX, tick);
        for (Particle p : particles) {
            p.draw(g, camX);
        }
    }

    private boolean visible(double x, double w) {
        return x + w >= camX - 32 && x <= camX + W + 32;
    }

    private void renderTiles(Gfx g) {
        Theme th = level.theme;
        int cam = (int) Math.round(camX);
        int x0 = Math.max(0, cam / T);
        int x1 = Math.min(level.w - 1, (cam + W) / T + 1);
        for (int ty = 0; ty < level.h; ty++) {
            for (int tx = x0; tx <= x1; tx++) {
                byte t = level.tile(tx, ty);
                int sx = tx * T - cam;
                int sy = ty * T;
                if (t == Level.SOLID) {
                    boolean top = level.tile(tx, ty - 1) != Level.SOLID;
                    g.drawSprite(top ? th.top(g) : th.fill(g), sx, sy);
                    if (top && level.tile(tx, ty - 1) == Level.EMPTY) {
                        decorate(g, th, tx, sx, sy);
                    }
                } else if (t == Level.ONEWAY) {
                    g.drawSprite(th.platform(g), sx, sy);
                } else if (t == Level.SPIKE) {
                    g.drawSprite(th.spikes(g), sx, sy);
                }
            }
        }
    }

    /** Fleurs, cactus et lampadaires posés sur le sol. */
    private void decorate(Gfx g, Theme th, int tx, int sx, int sy) {
        int h = hash(tx);
        if (h % 6 != 0) {
            return;
        }
        int cx = sx + 8 + (h >> 4) % 16;
        if (th.style == Theme.MEADOW) {
            g.setColor(Col.rgb(0x2b8a3e));
            g.fillRect(cx - 1, sy - 14, 2, 14);
            g.setColor((h >> 8) % 2 == 0 ? Col.rgb(0xffd43b) : Col.rgb(0xf783ac));
            int sway = (int) (Math.sin(tick * 0.05 + tx) * 1.5);
            for (int i = 0; i < 5; i++) {
                double a = i * Math.PI * 2 / 5;
                g.fillOval(cx + sway + (int) (Math.cos(a) * 4) - 3, sy - 18 + (int) (Math.sin(a) * 4) - 3, 6, 6);
            }
            g.setColor(Col.rgb(0x7048e8));
            g.fillOval(cx + sway - 2, sy - 20, 5, 5);
        } else if (th.style == Theme.CANYON) {
            g.setColor(Col.rgb(0x2f9e44));
            g.fillRoundRect(cx - 4, sy - 26, 8, 26, 8);
            g.fillRoundRect(cx - 12, sy - 18, 6, 10, 6);
            g.fillRect(cx - 9, sy - 11, 6, 3);
            g.fillRoundRect(cx + 6, sy - 22, 6, 10, 6);
            g.fillRect(cx + 3, sy - 15, 6, 3);
        } else {
            g.setColor(Col.rgb(0x495057));
            g.fillRect(cx - 2, sy - 40, 4, 40);
            g.fillRect(cx - 2, sy - 40, 12, 3);
            g.setColor((tick / 30 + tx) % 2 == 0 ? Col.rgb(0xffe066) : Col.rgb(0xffa94d));
            g.fillOval(cx + 6, sy - 38, 6, 6);
        }
    }

    private static int hash(int v) {
        int h = v * 0x45d9f3b;
        h ^= h >>> 16;
        h *= 0x45d9f3b;
        h ^= h >>> 16;
        return h & 0x7fffffff;
    }

    private void renderBackground(Gfx g, Theme th, double cam) {
        g.fillVerticalGradient(0, 0, W, H, th.skyTop, th.skyBottom);
        if (th.style == Theme.MEADOW) {
            g.setColor(Col.argb(200, 0xffffff));
            for (int i = 0; i < 8; i++) {
                double cx = wrap(i * 180 - cam * 0.1 - tick * 0.15, W + 200) - 100;
                int cy = 40 + (hash(i) % 90);
                g.fillOval((int) cx, cy, 90, 26);
                g.fillOval((int) cx + 20, cy - 12, 50, 30);
            }
            drawHills(g, cam, 0.2, 300, 50, th.far, 0.006);
            drawHills(g, cam, 0.4, 350, 35, th.near, 0.011);
        } else if (th.style == Theme.CANYON) {
            g.setColor(Col.rgb(0xffe8a3));
            g.fillOval(560, 150, 110, 110);
            drawMesas(g, cam, 0.15, 260, th.far);
            drawMesas(g, cam, 0.35, 330, th.near);
        } else {
            g.setColor(Col.WHITE);
            for (int i = 0; i < 60; i++) {
                int sx = (int) wrap(hash(i) % 1600 - cam * 0.05, W);
                int sy = hash(i + 99) % 250;
                g.fillRect(sx, sy, (i % 5 == 0) ? 2 : 1, (i % 5 == 0) ? 2 : 1);
            }
            drawFactories(g, cam, 0.2, th.far, 230);
            drawFactories(g, cam, 0.4, th.near, 300);
        }
    }

    private static double wrap(double v, double m) {
        double r = v % m;
        return r < 0 ? r + m : r;
    }

    private void drawHills(Gfx g, double cam, double factor, int base, int amp, int color, double freq) {
        int n = W / 8 + 1;
        double[] xs = new double[n + 2];
        double[] ys = new double[n + 2];
        xs[0] = 0;
        ys[0] = H;
        for (int i = 0; i < n; i++) {
            int x = i * 8;
            double wx = x + cam * factor;
            xs[i + 1] = x;
            ys[i + 1] = (int) (base - Math.abs(Math.sin(wx * freq)) * amp - Math.sin(wx * freq * 2.3) * amp * 0.3);
        }
        xs[n + 1] = W;
        ys[n + 1] = H;
        g.setColor(color);
        g.fillPolygon(xs, ys, n + 2);
    }

    private void drawMesas(Gfx g, double cam, double factor, int base, int color) {
        g.setColor(color);
        double off = cam * factor;
        int first = (int) Math.floor(off / 80) - 1;
        for (int i = first; i < first + W / 80 + 3; i++) {
            int h = hash(i + (int) (factor * 100)) % 120;
            int x = (int) (i * 80 - off);
            g.fillRect(x, base - h, 82, H);
            g.fillRect(x - 8, base - h, 98, 10);
        }
    }

    private void drawFactories(Gfx g, double cam, double factor, int color, int base) {
        double off = cam * factor;
        int first = (int) Math.floor(off / 100) - 1;
        for (int i = first; i < first + W / 100 + 3; i++) {
            int h = 40 + hash(i * 7 + (int) (factor * 10)) % 140;
            int x = (int) (i * 100 - off);
            g.setColor(color);
            g.fillRect(x, base - h, 90, H);
            if (hash(i) % 3 == 0) {
                g.fillRect(x + 60, base - h - 60, 14, 60);
                g.setColor(Col.rgba(120, 120, 140, 90));
                int puff = (tick / 2 + i * 13) % 60;
                g.fillOval(x + 54 + puff / 3, base - h - 75 - puff, 24 + puff / 3, 18 + puff / 4);
            }
            g.setColor(Col.rgb(0xffe066));
            for (int wy = base - h + 12; wy < base; wy += 22) {
                for (int wx = x + 10; wx < x + 80; wx += 20) {
                    if (hash(wx * 31 + wy) % 3 == 0) {
                        g.fillRect(wx, wy, 8, 8);
                    }
                }
            }
        }
    }

    private void renderHud(Gfx g) {
        int seconds = timeFrames / 60;
        String time = (seconds / 60) + ":" + (seconds % 60 < 10 ? "0" : "") + (seconds % 60);
        hudLine(g, "SCORE", String.valueOf(score), 30, YELLOW);
        hudLine(g, "TEMPS", time, 56, YELLOW);
        boolean warn = player.rings == 0 && (tick / 15) % 2 == 0;
        hudLine(g, "ANNEAUX", String.valueOf(player.rings), 82, warn ? Col.RED : YELLOW);

        // vies (sur téléphone, en haut : le bas de l'écran est occupé par les boutons)
        int bx = 32;
        int by = touchMode ? 112 : H - 22;
        g.setColor(Player.BODY);
        g.fillOval(bx - 12, by - 12, 24, 24);
        g.setColor(Player.SKIN);
        g.fillOval(bx + 1, by - 3, 10, 8);
        g.setColor(Col.WHITE);
        g.fillOval(bx, by - 9, 7, 8);
        g.setColor(Col.BLACK);
        g.fillOval(bx + 3, by - 7, 3, 5);
        g.setFont(16, false);
        shadowText(g, "HENRY", bx + 18, by - 2, YELLOW);
        shadowText(g, "x " + lives, bx + 18, by + 14, Col.WHITE);
        if (!Sound.isEnabled() && !touchMode) {
            shadowText(g, "SON COUPÉ", W - 110, H - 10, Col.WHITE);
        }
    }

    private void hudLine(Gfx g, String label, String value, int y, int labelColor) {
        g.setFont(20, true);
        shadowText(g, label, 20, y, labelColor);
        shadowText(g, value, 145, y, Col.WHITE);
    }

    private static void shadowText(Gfx g, String s, double x, double y, int c) {
        g.setColor(Col.BLACK);
        g.drawText(s, x + 2, y + 2);
        g.setColor(c);
        g.drawText(s, x, y);
    }

    private static void centered(Gfx g, String s, float size, boolean italic, int c, int y) {
        g.setFont(size, italic);
        shadowText(g, s, (W - g.textWidth(s)) / 2, y, c);
    }

    private void renderTitleCard(Gfx g) {
        int t = 160 - introTimer;
        double in = Math.min(1, t / 20.0);
        double out = Math.min(1, introTimer / 20.0);
        double k = Math.min(in, out);
        int bandX = (int) (-260 + 260 * k);
        g.setColor(Col.rgb(0x1c7ed6));
        g.fillPolygon(new double[] {bandX, bandX + 220, bandX + 160, bandX}, new double[] {0, 0, H, H}, 4);
        g.setColor(YELLOW);
        g.fillRect(0, (int) (H - 90 * k), W, 14);
        int tx = (int) (W / 2.0 + 60 + (1 - k) * W);
        g.setFont(32, true);
        float zw = g.textWidth(level.zone);
        shadowText(g, level.zone, tx - zw / 2, 210, Col.WHITE);
        g.setFont(20, true);
        shadowText(g, "ACTE " + level.act, tx + zw / 2 - g.textWidth("ACTE 1"), 250, YELLOW);
    }

    private void renderClear(Gfx g) {
        int slide = Math.min(stateTimer, 30);
        int off = (30 - slide) * 25;
        g.setColor(Col.argb(110, 0));
        g.fillRoundRect(190, 105, 420, 235, 30);
        g.setFont(32, true);
        String title = levelIndex == Levels.COUNT - 1 ? "HENRY A SAUVÉ LE MONDE !" : "HENRY A RÉUSSI !";
        shadowText(g, title, (W - g.textWidth(title)) / 2 - off, 150, Col.WHITE);
        g.setFont(20, true);
        shadowText(g, level.zone, (W - g.textWidth(level.zone)) / 2 + off, 185, YELLOW);
        if (stateTimer >= 30) {
            row(g, "SCORE", score, 250);
            row(g, "BONUS TEMPS", timeBonus, 285);
            row(g, "BONUS ANNEAUX", ringBonus, 320);
        }
    }

    private static void row(Gfx g, String label, int value, int y) {
        g.setFont(20, true);
        shadowText(g, label, 240, y, YELLOW);
        String v = String.valueOf(value);
        shadowText(g, v, 560 - g.textWidth(v), y, Col.WHITE);
    }

    private void renderTitle(Gfx g) {
        double cam = tick * 4;
        Theme th = Theme.PRAIRIE;
        renderBackground(g, th, cam);
        int ground = 380;
        int off = (int) (cam % T);
        for (int x = -off; x < W; x += T) {
            g.drawSprite(th.top(g), x, ground);
            g.drawSprite(th.fill(g), x, ground + T);
            g.drawSprite(th.fill(g), x, ground + 2 * T);
            g.drawSprite(th.fill(g), x, ground + 3 * T);
        }
        titleHenry.x = W / 2.0 - Player.W / 2.0;
        titleHenry.y = ground - Player.H;
        g.save();
        g.translate(W / 2.0, ground);
        g.scale(2, 2);
        g.translate(-W / 2.0, -ground);
        titleHenry.draw(g, 0, tick);
        g.restore();

        // logo
        int bob = (int) (Math.sin(tick * 0.05) * 4);
        g.setColor(Col.rgb(0x1c2f8a));
        g.fillRoundRect(170, 40 + bob, 460, 150, 60);
        g.setColor(YELLOW);
        g.setStroke(5f);
        g.drawRoundRect(170, 40 + bob, 460, 150, 60);
        g.setStroke(1f);
        centered(g, "HENRY", 84, true, Col.WHITE, 125 + bob);
        centered(g, "LE HÉRISSON", 32, true, YELLOW, 170 + bob);

        if ((tick / 30) % 2 == 0) {
            centered(g, touchMode ? "TOUCHE L'ÉCRAN POUR JOUER" : "APPUIE SUR ENTRÉE", 20, true, Col.WHITE, 235);
        }
        g.setColor(Col.argb(120, 0));
        g.fillRoundRect(110, 250, 580, 20, 10);
        centered(g, touchMode
                ? "◀ ▶ : courir    A : sauter    ▼ : rouler    ▼ + A : spin dash"
                : "←/→ ou Q/D : courir   ESPACE : sauter   ↓ : rouler   ↓+ESPACE : spin dash",
                13, false, Col.WHITE, 265);
        if (bestScore > 0) {
            centered(g, "MEILLEUR SCORE : " + bestScore, 16, false, YELLOW, 22);
        }
    }

    private void renderGameOver(Gfx g) {
        g.setColor(Col.BLACK);
        g.fillRect(0, 0, W, H);
        int slide = Math.max(0, 40 - stateTimer) * 12;
        g.setFont(64, true);
        shadowText(g, "GAME", W / 2 - g.textWidth("GAME") - 10 - slide, 230, YELLOW);
        shadowText(g, "OVER", W / 2 + 10 + slide, 230, YELLOW);
        centered(g, "Score : " + score, 20, true, Col.WHITE, 290);
        if (stateTimer > 60) {
            centered(g, touchMode ? "Touche l'écran pour revenir au titre" : "Entrée pour revenir au titre",
                    16, false, Col.LIGHT_GRAY, 340);
        }
    }

    private void renderVictory(Gfx g) {
        renderBackground(g, Theme.PRAIRIE, tick);
        centered(g, "BRAVO HENRY !", 64, true, YELLOW, 150);
        centered(g, "Le Dr. Vortex est vaincu et les animaux sont libres !", 20, true, Col.WHITE, 210);
        centered(g, "Score final : " + score, 32, true, Col.WHITE, 280);
        if (stateTimer > 60) {
            centered(g, touchMode ? "Touche l'écran pour revenir au titre" : "Entrée pour revenir au titre",
                    16, false, Col.WHITE, 340);
        }
    }
}
