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
    private static final int FADE = 24;

    enum State { TITLE, PLAY, CLEAR, GAME_OVER, VICTORY }

    public final Input input = new Input();
    /** Sur téléphone, les textes parlent de toucher l'écran plutôt que du clavier. */
    private final boolean touchMode;
    private final Storage storage;
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
    private final List<Shot> shots = new ArrayList<>();
    private final List<Popup> popups = new ArrayList<>();
    private Boss boss;
    private boolean bossDone;

    int score;
    int lives = 3;
    private int bestScore;
    /** Dernier acte débloqué (0 à COUNT-1). */
    private int unlocked;
    int timeFrames;
    private int tick;
    private double camX;
    private double camY;
    private double lead;
    private int shake;
    private int introTimer;
    private int stateTimer;
    private int fadeIn;
    private int fadeOut;
    private boolean paused;
    private int nextLifeRings = 100;

    private boolean hasCheckpoint;
    private double checkpointX;
    private double checkpointY;
    private int checkpointTime;

    private int timeBonus;
    private int ringBonus;

    private int menuIndex;
    private int menuLevel;

    private final Player titleHenry = new Player(0, 0);

    public Game() {
        this(false, new Storage.Memory());
    }

    public Game(boolean touchMode, Storage storage) {
        this.touchMode = touchMode;
        this.storage = storage;
        bestScore = storage.getInt("meilleur", 0);
        unlocked = Math.max(0, Math.min(Levels.COUNT - 1, storage.getInt("niveau", 0)));
        menuLevel = unlocked;
        Sound.setEnabled(storage.getInt("son", 1) == 1);
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
            toggleSound();
        }
        if (shake > 0) {
            shake--;
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
                if (stateTimer > 60 && (input.startPressed() || input.anyTap())) {
                    state = State.TITLE;
                    stateTimer = 0;
                    menuLevel = unlocked;
                }
                break;
            default:
                break;
        }
        updateMusic();
        input.endFrame();
    }

    private void toggleSound() {
        Sound.toggle();
        storage.putInt("son", Sound.isEnabled() ? 1 : 0);
    }

    private void updateMusic() {
        int track;
        switch (state) {
            case TITLE:
            case VICTORY:
                track = Music.TITLE;
                break;
            case PLAY:
                if (boss != null && !boss.defeated) {
                    track = Music.BOSS;
                } else if (bossDone || (level.goal != null && level.goal.passed)) {
                    track = -1;
                } else {
                    track = Music.PRAIRIE + level.zoneIndex;
                }
                break;
            default:
                track = -1;
                break;
        }
        Sound.music(track);
        Sound.pauseMusic(state == State.PLAY && (paused || (player != null && player.dead)));
    }

    // ----------------------------------------------------------------- menu titre

    private static final int MENU_Y = 238;
    private static final int MENU_GAP = 40;
    private static final int MENU_W = 400;

    private void updateTitle() {
        titleHenry.anim += 9;
        titleHenry.gsp = 9;
        titleHenry.onGround = true;
        int items = 3;
        if (input.upPressed()) {
            menuIndex = (menuIndex + items - 1) % items;
        }
        if (input.downPressed()) {
            menuIndex = (menuIndex + 1) % items;
        }
        if (menuIndex == 1) {
            if (input.leftPressed()) {
                menuLevel = Math.max(0, menuLevel - 1);
            }
            if (input.rightPressed()) {
                menuLevel = Math.min(unlocked, menuLevel + 1);
            }
        }
        // toucher direct sur les lignes du menu
        double mx = (W - MENU_W) / 2.0;
        for (int i = 0; i < items; i++) {
            double my = MENU_Y + i * MENU_GAP - 26;
            if (input.tappedIn(mx, my, MENU_W, 34)) {
                menuIndex = i;
                if (i == 1 && input.tappedIn(mx, my, 70, 34)) {
                    menuLevel = Math.max(0, menuLevel - 1);
                } else if (i == 1 && input.tappedIn(mx + MENU_W - 70, my, 70, 34)) {
                    menuLevel = Math.min(unlocked, menuLevel + 1);
                } else {
                    activateMenu(i);
                }
                return;
            }
        }
        if (input.startPressed()) {
            activateMenu(menuIndex);
        }
    }

    private void activateMenu(int item) {
        if (item == 2) {
            toggleSound();
            return;
        }
        if (item == 1 && unlocked == 0) {
            return;
        }
        score = 0;
        lives = 3;
        levelIndex = item == 0 ? 0 : menuLevel;
        startLevel(true);
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
        shots.clear();
        popups.clear();
        boss = null;
        bossDone = false;
        nextLifeRings = 100;
        introTimer = 160;
        fadeIn = FADE;
        fadeOut = 0;
        paused = false;
        lead = 0;
        camX = clampCamX(player.centerX() - W / 2.0);
        camY = clampCamY(targetCamY());
        state = State.PLAY;
    }

    // ----------------------------------------------------------------- partie

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
        if (fadeIn > 0) {
            fadeIn--;
        }
        Goal goal = level.goal;
        boolean finished = goal != null && goal.passed;
        if (!player.dead && !finished) {
            timeFrames++;
            if (timeFrames >= TIME_LIMIT) {
                player.die();
            }
        }

        for (Platform p : level.platforms) {
            p.update();
        }
        player.update(finished ? noInput : input, level);
        if (bossActive()) {
            double left = level.bossArenaX * T;
            if (player.x < left) {
                player.pushOutX(left);
            } else if (player.x > left + W - Player.W) {
                player.pushOutX(left + W - Player.W);
            }
        }

        if (player.dead) {
            if (player.deadTimer == 1) {
                Sound.vibrate(250);
            }
            if (player.deadTimer > 110) {
                loseLife();
                return;
            }
        } else {
            if (player.y > level.pixelHeight() + 40) {
                player.die();
            }
            if (player.touchedSpikes) {
                hurtPlayer(player.centerX() + player.facing);
            }
            handleCollisions();
            effects();
        }

        for (Enemy e : level.enemies) {
            if (e.alive && Math.abs(e.x - player.x) < W * 1.5) {
                e.update(level, this);
            }
        }
        for (Spring s : level.springs) {
            s.update();
        }
        for (Checkpoint c : level.checkpoints) {
            c.update();
        }
        for (Monitor m : level.monitors) {
            m.update();
            if (m.broken && !m.given && m.popTimer >= 30) {
                m.given = true;
                givePowerUp(m);
            }
        }
        for (Iterator<Ring> it = lostRings.iterator(); it.hasNext();) {
            Ring r = it.next();
            r.updateLost(level);
            if (r.collected || r.life <= 0) {
                it.remove();
            }
        }
        for (Iterator<Particle> it = particles.iterator(); it.hasNext();) {
            if (!it.next().update()) {
                it.remove();
            }
        }
        for (Iterator<Animal> it = animals.iterator(); it.hasNext();) {
            if (!it.next().update(level)) {
                it.remove();
            }
        }
        for (Iterator<Bomb> it = bombs.iterator(); it.hasNext();) {
            if (!it.next().update(this, level)) {
                it.remove();
            }
        }
        for (Iterator<Shot> it = shots.iterator(); it.hasNext();) {
            if (!it.next().update(level)) {
                it.remove();
            }
        }
        for (Iterator<Popup> it = popups.iterator(); it.hasNext();) {
            if (!it.next().update()) {
                it.remove();
            }
        }
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
                collectRing(r.x, r.y);
            }
        }
        for (Ring r : lostRings) {
            if (r.collectable() && pb.intersects(r.bounds())) {
                r.collected = true;
                collectRing(r.x, r.y);
            }
        }

        for (Enemy e : level.enemies) {
            if (!e.alive || !pb.intersects(e.bounds())) {
                continue;
            }
            boolean stomp = player.vy > 0 && !player.onGround && player.bottom() - e.y < 16;
            if (player.attacking() || stomp) {
                e.alive = false;
                player.combo++;
                int[] values = {100, 200, 500, 1000};
                int points = values[Math.min(player.combo, values.length) - 1];
                score += points;
                popups.add(new Popup(String.valueOf(points), e.centerX(), e.y - 6));
                explosion(e.centerX(), e.y + e.h / 2.0, 1);
                animals.add(new Animal(e.centerX(), e.y + e.h, random.nextInt(100)));
                Sound.play(Sound.POP);
                player.bounce(input.jumpHeld());
            } else {
                hurtPlayer(e.centerX());
            }
        }

        for (Monitor m : level.monitors) {
            if (m.broken || !pb.intersects(m.bounds())) {
                continue;
            }
            boolean fromAbove = player.vy > 0 && !player.onGround && player.bottom() - (m.base - Monitor.H) < 14;
            if (player.attacking() || fromAbove) {
                m.broken = true;
                score += 10;
                explosion(m.centerX(), m.base - Monitor.H / 2.0, 1);
                Sound.play(Sound.POP);
                player.bounce(input.jumpHeld());
            } else if (player.centerX() < m.centerX()) {
                player.pushOutX(m.x - Player.W);
            } else {
                player.pushOutX(m.x + Monitor.W);
            }
        }

        for (Spring s : level.springs) {
            if ((player.vy >= 0 || player.onGround) && pb.intersects(s.bounds())) {
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
        for (Shot s : shots) {
            if (pb.intersects(s.bounds())) {
                hurtPlayer(s.x);
            }
        }

        if (boss != null && boss.dangerous() && pb.intersects(boss.bounds())) {
            if (player.attacking()) {
                if (boss.flash == 0) {
                    boss.hits++;
                    boss.flash = 40;
                    score += 100;
                    shake(10);
                    Sound.play(Sound.BOSS_HIT);
                    Sound.vibrate(40);
                    if (boss.hits >= boss.maxHits) {
                        boss.defeated = true;
                        score += 1000;
                        popups.add(new Popup("1000", boss.centerX(), boss.y - 10));
                    }
                }
                player.bounceOff(player.centerX() < boss.centerX() ? -1 : 1);
            } else {
                hurtPlayer(boss.centerX());
            }
        }
    }

    /** Poussière quand Henry dérape, charge un spin dash ou atterrit vite. */
    private void effects() {
        double fx = player.centerX();
        double fy = player.bottom();
        if (player.skidding && tick % 3 == 0) {
            particles.add(new Particle(Particle.DUST, fx - player.facing * 8, fy - 3,
                    -player.facing * 0.5, -0.6, 22, 0));
            if (tick % 9 == 0) {
                Sound.play(Sound.TICK);
            }
        }
        if (player.spinDash && tick % 4 == 0) {
            particles.add(new Particle(Particle.DUST, fx - player.facing * 14, fy - 4,
                    -player.facing * 1.8, -0.5, 20, 0));
        }
        if (player.landed && player.speed() > 5) {
            for (int i = -1; i <= 1; i += 2) {
                particles.add(new Particle(Particle.DUST, fx + i * 8, fy - 3, i * 1.2, -0.4, 18, 0));
            }
        }
        if (player.stars > 0 && tick % 5 == 0) {
            particles.add(new Particle(Particle.SPARK, fx + random.nextInt(31) - 15,
                    player.y + random.nextInt(Player.H), 0, -0.5, 18, YELLOW));
        }
    }

    private void collectRing(double x, double y) {
        player.rings++;
        Sound.play(Sound.RING);
        for (int i = 0; i < 4; i++) {
            double a = Math.PI / 2 * i + Math.PI / 4;
            particles.add(new Particle(Particle.SPARK, x, y, Math.cos(a) * 1.5, Math.sin(a) * 1.5, 14,
                    Col.WHITE));
        }
        if (player.rings >= nextLifeRings) {
            nextLifeRings += 100;
            lives++;
            Sound.play(Sound.ONE_UP);
            popups.add(new Popup("1 VIE !", player.centerX(), player.y - 10));
        }
    }

    private void givePowerUp(Monitor m) {
        String text;
        switch (m.type) {
            case Monitor.RINGS:
                for (int i = 0; i < 10; i++) {
                    collectRing(m.centerX(), m.base - Monitor.H);
                }
                text = "+10";
                break;
            case Monitor.SHIELD:
                player.shield = true;
                Sound.play(Sound.CHECKPOINT);
                text = "BOUCLIER";
                break;
            case Monitor.SHOES:
                player.shoes = Player.SHOES_TIME;
                Sound.play(Sound.DASH);
                text = "VITESSE !";
                break;
            case Monitor.STARS:
                player.stars = Player.STARS_TIME;
                Sound.play(Sound.ONE_UP);
                text = "INVINCIBLE !";
                break;
            default:
                lives++;
                Sound.play(Sound.ONE_UP);
                text = "1 VIE !";
                break;
        }
        popups.add(new Popup(text, m.centerX(), m.base - Monitor.H - 30));
    }

    private void hurtPlayer(double fromX) {
        if (player.invuln > 0 || player.dead || player.stars > 0) {
            return;
        }
        if (player.shield) {
            player.shield = false;
            player.knockBack(fromX);
            Sound.play(Sound.BOSS_HIT);
            Sound.vibrate(50);
        } else if (player.rings > 0) {
            scatterRings();
            player.rings = 0;
            player.knockBack(fromX);
            Sound.play(Sound.LOSE_RINGS);
            Sound.vibrate(80);
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
            saveBest();
            state = State.GAME_OVER;
            stateTimer = 0;
        } else {
            startLevel(false);
        }
    }

    private void saveBest() {
        if (score > bestScore) {
            bestScore = score;
            storage.putInt("meilleur", bestScore);
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
        double groundY = level.bossGroundRow * T;
        if (boss == null && !bossDone && player.x > arenaLeft + 48) {
            boss = new Boss(arenaLeft, groundY, level.zoneIndex);
        }
        if (boss == null) {
            return;
        }
        boss.update(this);
        if (boss.defeated && boss.deadTimer > 200 && !bossDone) {
            bossDone = true;
            level.goal = new Goal(arenaLeft + W - 6 * T, groundY);
            Sound.play(Sound.CHECKPOINT);
        }
    }

    void dropBomb(double x, double y) {
        bombs.add(new Bomb(x, y));
    }

    void shoot(int kind, double x, double y, double vx, double vy) {
        if (Math.abs(x - player.x) < W) {
            shots.add(new Shot(kind, x, y, vx, vy));
        }
    }

    void shake(int frames) {
        shake = Math.max(shake, frames);
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

    // ----------------------------------------------------------------- caméra

    private double targetCamY() {
        double look = player.lookingUp ? -110 : (player.crouching ? 90 : 0);
        return player.y + Player.H / 2.0 - H / 2.0 - 20 + look;
    }

    private void updateCamera() {
        boolean arena = level.bossArenaX >= 0 && (bossActive()
                || (bossDone && player.x > level.bossArenaX * T));
        if (arena) {
            camX += (level.bossArenaX * T - camX) * 0.08;
            camY += (clampCamY(level.bossGroundRow * T - 400) - camY) * 0.08;
            return;
        }
        if (!player.dead) {
            lead += (Math.max(-100, Math.min(100, player.vx * 12)) - lead) * 0.04;
            camY += (clampCamY(targetCamY()) - camY) * (player.onGround ? 0.12 : 0.08);
        }
        double target = player.centerX() - W / 2.0 + lead;
        camX += (target - camX) * 0.18;
        camX = clampCamX(camX);
    }

    private double clampCamX(double v) {
        return Math.max(0, Math.min(level.pixelWidth() - W, v));
    }

    private double clampCamY(double v) {
        return Math.max(0, Math.min(level.pixelHeight() - H, v));
    }

    // ----------------------------------------------------------------- fin de niveau

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
        int next = Math.min(Levels.COUNT - 1, levelIndex + 1);
        if (next > unlocked) {
            unlocked = next;
            storage.putInt("niveau", unlocked);
        }
    }

    private void updateClear() {
        stateTimer++;
        level.goal.update();
        if (fadeOut > 0) {
            if (++fadeOut >= FADE) {
                levelIndex++;
                if (levelIndex >= Levels.COUNT) {
                    saveBest();
                    state = State.VICTORY;
                    stateTimer = 0;
                    Sound.play(Sound.ONE_UP);
                } else {
                    startLevel(true);
                }
            }
            return;
        }
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
            if (input.startPressed() || input.anyTap()) {
                score += timeBonus + ringBonus;
                timeBonus = 0;
                ringBonus = 0;
            }
            stateTimer = 90;
            return;
        }
        if (stateTimer > 200 || (stateTimer > 100 && (input.startPressed() || input.anyTap()))) {
            fadeOut = 1;
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
                int fade = Math.max(fadeIn, fadeOut);
                if (fade > 0) {
                    g.setColor(Col.argb(255 * fade / FADE, 0));
                    g.fillRect(0, 0, W, H);
                }
                break;
        }
    }

    // ----------------------------------------------------------------- rendu en relief (2,5D)

    /** Distance focale de la perspective. */
    private static final double FOCAL = 700;
    /** Demi-épaisseur du décor : le sol va de z = -DEPTH (au fond) à z = +DEPTH (devant). */
    private static final double DEPTH = 44;
    /** Hauteur du point de fuite à l'écran : plus il est haut, plus on voit le dessus du sol. */
    private static final double VPY = 120;

    /** Caméra de l'image en cours (avec le tremblement). */
    private double viewX;
    private double viewY;

    private static double scaleAt(double z) {
        return FOCAL / (FOCAL - z);
    }

    /** Projection d'un point du monde (x, y, profondeur z) sur l'écran. */
    private double sx(double x, double z) {
        return W / 2.0 + (x - viewX - W / 2.0) * scaleAt(z);
    }

    private double sy(double y, double z) {
        return VPY + (y - viewY - VPY) * scaleAt(z);
    }

    /**
     * Place le repère de dessin sur le plan de profondeur z : ensuite on dessine en coordonnées
     * du monde, comme en 2D (x moins la caméra, y du monde).
     */
    private void plane(Gfx g, double z) {
        double s = scaleAt(z);
        g.translate(W / 2.0 * (1 - s), VPY * (1 - s));
        g.scale(s, s);
        g.translate(0, -viewY);
    }

    /** Quadrilatère horizontal (dessus) entre x0 et x1, de la profondeur z0 à z1. */
    private void topQuad(Gfx g, double x0, double y0, double x1, double y1, double z0, double z1) {
        g.fillPolygon(new double[] {sx(x0, z0), sx(x1, z0), sx(x1, z1), sx(x0, z1)},
                new double[] {sy(y0, z0), sy(y1, z0), sy(y1, z1), sy(y0, z1)}, 4);
    }

    /** Quadrilatère vertical (côté) à l'abscisse x, de y0 à y1. */
    private void sideQuad(Gfx g, double x, double y0, double y1) {
        g.fillPolygon(new double[] {sx(x, -DEPTH), sx(x, DEPTH), sx(x, DEPTH), sx(x, -DEPTH)},
                new double[] {sy(y0, -DEPTH), sy(y0, DEPTH), sy(y1, DEPTH), sy(y1, -DEPTH)}, 4);
    }

    private void renderWorld(Gfx g) {
        double shx = 0;
        double shy = 0;
        if (shake > 0) {
            shx = random.nextInt(7) - 3;
            shy = random.nextInt(7) - 3;
        }
        viewX = camX - shx;
        viewY = Math.round(camY) - shy;
        double shift = (level.pixelHeight() - H - camY) * 0.25;
        Theme th = level.theme;
        renderBackground(g, th, viewX, shift);

        // 1. le fond : surface de l'eau, dessus et côtés du décor, décor lointain, ombres
        renderLiquidTop(g);
        renderTerrainFaces(g);
        for (Platform p : level.platforms) {
            g.setColor(th.topColor(0));
            topQuad(g, p.x, p.y, p.x + p.w, p.y, -DEPTH * 0.7, DEPTH);
        }
        for (Monitor m : level.monitors) {
            if (!m.broken && visible(m.x, Monitor.W)) {
                g.setColor(Col.rgb(0x868e96));
                topQuad(g, m.x, m.base - Monitor.H, m.x + Monitor.W, m.base - Monitor.H, -14, 0);
            }
        }
        g.save();
        plane(g, -DEPTH * 0.6);
        renderDecorations(g, true);
        g.restore();
        renderSpikes(g, true);
        for (Loop l : level.loops) {
            g.save();
            plane(g, -24);
            l.draw(g, th, viewX);
            g.restore();
        }
        renderShadows(g);

        // 2. le plan du jeu (z = 0), à l'échelle 1 : la physique et le dessin coïncident
        g.save();
        plane(g, 0);
        double cam = viewX;
        for (Checkpoint c : level.checkpoints) {
            c.draw(g, cam);
        }
        if (level.goal != null) {
            level.goal.draw(g, cam);
        }
        for (Spring s : level.springs) {
            if (visible(s.x, T)) {
                s.draw(g, cam);
            }
        }
        for (Monitor m : level.monitors) {
            if (visible(m.x, Monitor.W)) {
                m.draw(g, cam, tick);
            }
        }
        for (Ring r : level.rings) {
            if (!r.collected && visible(r.x - Ring.SIZE, Ring.SIZE * 2)) {
                r.draw(g, cam, tick);
            }
        }
        for (Ring r : lostRings) {
            r.draw(g, cam, tick);
        }
        for (Enemy e : level.enemies) {
            if (e.alive && visible(e.x, e.w)) {
                e.draw(g, cam);
            }
        }
        for (Animal a : animals) {
            a.draw(g, cam);
        }
        for (Bomb b : bombs) {
            b.draw(g, cam, tick);
        }
        for (Shot s : shots) {
            s.draw(g, cam, tick);
        }
        if (boss != null) {
            boss.draw(g, cam, tick);
        }
        player.drawTrail(g, cam);
        player.draw(g, cam, tick);
        for (Particle p : particles) {
            p.draw(g, cam);
        }
        g.restore();

        // 3. l'avant : bord des loopings, pics, façade du décor et fleurs
        for (Loop l : level.loops) {
            g.save();
            plane(g, 24);
            l.drawRim(g, th, viewX);
            g.restore();
        }
        renderSpikes(g, false);
        g.save();
        plane(g, DEPTH);
        renderLiquid(g, viewX);
        for (Platform p : level.platforms) {
            p.draw(g, th, viewX);
        }
        renderFrontTiles(g);
        g.restore();

        g.save();
        plane(g, 0);
        for (Popup p : popups) {
            p.draw(g, cam);
        }
        g.restore();
    }

    private boolean visible(double x, double w) {
        return x + w >= camX - 64 && x <= camX + W + 64;
    }

    /** Une tuile qui a un volume (sol plein ou pente). */
    private static boolean solid3d(byte t) {
        return t == Level.SOLID || Level.isSlope(t);
    }

    /** Dessus (sol, herbe) et côtés (falaises) de chaque tuile, vus en perspective. */
    private void renderTerrainFaces(Gfx g) {
        Theme th = level.theme;
        double eyeX = viewX + W / 2.0;
        double eyeY = viewY + VPY;
        int x0 = Math.max(0, (int) Math.floor((viewX - 80) / T));
        int x1 = Math.min(level.w - 1, (int) ((viewX + W + 80) / T));
        int y0 = Math.max(0, (int) Math.floor((viewY - 80) / T));
        int y1 = Math.min(level.h - 1, (int) ((viewY + H + 80) / T));
        for (int tx = x0; tx <= x1; tx++) {
            double left = tx * T;
            double right = left + T;
            for (int ty = y0; ty <= y1; ty++) {
                byte t = level.tile(tx, ty);
                byte above = level.tile(tx, ty - 1);
                if (t == Level.ONEWAY) {
                    if (above == Level.EMPTY && ty * T > eyeY) {
                        g.setColor(th.topColor(tx));
                        topQuad(g, left, ty * T, right, ty * T, -DEPTH * 0.7, DEPTH);
                    }
                    continue;
                }
                if (!solid3d(t)) {
                    continue;
                }
                double bottom = (ty + 1) * T;
                double yl = bottom - Level.height(t, 0);
                double yr = bottom - Level.height(t, T - 1);
                // côtés : seulement ceux tournés vers la caméra
                if (right < eyeX && !solid3d(level.tile(tx + 1, ty))) {
                    g.setColor(th.sideColor(tx + ty));
                    sideQuad(g, right, Math.max(yr, ty * T), bottom);
                }
                if (left > eyeX && !solid3d(level.tile(tx - 1, ty))) {
                    g.setColor(th.sideColor(tx + ty));
                    sideQuad(g, left, Math.max(yl, ty * T), bottom);
                }
                // dessus : visible seulement sous le niveau des yeux
                if (!solid3d(above) && Math.min(yl, yr) > eyeY) {
                    g.setColor(th.topColor(tx));
                    topQuad(g, left, yl, right, yr, -DEPTH, DEPTH);
                    g.setColor(Col.argb(60, 0xffffff));
                    topQuad(g, left, yl, right, yr, -DEPTH, -DEPTH * 0.55);
                }
            }
        }
    }

    /** Surface de l'eau (ou de la lave, du liquide toxique) qui s'étend vers le fond. */
    private void renderLiquidTop(Gfx g) {
        double top = level.pixelHeight() - 69;
        if (top <= viewY + VPY) {
            return;
        }
        g.setColor(Col.withAlpha(Col.brighter(level.theme.liquid()), 200));
        topQuad(g, viewX - 200, top, viewX + W + 200, top, -DEPTH * 4, DEPTH);
    }

    /** Pics plantés sur plusieurs rangs de profondeur. */
    private void renderSpikes(Gfx g, boolean back) {
        double[] depths = back ? new double[] {-30, -10} : new double[] {10, 30};
        int x0 = Math.max(0, (int) Math.floor((viewX - 64) / T));
        int x1 = Math.min(level.w - 1, (int) ((viewX + W + 64) / T));
        int y0 = Math.max(0, (int) Math.floor((viewY - 64) / T));
        int y1 = Math.min(level.h - 1, (int) ((viewY + H + 64) / T));
        for (double z : depths) {
            boolean any = false;
            for (int tx = x0; tx <= x1 && !any; tx++) {
                for (int ty = y0; ty <= y1; ty++) {
                    if (level.tile(tx, ty) == Level.SPIKE) {
                        any = true;
                        break;
                    }
                }
            }
            if (!any) {
                continue;
            }
            g.save();
            plane(g, z);
            for (int tx = x0; tx <= x1; tx++) {
                for (int ty = y0; ty <= y1; ty++) {
                    if (level.tile(tx, ty) == Level.SPIKE) {
                        g.drawSprite(level.theme.spikes(g), tx * T - viewX, ty * T);
                    }
                }
            }
            g.restore();
        }
    }

    /** Ombres portées sur le sol, sous Henry, les robots et le boss. */
    private void renderShadows(Gfx g) {
        if (!player.dead && !player.inLoop()) {
            shadow(g, player.centerX(), player.bottom(), 14);
        }
        for (Enemy e : level.enemies) {
            if (e.alive && visible(e.x, e.w)) {
                shadow(g, e.centerX(), e.y + e.h, 14);
            }
        }
        if (boss != null && !boss.defeated) {
            shadow(g, boss.centerX(), boss.y + Boss.H, 44);
        }
    }

    private void shadow(Gfx g, double x, double fromY, double r) {
        double gy = level.groundBelow(x, fromY, fromY, 2, 360);
        if (Double.isNaN(gy) || gy <= viewY + VPY) {
            return;
        }
        double k = Math.max(0, 1 - (gy - fromY) / 360);
        if (k <= 0) {
            return;
        }
        double rr = r * (0.55 + 0.45 * k);
        double top = sy(gy, -rr * 0.7);
        double bottom = sy(gy, rr * 0.7);
        double cx = sx(x, 0);
        g.setColor(Col.argb((int) (95 * k), 0));
        g.fillOval(cx - rr, top, 2 * rr, Math.max(2, bottom - top));
    }

    /** Façade du décor (plan de devant) : le damier, l'herbe, les plates-formes. */
    private void renderFrontTiles(Gfx g) {
        Theme th = level.theme;
        int camI = (int) Math.round(viewX);
        int x0 = Math.max(0, camI / T - 1);
        int x1 = Math.min(level.w - 1, (camI + W) / T + 1);
        int y0 = Math.max(0, (int) (viewY / T) - 1);
        int y1 = Math.min(level.h - 1, (int) ((viewY + H) / T) + 1);
        for (int ty = y0; ty <= y1; ty++) {
            for (int tx = x0; tx <= x1; tx++) {
                byte t = level.tile(tx, ty);
                int sx = tx * T - camI;
                int sy = ty * T;
                if (t == Level.SOLID) {
                    byte above = level.tile(tx, ty - 1);
                    boolean top = above == Level.EMPTY || above == Level.ONEWAY || above == Level.SPIKE;
                    g.drawSprite(top ? th.top(g) : th.fill(g), sx, sy);
                    if (top && above == Level.EMPTY && ty > 0) {
                        decorate(g, th, tx, sx, sy, false);
                    }
                } else if (t == Level.ONEWAY) {
                    g.drawSprite(th.platform(g), sx, sy);
                } else if (Level.isSlope(t)) {
                    g.drawSprite(th.slope(g, t), sx, sy);
                }
            }
        }
    }

    /** Décor posé au fond du sol (palmiers, cactus, lampadaires). */
    private void renderDecorations(Gfx g, boolean back) {
        Theme th = level.theme;
        int camI = (int) Math.round(viewX);
        int x0 = Math.max(0, camI / T - 3);
        int x1 = Math.min(level.w - 1, (camI + W) / T + 3);
        int y0 = Math.max(1, (int) (viewY / T) - 3);
        int y1 = Math.min(level.h - 1, (int) ((viewY + H) / T) + 3);
        for (int tx = x0; tx <= x1; tx++) {
            for (int ty = y0; ty <= y1; ty++) {
                if (level.tile(tx, ty) == Level.SOLID && level.tile(tx, ty - 1) == Level.EMPTY) {
                    decorate(g, th, tx, tx * T - camI, ty * T, back);
                }
            }
        }
    }

    /**
     * Décor posé sur le sol : au fond, les grands éléments (palmiers, cactus, lampadaires) ;
     * devant, les fleurs.
     */
    private void decorate(Gfx g, Theme th, int tx, int sx, int sy, boolean back) {
        int h = hash(tx);
        if (h % 6 != 0) {
            return;
        }
        int cx = sx + 8 + (h >> 4) % 16;
        if (th.style == Theme.MEADOW) {
            boolean palm = (h >> 9) % 3 == 0;
            if (palm && back) {
                drawPalm(g, cx, sy, h);
            } else if (!palm && !back) {
                g.setColor(Col.rgb(0x2b8a3e));
                g.fillRect(cx - 1, sy - 14, 2, 14);
                g.setColor((h >> 8) % 2 == 0 ? Col.rgb(0xffd43b) : Col.rgb(0xf783ac));
                int sway = (int) (Math.sin(tick * 0.05 + tx) * 1.5);
                for (int i = 0; i < 5; i++) {
                    double a = i * Math.PI * 2 / 5 + tick * 0.02;
                    g.fillOval(cx + sway + Math.cos(a) * 4 - 3, sy - 18 + Math.sin(a) * 4 - 3, 6, 6);
                }
                g.setColor(Col.rgb(0x7048e8));
                g.fillOval(cx + sway - 2, sy - 20, 5, 5);
            }
        } else if (!back) {
            return;
        } else if (th.style == Theme.CANYON) {
            g.setColor(Col.rgb(0x2f9e44));
            g.fillRoundRect(cx - 4, sy - 26, 8, 26, 8);
            g.fillRoundRect(cx - 12, sy - 18, 6, 10, 6);
            g.fillRect(cx - 9, sy - 11, 6, 3);
            g.fillRoundRect(cx + 6, sy - 22, 6, 10, 6);
            g.fillRect(cx + 3, sy - 15, 6, 3);
            g.setColor(Col.rgb(0x40c057));
            g.fillRect(cx - 2, sy - 24, 1, 20);
        } else {
            g.setColor(Col.rgb(0x495057));
            g.fillRect(cx - 2, sy - 40, 4, 40);
            g.fillRect(cx - 2, sy - 40, 12, 3);
            boolean on = (tick / 30 + tx) % 2 == 0;
            g.setColor(on ? Col.rgb(0xffe066) : Col.rgb(0xffa94d));
            g.fillOval(cx + 6, sy - 38, 6, 6);
            if (on) {
                g.setColor(Col.argb(40, 0xffe066));
                g.fillPolygon(new double[] {cx + 9, cx - 6, cx + 24}, new double[] {sy - 35, sy, sy}, 3);
            }
        }
    }

    /** Eau, lave ou liquide toxique tout au fond : visible dans les gouffres. */
    private void renderLiquid(Gfx g, double cam) {
        double top = level.pixelHeight() - 72;
        int color = level.theme.liquid();
        g.setColor(color);
        g.fillRect(0, top + 6, W, 80);
        int n = W / 16 + 2;
        double[] xs = new double[n + 2];
        double[] ys = new double[n + 2];
        double off = cam % 16;
        for (int i = 0; i < n; i++) {
            xs[i] = i * 16 - off;
            ys[i] = top + 3 + Math.sin((i * 16 + cam) * 0.05 + tick * 0.08) * 3;
        }
        xs[n] = xs[n - 1];
        ys[n] = top + 10;
        xs[n + 1] = xs[0];
        ys[n + 1] = top + 10;
        g.fillPolygon(xs, ys, n + 2);
        g.setColor(Col.argb(120, 0xffffff));
        for (int i = 0; i < 12; i++) {
            double x = wrap(i * 97 - cam * 0.9 + Math.sin(tick * 0.03 + i) * 20, W + 60) - 30;
            g.fillRect(x, top + 16 + (i % 3) * 14, 18 + (i % 4) * 6, 2);
        }
    }

    private void drawPalm(Gfx g, int cx, int sy, int h) {
        double sway = Math.sin(tick * 0.03 + h) * 2;
        g.setColor(Col.rgb(0x8d5a2b));
        for (int i = 0; i < 6; i++) {
            g.fillRoundRect(cx - 4 + i * 0.8 + sway * i / 6, sy - 12 - i * 10, 9, 11, 4);
        }
        g.setColor(Col.rgb(0x6b4220));
        for (int i = 0; i < 6; i++) {
            g.fillRect(cx - 3 + i * 0.8 + sway * i / 6, sy - 3 - i * 10, 7, 2);
        }
        double tx = cx + 5 + sway;
        double ty = sy - 66;
        g.setColor(Col.rgb(0x2f9e44));
        for (int i = 0; i < 6; i++) {
            double a = -Math.PI + i * Math.PI / 5 + Math.sin(tick * 0.04 + i) * 0.05;
            double ex = tx + Math.cos(a) * 30;
            double ey = ty + Math.sin(a) * 14 + 12;
            g.fillPolygon(new double[] {tx, (tx + ex) / 2 + 2, ex, (tx + ex) / 2 - 2},
                    new double[] {ty, ty - 8 + Math.sin(a) * 6, ey, ty + 6}, 4);
        }
        g.setColor(Col.rgb(0x8d5a2b));
        g.fillOval(tx - 6, ty - 2, 6, 6);
        g.fillOval(tx, ty, 6, 6);
    }

    private static int hash(int v) {
        int h = v * 0x45d9f3b;
        h ^= h >>> 16;
        h *= 0x45d9f3b;
        h ^= h >>> 16;
        return h & 0x7fffffff;
    }

    private void renderBackground(Gfx g, Theme th, double cam, double shift) {
        g.fillVerticalGradient(0, 0, W, H, th.skyTop, th.skyBottom);
        if (th.style == Theme.MEADOW) {
            g.setColor(Col.argb(90, 0xffffff));
            g.fillOval(600, 20 + shift * 0.2, 140, 140);
            g.setColor(Col.argb(200, 0xffffff));
            for (int i = 0; i < 8; i++) {
                double cx = wrap(i * 180 - cam * 0.1 - tick * 0.15, W + 200) - 100;
                double cy = 40 + (hash(i) % 90) + shift * 0.3;
                g.fillOval(cx, cy, 90, 26);
                g.fillOval(cx + 20, cy - 12, 50, 30);
            }
            drawMountains(g, cam, 0.08, 270 + shift * 0.4, Col.rgb(0x91a7ff), Col.rgb(0xe7f5ff));
            drawHills(g, cam, 0.2, 300 + shift * 0.6, 50, th.far, 0.006);
            drawHills(g, cam, 0.4, 350 + shift, 35, th.near, 0.011);
        } else if (th.style == Theme.CANYON) {
            g.setColor(Col.argb(80, 0xfff3bf));
            g.fillOval(530, 120 + shift * 0.3, 170, 170);
            g.setColor(Col.rgb(0xffe8a3));
            g.fillOval(560, 150 + shift * 0.3, 110, 110);
            drawMesas(g, cam, 0.15, 260 + shift * 0.5, th.far);
            drawMesas(g, cam, 0.35, 330 + shift, th.near);
        } else {
            g.setColor(Col.WHITE);
            for (int i = 0; i < 60; i++) {
                int sx = (int) wrap(hash(i) % 1600 - cam * 0.05, W);
                double sy = hash(i + 99) % 250 + shift * 0.2;
                double r = (i % 5 == 0) ? 2 : 1;
                if ((tick / 20 + i) % 9 != 0) {
                    g.fillRect(sx, sy, r, r);
                }
            }
            drawFactories(g, cam, 0.2, th.far, 230 + shift * 0.5);
            drawFactories(g, cam, 0.4, th.near, 300 + shift);
        }
    }

    private static double wrap(double v, double m) {
        double r = v % m;
        return r < 0 ? r + m : r;
    }

    private void drawMountains(Gfx g, double cam, double factor, double base, int color, int snow) {
        double off = cam * factor;
        int first = (int) Math.floor(off / 160) - 1;
        for (int i = first; i < first + W / 160 + 3; i++) {
            double x = i * 160 - off;
            double h = 90 + hash(i * 3) % 70;
            g.setColor(color);
            g.fillPolygon(new double[] {x - 40, x + 80, x + 200}, new double[] {base, base - h, base}, 3);
            g.setColor(snow);
            g.fillPolygon(new double[] {x + 80 - h * 0.25, x + 80, x + 80 + h * 0.25, x + 80, x + 70},
                    new double[] {base - h * 0.75, base - h, base - h * 0.75, base - h * 0.7, base - h * 0.78}, 5);
        }
    }

    private void drawHills(Gfx g, double cam, double factor, double base, int amp, int color, double freq) {
        int n = W / 8 + 1;
        double[] xs = new double[n + 2];
        double[] ys = new double[n + 2];
        xs[0] = 0;
        ys[0] = H;
        for (int i = 0; i < n; i++) {
            int x = i * 8;
            double wx = x + cam * factor;
            xs[i + 1] = x;
            ys[i + 1] = base - Math.abs(Math.sin(wx * freq)) * amp - Math.sin(wx * freq * 2.3) * amp * 0.3;
        }
        xs[n + 1] = W;
        ys[n + 1] = H;
        g.setColor(color);
        g.fillPolygon(xs, ys, n + 2);
    }

    private void drawMesas(Gfx g, double cam, double factor, double base, int color) {
        g.setColor(color);
        double off = cam * factor;
        int first = (int) Math.floor(off / 80) - 1;
        for (int i = first; i < first + W / 80 + 3; i++) {
            int h = hash(i + (int) (factor * 100)) % 120;
            double x = i * 80 - off;
            g.fillRect(x, base - h, 82, H);
            g.fillRect(x - 8, base - h, 98, 10);
        }
    }

    private void drawFactories(Gfx g, double cam, double factor, int color, double base) {
        double off = cam * factor;
        int first = (int) Math.floor(off / 100) - 1;
        for (int i = first; i < first + W / 100 + 3; i++) {
            int h = 40 + hash(i * 7 + (int) (factor * 10)) % 140;
            double x = i * 100 - off;
            g.setColor(color);
            g.fillRect(x, base - h, 90, H);
            if (hash(i) % 3 == 0) {
                g.fillRect(x + 60, base - h - 60, 14, 60);
                g.setColor(Col.rgba(120, 120, 140, 90));
                int puff = (tick / 2 + i * 13) % 60;
                g.fillOval(x + 54 + puff / 3.0, base - h - 75 - puff, 24 + puff / 3.0, 18 + puff / 4.0);
            }
            g.setColor(Col.rgb(0xffe066));
            for (int wy = (int) (base - h + 12); wy < base; wy += 22) {
                for (int wx = (int) x + 10; wx < x + 80; wx += 20) {
                    if (hash((wx - (int) x) * 31 + wy + i * 7) % 3 == 0) {
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
        hudLine(g, "TEMPS", time, 56, timeFrames > TIME_LIMIT - 60 * 60 && (tick / 15) % 2 == 0 ? Col.RED : YELLOW);
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

        // bonus actifs
        int px = touchMode ? 120 : 20;
        int py = 100;
        if (player.shield) {
            powerIcon(g, px, py, Monitor.SHIELD, 1);
            px += 34;
        }
        if (player.shoes > 0) {
            powerIcon(g, px, py, Monitor.SHOES, player.shoes / (double) Player.SHOES_TIME);
            px += 34;
        }
        if (player.stars > 0) {
            powerIcon(g, px, py, Monitor.STARS, player.stars / (double) Player.STARS_TIME);
        }
        if (!Sound.isEnabled() && !touchMode) {
            g.setFont(16, false);
            shadowText(g, "SON COUPÉ", W - 110, H - 10, Col.WHITE);
        }
    }

    private void powerIcon(Gfx g, int x, int y, int type, double left) {
        g.setColor(Col.argb(140, 0));
        g.fillRoundRect(x, y, 28, 28, 8);
        new Monitor(0, 0, type).drawIcon(g, x + 14, y + 13, tick);
        g.setColor(YELLOW);
        g.fillRect(x + 3, y + 24, 22 * left, 2);
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

    private static void centered(Gfx g, String s, float size, boolean italic, int c, double y) {
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
        g.setColor(Col.rgb(0xe03131));
        g.fillPolygon(new double[] {bandX + 200, bandX + 230, bandX + 170, bandX + 140},
                new double[] {0, 0, H, H}, 4);
        g.setColor(YELLOW);
        g.fillRect(0, (int) (H - 90 * k), W, 14);
        int tx = (int) (W / 2.0 + 60 + (1 - k) * W);
        g.setFont(32, true);
        float zw = g.textWidth(level.zone);
        shadowText(g, level.zone, tx - zw / 2, 210, Col.WHITE);
        g.setFont(20, true);
        String act = "ACTE " + level.act;
        shadowText(g, act, tx + zw / 2 - g.textWidth(act), 250, YELLOW);
    }

    private void renderClear(Gfx g) {
        int slide = Math.min(stateTimer, 30);
        int off = (30 - slide) * 25;
        g.setColor(Col.argb(110, 0));
        g.fillRoundRect(190, 105, 420, 235, 30);
        g.setFont(32, true);
        String title;
        if (levelIndex == Levels.COUNT - 1) {
            title = "HENRY A SAUVÉ LE MONDE !";
        } else if (level.bossArenaX >= 0) {
            title = "DR. VORTEX EST EN FUITE !";
        } else {
            title = "HENRY A RÉUSSI !";
        }
        shadowText(g, title, (W - g.textWidth(title)) / 2 - off, 150, Col.WHITE);
        g.setFont(20, true);
        String sub = level.zone + " - ACTE " + level.act;
        shadowText(g, sub, (W - g.textWidth(sub)) / 2 + off, 185, YELLOW);
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
        renderBackground(g, th, cam, 0);
        int ground = 400;
        // sol en relief : dessus rayé puis façade
        viewX = cam;
        viewY = 0;
        int first = (int) Math.floor((cam - 80) / T);
        for (int tx = first; tx * T < cam + W + 80; tx++) {
            g.setColor(th.topColor(tx));
            topQuad(g, tx * T, ground, (tx + 1) * T, ground, -DEPTH, DEPTH);
        }
        g.save();
        plane(g, DEPTH);
        int off = (int) (cam % T);
        for (int x = -off - T; x < W + T; x += T) {
            g.drawSprite(th.top(g), x, ground);
            g.drawSprite(th.fill(g), x, ground + T);
            g.drawSprite(th.fill(g), x, ground + 2 * T);
        }
        g.restore();
        titleHenry.x = 660 - Player.W / 2.0;
        titleHenry.y = ground - Player.H;
        g.save();
        g.translate(660, ground);
        g.scale(1.6, 1.6);
        g.translate(-660, -ground);
        titleHenry.draw(g, 0, tick);
        g.restore();

        // logo
        int bob = (int) (Math.sin(tick * 0.05) * 4);
        g.setColor(Col.argb(90, 0));
        g.fillRoundRect(178, 34 + bob + 8, 460, 150, 60);
        g.setColor(Col.rgb(0x1c2f8a));
        g.fillRoundRect(170, 34 + bob, 460, 150, 60);
        g.setColor(Col.rgb(0x364fc7));
        g.fillRoundRect(176, 40 + bob, 448, 70, 50);
        g.setColor(YELLOW);
        g.setStroke(5f);
        g.drawRoundRect(170, 34 + bob, 460, 150, 60);
        g.setStroke(1f);
        for (int i = 0; i < 3; i++) {
            double a = tick * 0.04 + i * 2.1;
            g.setColor(Col.argb(200, 0xffffff));
            Monitor.star(g, 400 + Math.cos(a) * 215, 109 + bob + Math.sin(a) * 70, 6, 2.5);
        }
        centered(g, "HENRY", 84, true, Col.WHITE, 119 + bob);
        centered(g, "LE HÉRISSON", 32, true, YELLOW, 164 + bob);

        // menu
        String[] items = {
            "JOUER",
            unlocked > 0 ? "◀  " + Levels.zoneName(menuLevel).replace("ZONE ", "") + " " + (menuLevel % 2 + 1) + "  ▶"
                    : "ZONES : finis un acte pour les débloquer",
            "SON : " + (Sound.isEnabled() ? "OUI" : "NON"),
        };
        double mx = (W - MENU_W) / 2.0;
        for (int i = 0; i < items.length; i++) {
            double my = MENU_Y + i * MENU_GAP;
            boolean sel = i == menuIndex;
            g.setColor(sel ? Col.argb(170, 0x1c2f8a) : Col.argb(110, 0));
            g.fillRoundRect(mx, my - 26, MENU_W, 34, 16);
            if (sel) {
                g.setColor(YELLOW);
                g.setStroke(2f);
                g.drawRoundRect(mx, my - 26, MENU_W, 34, 16);
                g.setStroke(1f);
            }
            float size = i == 1 ? 17 : 22;
            int color = sel ? YELLOW : Col.WHITE;
            if (i == 1 && unlocked == 0) {
                size = 14;
                color = Col.LIGHT_GRAY;
            }
            centered(g, items[i], size, true, color, my - (22 - size) / 2);
        }
        g.setColor(Col.argb(130, 0));
        g.fillRect(0, H - 26, W, 26);
        centered(g, touchMode
                ? "Touche un choix  •  ◀ ▶ courir   A sauter   ▼ rouler   ▼ + A spin dash"
                : "↑↓ choisir  Entrée valider  •  ←→ courir  Espace sauter  ↓ rouler  ↓+Espace spin dash",
                13, false, Col.WHITE, H - 8);
        if (bestScore > 0) {
            centered(g, "MEILLEUR SCORE : " + bestScore, 15, false, YELLOW, 22);
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
        renderBackground(g, Theme.PRAIRIE, tick, 0);
        for (int i = 0; i < 12; i++) {
            double x = wrap(i * 70 + tick * (1 + i % 3), W + 40) - 20;
            double y = 360 - Math.abs(Math.sin(tick * 0.08 + i)) * 50;
            new Animal(x, y, i).draw(g, 0);
        }
        centered(g, "BRAVO HENRY !", 64, true, YELLOW, 130);
        centered(g, "Le Dr. Vortex est vaincu et les animaux sont libres !", 20, true, Col.WHITE, 185);
        centered(g, "Score final : " + score, 32, true, Col.WHITE, 245);
        if (score >= bestScore && score > 0) {
            centered(g, "NOUVEAU RECORD !", 20, true, YELLOW, 280);
        }
        if (stateTimer > 60) {
            centered(g, touchMode ? "Touche l'écran pour revenir au titre" : "Entrée pour revenir au titre",
                    16, false, Col.WHITE, 330);
        }
    }
}
