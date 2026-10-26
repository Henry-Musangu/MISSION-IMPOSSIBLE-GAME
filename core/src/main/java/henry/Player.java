package henry;

/**
 * Henry le Hérisson : course avec élan sur les pentes, saut en boule, roulade,
 * « spin dash », loopings et bonus (bouclier, chaussures de vitesse, invincibilité).
 *
 * Au sol, la vitesse est une vitesse le long du sol ({@link #gsp}) : la pente
 * la freine en montée et l'accélère en descente, surtout en boule.
 */
final class Player {

    static final int W = 22;
    static final int H = 32;

    private static final double ACC = 0.10;
    private static final double DEC = 0.5;
    private static final double FRICTION = 0.09;
    static final double TOP = 6.5;
    private static final double AIR_ACC = 0.2;
    private static final double GRAVITY = 0.36;
    private static final double JUMP = 8.6;
    private static final double JUMP_CUT = -4.0;
    private static final double ROLL_FRICTION = 0.035;
    private static final double ROLL_BRAKE = 0.14;
    private static final double SLOPE = 0.13;
    private static final double ROLL_UPHILL = 0.08;
    private static final double ROLL_DOWNHILL = 0.32;
    private static final double MAX_SPEED = 16;
    private static final double MAX_FALL = 14;
    static final int SHOES_TIME = 15 * 60;
    static final int STARS_TIME = 20 * 60;
    private static final int TRAIL = 6;

    static final int BODY = Col.rgb(0x7b3fe4);
    static final int BODY_DARK = Col.rgb(0x4c1fa8);
    static final int SKIN = Col.rgb(0xffd8b0);
    static final int SHOE = Col.rgb(0x2fb344);
    static final int SCARF = Col.rgb(0xe03131);

    double x;
    double y;
    /** Vitesse le long du sol (au sol uniquement). */
    double gsp;
    double vx;
    double vy;
    /** Angle du sol sous Henry (radians, positif quand ça monte vers la droite). */
    double angle;
    int facing = 1;
    boolean onGround;
    boolean jumping;
    boolean rolling;
    boolean crouching;
    boolean spinDash;
    boolean lookingUp;
    boolean hurt;
    boolean dead;
    boolean springing;
    boolean skidding;
    boolean teetering;
    double charge;
    int invuln;
    int deadTimer;
    int rings;
    int combo;
    double anim;
    boolean touchedSpikes;
    boolean shield;
    int shoes;
    int stars;
    /** Vient d'atterrir cette image (pour la poussière). */
    boolean landed;

    private Platform platform;
    private Loop loop;
    private Loop loopCooldown;
    private double loopA;
    private final double[] trailX = new double[TRAIL];
    private final double[] trailY = new double[TRAIL];
    private int trailPos;
    private int trailCount;

    Player(double x, double y) {
        this.x = x;
        this.y = y;
        onGround = true;
    }

    boolean ball() {
        return rolling || jumping || spinDash;
    }

    boolean attacking() {
        return (ball() || stars > 0) && !hurt && !dead;
    }

    boolean inLoop() {
        return loop != null;
    }

    Box bounds() {
        return new Box(x, y, W, H);
    }

    double centerX() {
        return x + W / 2.0;
    }

    double bottom() {
        return y + H;
    }

    /** Vitesse actuelle, au sol ou en l'air. */
    double speed() {
        return onGround || loop != null ? Math.abs(gsp) : Math.hypot(vx, vy);
    }

    private double topSpeed() {
        return shoes > 0 ? TOP * 1.6 : TOP;
    }

    void update(Input in, Level level) {
        landed = false;
        if (dead) {
            deadTimer++;
            vy = Math.min(vy + GRAVITY, MAX_FALL);
            y += vy;
            return;
        }
        if (invuln > 0) {
            invuln--;
        }
        if (shoes > 0) {
            shoes--;
        }
        if (stars > 0) {
            stars--;
        }
        touchedSpikes = false;
        skidding = false;
        teetering = false;
        lookingUp = false;
        if (loopCooldown != null && Math.abs(centerX() - loopCooldown.cx) > Loop.R + 24) {
            loopCooldown = null;
        }
        if (loop != null) {
            updateLoop(in);
        } else if (onGround) {
            ground(in, level);
        } else {
            air(in, level);
        }
        if (onGround && !ball()) {
            anim += Math.abs(gsp);
        } else if (ball()) {
            anim += 6 + speed() * 2;
        }
        trailX[trailPos] = x;
        trailY[trailPos] = y;
        trailPos = (trailPos + 1) % TRAIL;
        trailCount = speed() >= 8.5 || (stars > 0 && speed() > 3) ? Math.min(TRAIL, trailCount + 1) : 0;
    }

    // ---------------------------------------------------------------- au sol

    private void ground(Input in, Level level) {
        boolean left = in.left();
        boolean right = in.right();
        double top = topSpeed();
        double acc = shoes > 0 ? ACC * 2 : ACC;
        if (platform != null) {
            x += platform.dx;
        }

        if (spinDash) {
            charge *= 0.97;
            if (in.jumpPressed()) {
                charge = Math.min(charge + 2, 8);
                Sound.play(Sound.REV);
            }
            if (!in.down()) {
                spinDash = false;
                rolling = true;
                gsp = facing * (8 + charge);
                charge = 0;
                Sound.play(Sound.DASH);
            }
        } else if (rolling) {
            if ((left && gsp > 0) || (right && gsp < 0)) {
                gsp = approachZero(gsp, ROLL_BRAKE);
            }
            gsp = approachZero(gsp, ROLL_FRICTION);
            if (Math.abs(gsp) < 0.5 && Math.abs(angle) < 0.2) {
                rolling = false;
                gsp = 0;
            }
        } else {
            crouching = false;
            if (left && !right) {
                if (gsp > 0) {
                    skidding = gsp > 4;
                    gsp -= DEC;
                    if (gsp <= 0) {
                        gsp = -0.5;
                    }
                } else if (gsp > -top) {
                    gsp = Math.max(gsp - acc, -top);
                }
            } else if (right && !left) {
                if (gsp < 0) {
                    skidding = gsp < -4;
                    gsp += DEC;
                    if (gsp >= 0) {
                        gsp = 0.5;
                    }
                } else if (gsp < top) {
                    gsp = Math.min(gsp + acc, top);
                }
            } else {
                gsp = approachZero(gsp, shoes > 0 ? FRICTION * 2 : FRICTION);
            }
            if (gsp < 0) {
                facing = -1;
            } else if (gsp > 0) {
                facing = 1;
            } else if (left) {
                facing = -1;
            } else if (right) {
                facing = 1;
            }
            if (in.down() && !left && !right) {
                if (Math.abs(gsp) < 1) {
                    crouching = true;
                    gsp = 0;
                } else {
                    rolling = true;
                    Sound.play(Sound.ROLL);
                }
            } else if (in.up() && gsp == 0) {
                lookingUp = true;
            }
        }

        // la pente freine ou accélère
        double sin = Math.sin(angle);
        if (rolling) {
            boolean uphill = gsp * sin > 0;
            gsp -= (uphill ? ROLL_UPHILL : ROLL_DOWNHILL) * sin;
        } else if (!spinDash && !crouching && (Math.abs(gsp) > 0.05 || Math.abs(angle) > 0.4)) {
            gsp -= SLOPE * sin;
        }
        gsp = Math.max(-MAX_SPEED, Math.min(MAX_SPEED, gsp));

        if (in.jumpPressed() && !spinDash) {
            if (crouching) {
                crouching = false;
                spinDash = true;
                charge = 2;
                Sound.play(Sound.REV);
            } else {
                // saut perpendiculaire au sol (poussée arrière adoucie sur les pentes raides)
                double cos = Math.cos(angle);
                vx = gsp * cos - JUMP * sin * 0.5;
                vy = -gsp * sin - JUMP * cos;
                jumping = true;
                rolling = false;
                onGround = false;
                platform = null;
                angle = 0;
                Sound.play(Sound.JUMP);
                air(in, level);
                return;
            }
        }

        vx = gsp * Math.cos(angle);
        double prevCenter = centerX();
        x += vx;
        if (hitWall(level, 12)) {
            gsp = 0;
            vx = 0;
        }
        snapToGround(level);
        if (onGround) {
            checkLoops(level, prevCenter);
        }
    }

    /** Colle Henry au sol (pentes comprises) ; s'il n'y a plus de sol, il décolle. */
    private void snapToGround(Level level) {
        double foot = y + H;
        double maxDown = 16 + Math.abs(vx);
        double sl = level.groundBelow(x + 3, foot, foot, 16, maxDown);
        Platform pl = level.lastPlatform;
        double sr = level.groundBelow(x + W - 3, foot, foot, 16, maxDown);
        Platform pr = level.lastPlatform;
        if (Double.isNaN(sl) && Double.isNaN(sr)) {
            onGround = false;
            platform = null;
            vx = gsp * Math.cos(angle);
            vy = -gsp * Math.sin(angle);
            angle = 0;
            return;
        }
        double s;
        if (Double.isNaN(sr) || (!Double.isNaN(sl) && sl <= sr)) {
            s = sl;
            platform = pl;
        } else {
            s = sr;
            platform = pr;
        }
        y = s - H;
        angle = groundAngle(level, sl, sr, s);
        teetering = gsp == 0 && angle == 0 && (Double.isNaN(sl) || Double.isNaN(sr))
                && !crouching && !spinDash;
        if (level.spikesAt(centerX(), s)) {
            touchedSpikes = true;
        }
    }

    private double groundAngle(Level level, double sl, double sr, double s) {
        if (!Double.isNaN(sl) && !Double.isNaN(sr) && Math.abs(sl - sr) <= W) {
            double a = Math.atan2(sl - sr, W - 6);
            return Math.max(-0.9, Math.min(0.9, a));
        }
        return level.slopeAngle(Double.isNaN(sl) ? x + W - 3 : x + 3, s);
    }

    private void checkLoops(Level level, double prevCenter) {
        double c = centerX();
        for (Loop l : level.loops) {
            if (l != loopCooldown && gsp >= Loop.MIN_SPEED && prevCenter < l.cx && c >= l.cx
                    && Math.abs(y + H - l.groundY) < 6) {
                loop = l;
                loopA = (c - l.cx) / Loop.R;
                platform = null;
                positionOnLoop();
                return;
            }
        }
    }

    // ---------------------------------------------------------------- looping

    private void updateLoop(Input in) {
        gsp -= 0.16 * Math.sin(loopA);
        if (in.right()) {
            gsp += 0.04;
        }
        gsp = Math.min(gsp, MAX_SPEED);
        boolean upper = loopA > Math.PI * 0.5 && loopA < Math.PI * 1.5;
        if (gsp < 0.5 || (upper && gsp < 3.2)) {
            // trop lent : Henry décroche
            vx = gsp * Math.cos(loopA);
            vy = -gsp * Math.sin(loopA);
            loopCooldown = loop;
            loop = null;
            onGround = false;
            angle = 0;
            return;
        }
        loopA += gsp / Loop.R;
        if (loopA >= Math.PI * 2) {
            Loop l = loop;
            loopCooldown = l;
            loop = null;
            onGround = true;
            angle = 0;
            x = l.cx + (loopA - Math.PI * 2) * Loop.R - W / 2.0;
            y = l.groundY - H;
            vx = gsp;
            return;
        }
        positionOnLoop();
    }

    private void positionOnLoop() {
        double fx = loop.px(loopA);
        double fy = loop.py(loopA);
        double cx = fx - Math.sin(loopA) * H / 2.0;
        double cy = fy - Math.cos(loopA) * H / 2.0;
        x = cx - W / 2.0;
        y = cy - H / 2.0;
        vx = gsp * Math.cos(loopA);
        vy = -gsp * Math.sin(loopA);
        onGround = true;
        facing = 1;
    }

    // ---------------------------------------------------------------- en l'air

    private void air(Input in, Level level) {
        crouching = false;
        double top = topSpeed();
        if (!hurt) {
            if (in.left() && !in.right()) {
                if (vx > -top) {
                    vx = Math.max(vx - AIR_ACC, -top);
                }
                facing = -1;
            } else if (in.right() && !in.left()) {
                if (vx < top) {
                    vx = Math.min(vx + AIR_ACC, top);
                }
                facing = 1;
            }
            if (vy < 0 && vy > -4 && Math.abs(vx) > 0.125) {
                vx *= 0.97;
            }
            if (jumping && !in.jumpHeld() && vy < JUMP_CUT) {
                vy = JUMP_CUT;
            }
        }
        vy = Math.min(vy + GRAVITY, MAX_FALL);
        vx = Math.max(-MAX_SPEED, Math.min(MAX_SPEED, vx));
        x += vx;
        hitWall(level, 6);
        double prevFoot = y + H;
        y += vy;
        double foot = y + H;
        // une pente peut monter de |vx| pixels pendant que Henry avance : on élargit la recherche du sol
        double up = Math.max(0, foot - prevFoot) + 8 + Math.abs(vx);
        if (vy < 0) {
            int row = Level.toTile(y);
            for (int c = Level.toTile(x + 2); c <= Level.toTile(x + W - 2); c++) {
                if (level.isCeiling(c, row)) {
                    y = (row + 1) * Game.T;
                    vy = 0;
                    break;
                }
            }
            // en montant contre une pente, Henry glisse dessus au lieu de s'y enfoncer
            double sl = level.groundBelow(x + 3, foot, foot, up, 0);
            double sr = level.groundBelow(x + W - 3, foot, foot, up, 0);
            double s = Double.isNaN(sl) ? sr : (Double.isNaN(sr) ? sl : Math.min(sl, sr));
            if (!Double.isNaN(s) && s < foot) {
                y = s - H;
            }
            return;
        }
        double sl = level.groundBelow(x + 3, foot, prevFoot, up, 1);
        Platform pl = level.lastPlatform;
        double sr = level.groundBelow(x + W - 3, foot, prevFoot, up, 1);
        Platform pr = level.lastPlatform;
        if (Double.isNaN(sl) && Double.isNaN(sr)) {
            return;
        }
        double s;
        if (Double.isNaN(sr) || (!Double.isNaN(sl) && sl <= sr)) {
            s = sl;
            platform = pl;
        } else {
            s = sr;
            platform = pr;
        }
        y = s - H;
        angle = groundAngle(level, sl, sr, s);
        gsp = vx * Math.cos(angle) - vy * Math.sin(angle);
        vy = 0;
        onGround = true;
        landed = true;
        land(in);
        if (level.spikesAt(centerX(), s)) {
            touchedSpikes = true;
        }
    }

    private void land(Input in) {
        springing = false;
        combo = 0;
        if (hurt) {
            hurt = false;
            gsp = 0;
        }
        if (jumping) {
            jumping = false;
            rolling = in.down() && Math.abs(gsp) > 1;
        }
    }

    /** Bloque contre les murs ; ignore le bas du corps pour laisser monter les petites marches et les pentes. */
    private boolean hitWall(Level level, double ignoreBottom) {
        int top = Level.toTile(y + 1);
        int bot = Level.toTile(y + H - ignoreBottom);
        if (vx > 0) {
            int col = Level.toTile(x + W - 0.01);
            for (int r = top; r <= bot; r++) {
                if (level.isWall(col, r)) {
                    x = col * Game.T - W;
                    vx = 0;
                    return true;
                }
            }
        } else if (vx < 0) {
            int col = Level.toTile(x);
            for (int r = top; r <= bot; r++) {
                if (level.isWall(col, r)) {
                    x = (col + 1) * Game.T;
                    vx = 0;
                    return true;
                }
            }
        }
        return false;
    }

    private static double approachZero(double v, double amount) {
        if (v > 0) {
            return Math.max(0, v - amount);
        }
        return Math.min(0, v + amount);
    }

    // ---------------------------------------------------------------- événements

    /** Rebond après avoir détruit un ennemi en l'air. */
    void bounce(boolean jumpHeld) {
        if (onGround) {
            return;
        }
        if (vy > 0) {
            vy = jumpHeld ? -Math.max(vy, 7.5) : -Math.max(vy, 4.5);
        } else {
            vy -= 1;
        }
    }

    /** Repoussé en l'air (contre le boss). */
    void bounceOff(double dirX) {
        onGround = false;
        platform = null;
        angle = 0;
        vx = dirX * 4;
        vy = -5;
    }

    void launch(double power) {
        loop = null;
        vy = power;
        vx = onGround ? gsp * Math.cos(angle) : vx;
        onGround = false;
        platform = null;
        angle = 0;
        jumping = false;
        rolling = false;
        spinDash = false;
        crouching = false;
        springing = true;
    }

    void knockBack(double fromX) {
        loop = null;
        hurt = true;
        jumping = false;
        rolling = false;
        spinDash = false;
        crouching = false;
        springing = false;
        onGround = false;
        platform = null;
        angle = 0;
        vy = -4.5;
        vx = centerX() < fromX ? -2.5 : 2.5;
        invuln = 120;
    }

    /** Repousse Henry hors d'un objet solide (moniteur) sans le casser. */
    void pushOutX(double newX) {
        x = newX;
        gsp = 0;
        vx = 0;
    }

    void die() {
        if (dead) {
            return;
        }
        loop = null;
        dead = true;
        hurt = false;
        rolling = false;
        jumping = false;
        spinDash = false;
        shield = false;
        stars = 0;
        shoes = 0;
        onGround = false;
        angle = 0;
        vx = 0;
        vy = -8;
        deadTimer = 0;
        Sound.play(Sound.DIE);
    }

    // ------------------------------------------------------------------ dessin

    /** Traînée de vitesse : des silhouettes transparentes derrière Henry. */
    void drawTrail(Gfx g, double camX) {
        for (int i = 1; i < trailCount; i++) {
            int idx = ((trailPos - 1 - i * 1) % TRAIL + TRAIL) % TRAIL;
            int alpha = 90 - i * 15;
            double cx = trailX[idx] + W / 2.0 - camX;
            double cy = trailY[idx] + H / 2.0;
            if (stars > 0) {
                g.setColor(Col.argb(alpha, (i % 2 == 0) ? 0xffe066 : 0xffffff));
            } else {
                g.setColor(Col.withAlpha(BODY, alpha));
            }
            if (ball()) {
                g.fillOval(cx - 15, cy - 13, 30, 30);
            } else {
                g.fillOval(cx - 11, cy - 18, 22, 36);
            }
        }
    }

    void draw(Gfx g, double camX, int tick) {
        if (invuln > 0 && !dead && (invuln / 3) % 2 == 0) {
            return;
        }
        double cx = x + W / 2.0 - camX;
        double cy = y + H / 2.0;
        g.save();
        g.translate(cx, cy);
        double rot = loop != null ? -loopA : (onGround && !dead ? -angle : 0);
        g.rotate(rot);
        g.translate(0, H / 2.0);
        if (ball() && !dead) {
            drawBall(g);
        } else {
            if (facing < 0) {
                g.scale(-1, 1);
            }
            if (crouching) {
                g.scale(1, 0.72);
            }
            drawBody(g, tick);
        }
        g.restore();

        if (shield && !dead) {
            double r = 24 + Math.sin(tick * 0.2) * 1.5;
            g.setColor(Col.argb(70, 0x74c0fc));
            g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
            g.setStroke(2f);
            g.setColor(Col.argb(160, 0xa5d8ff));
            g.drawOval(cx - r, cy - r, 2 * r, 2 * r);
            g.setColor(Col.argb(200, 0xffffff));
            g.drawArc(cx - r + 5, cy - r + 5, 2 * r - 10, 2 * r - 10, 100 + tick * 4 % 360, 50);
            g.setStroke(1f);
        }
        if (stars > 0 && !dead) {
            for (int i = 0; i < 4; i++) {
                double a = tick * 0.15 + i * Math.PI / 2;
                double r = 20 + Math.sin(tick * 0.3 + i) * 4;
                g.setColor((tick / 3 + i) % 2 == 0 ? Col.rgb(0xffe066) : Col.WHITE);
                Monitor.star(g, cx + Math.cos(a) * r, cy + Math.sin(a) * r, 5, 2);
            }
        }
    }

    private void drawBall(Gfx g) {
        g.translate(0, -15);
        if (spinDash) {
            g.setColor(Col.argb(120, 0xffffff));
            for (int i = 1; i <= 3; i++) {
                g.fillOval(-facing * i * 7 - 6, 6 - i * 3, 12, 6);
            }
        }
        // boule en relief : ombre en bas à droite, reflet en haut à gauche
        g.setColor(BODY_DARK);
        g.fillOval(-15, -15, 30, 30);
        g.setColor(BODY);
        g.fillOval(-15, -15, 27, 27);
        g.setColor(Col.argb(80, 0xffffff));
        g.fillOval(-11, -12, 12, 8);
        g.save();
        g.rotate(Math.toRadians(anim) * facing);
        g.setColor(BODY_DARK);
        g.setStroke(3f);
        for (int i = 0; i < 3; i++) {
            g.rotate(Math.PI * 2 / 3);
            g.drawArc(-11, -11, 22, 22, 0, 70);
        }
        g.setColor(SKIN);
        g.fillOval(2, -6, 9, 9);
        g.setColor(SCARF);
        g.fillOval(-10, 4, 8, 6);
        g.setStroke(1f);
        g.restore();
    }

    private void drawBody(Gfx g, int tick) {
        double speed = onGround ? Math.abs(gsp) : Math.abs(vx);
        boolean airborne = !onGround && !dead;
        double lean = skidding ? -0.3 : (onGround ? Math.min(speed * 0.025, 0.2) : 0);
        g.save();
        g.translate(0, -16);
        g.rotate(lean);
        g.translate(0, 16);

        // jambes
        double phase = anim * 0.12;
        if (onGround && speed >= 9) {
            // à pleine vitesse, les jambes tournent en roue floue
            g.setColor(Col.withAlpha(SHOE, 160));
            g.fillOval(-11, -16, 22, 18);
            g.setColor(Col.argb(120, 0xffffff));
            g.setStroke(1f);
            g.drawOval(-9, -14, 18, 14);
        } else {
            double swing = onGround ? Math.min(speed / TOP, 1) * 0.9 : 0.35;
            double a1 = Math.sin(phase) * swing;
            double a2 = -a1;
            if (springing || hurt || dead) {
                a1 = 0.3;
                a2 = -0.3;
            }
            drawLeg(g, a2, true);
            drawLeg(g, a1, false);
        }

        // écharpe qui flotte au vent
        g.setColor(SCARF);
        double wave = Math.sin(tick * 0.4) * 3;
        int tail = (int) (6 + Math.min(speed, 10) * 1.4);
        g.fillPolygon(new double[] {-3, -3 - tail, -3 - tail + 2, 0},
                new double[] {-27, -29 + wave, -23 + wave, -24}, 4);

        // piquants
        double[] qx = {0, -21, -9, -24, -9, -19, -4};
        double[] qy = {-40, -42, -33, -29, -24, -15, -19};
        g.setColor(BODY);
        g.fillPolygon(qx, qy, qx.length);
        g.setColor(BODY_DARK);
        g.setStroke(1f);
        g.drawPolygon(qx, qy, qx.length);

        // corps (en relief : ombre puis couleur puis reflet)
        g.setColor(BODY_DARK);
        g.fillOval(-8, -27, 16, 17);
        g.setColor(BODY);
        g.fillOval(-8, -27, 14, 15);
        g.setColor(SKIN);
        g.fillOval(-2, -24, 9, 12);

        // bras
        double arm = airborne ? -1.0 : (onGround ? -Math.sin(phase) * Math.min(speed / TOP, 1) : 0);
        if (hurt || dead) {
            arm = -2.2;
        } else if (teetering) {
            arm = -1.6 + Math.sin(tick * 0.35) * 1.2;
        } else if (skidding) {
            arm = 0.9;
        }
        int hx = (int) (2 + Math.cos(arm + 1.2) * 9);
        int hy = (int) (-22 + Math.sin(arm + 1.2) * 9);
        g.setColor(SKIN);
        g.setStroke(3f);
        g.drawLine(1, -22, hx, hy);
        g.setStroke(1f);
        g.setColor(Col.WHITE);
        g.fillOval(hx - 3, hy - 3, 7, 7);
        g.setColor(Col.GRAY);
        g.drawOval(hx - 3, hy - 3, 7, 7);

        // écharpe autour du cou
        g.setColor(SCARF);
        g.fillRoundRect(-7, -29, 14, 5, 4);

        // tête
        int lookY = lookingUp ? -3 : 0;
        g.setColor(BODY_DARK);
        g.fillOval(-9, -45 + lookY, 22, 21);
        g.setColor(BODY);
        g.fillOval(-10, -46 + lookY, 21, 20);
        g.setColor(Col.argb(70, 0xffffff));
        g.fillOval(-7, -44 + lookY, 9, 6);
        g.setColor(BODY);
        g.fillPolygon(new double[] {-4, 0, 4}, new double[] {-42 + lookY, -51 + lookY, -43 + lookY}, 3);
        g.setColor(SKIN);
        g.fillOval(-1, -42 + lookY, 4, 5);
        g.fillOval(3, -35 + lookY, 12, 9);
        // yeux
        g.setColor(Col.WHITE);
        g.fillOval(1, -43 + lookY, 9, 11);
        g.setColor(Col.BLACK);
        if (dead) {
            g.drawLine(3, -40, 8, -35);
            g.drawLine(8, -40, 3, -35);
        } else {
            g.fillOval(5, -40 + lookY - (lookingUp ? 2 : 0), 4, 6);
            g.setColor(Col.WHITE);
            g.fillRect(6, -39 + lookY, 1, 1);
        }
        // nez et sourire
        g.setColor(Col.BLACK);
        g.fillOval(13, -35 + lookY, 4, 4);
        g.drawArc(6, -34 + lookY, 7, 5, 200, 120);

        g.restore();
    }

    private void drawLeg(Gfx g, double angle, boolean back) {
        int hipY = -11;
        int fx = (int) (Math.sin(angle) * 10);
        int fy = (int) (hipY + Math.cos(angle) * 10);
        g.setColor(back ? BODY_DARK : BODY);
        g.setStroke(3.5f);
        g.drawLine(0, hipY, fx, fy);
        g.setStroke(1f);
        g.setColor(back ? Col.darker(SHOE) : SHOE);
        g.fillRoundRect(fx - 5, fy - 4, 14, 7, 6);
        g.setColor(Col.WHITE);
        g.fillRect(fx, fy - 4, 3, 7);
    }
}
