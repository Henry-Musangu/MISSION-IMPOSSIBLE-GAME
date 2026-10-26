package henry;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

/**
 * Henry le Hérisson : course avec élan, saut en boule, roulade et « spin dash ».
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
    private static final double JUMP = -8.6;
    private static final double JUMP_CUT = -4.0;
    private static final double ROLL_FRICTION = 0.035;
    private static final double ROLL_BRAKE = 0.14;
    private static final double MAX_SPEED = 16;
    private static final double MAX_FALL = 14;

    static final Color BODY = new Color(0x7b3fe4);
    static final Color BODY_DARK = new Color(0x4c1fa8);
    static final Color SKIN = new Color(0xffd8b0);
    static final Color SHOE = new Color(0x2fb344);
    static final Color SCARF = new Color(0xe03131);

    double x;
    double y;
    double vx;
    double vy;
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
    double charge;
    int invuln;
    int deadTimer;
    int rings;
    int combo;
    double anim;
    boolean touchedSpikes;

    Player(double x, double y) {
        this.x = x;
        this.y = y;
    }

    boolean ball() {
        return rolling || jumping || spinDash;
    }

    boolean attacking() {
        return ball() && !hurt && !dead;
    }

    Rectangle2D bounds() {
        return new Rectangle2D.Double(x, y, W, H);
    }

    double centerX() {
        return x + W / 2.0;
    }

    double bottom() {
        return y + H;
    }

    void update(Input in, Level level) {
        if (dead) {
            deadTimer++;
            vy = Math.min(vy + GRAVITY, MAX_FALL);
            y += vy;
            return;
        }
        if (invuln > 0) {
            invuln--;
        }
        touchedSpikes = false;
        if (hurt) {
            // pas de contrôle pendant le recul
        } else if (onGround) {
            groundControl(in);
        } else {
            airControl(in);
        }
        vy = Math.min(vy + GRAVITY, MAX_FALL);
        vx = Math.max(-MAX_SPEED, Math.min(MAX_SPEED, vx));
        moveX(level);
        moveY(level, in);
        if (onGround && !ball()) {
            anim += Math.abs(vx);
        } else if (ball()) {
            anim += 6 + Math.abs(vx) * 2;
        }
    }

    private void groundControl(Input in) {
        boolean left = in.left();
        boolean right = in.right();
        lookingUp = false;

        if (spinDash) {
            charge *= 0.97;
            if (in.jumpPressed()) {
                charge = Math.min(charge + 2, 8);
                Sound.play(Sound.REV);
            }
            if (!in.down()) {
                spinDash = false;
                rolling = true;
                vx = facing * (8 + charge);
                charge = 0;
                Sound.play(Sound.DASH);
            }
            return;
        }

        if (rolling) {
            if ((left && vx > 0) || (right && vx < 0)) {
                vx = approachZero(vx, ROLL_BRAKE);
            }
            vx = approachZero(vx, ROLL_FRICTION);
            if (Math.abs(vx) < 0.5) {
                rolling = false;
                vx = 0;
            }
        } else {
            crouching = false;
            if (left && !right) {
                if (vx > 0) {
                    vx -= DEC;
                    if (vx <= 0) {
                        vx = -0.5;
                    }
                } else if (vx > -TOP) {
                    vx = Math.max(vx - ACC, -TOP);
                }
            } else if (right && !left) {
                if (vx < 0) {
                    vx += DEC;
                    if (vx >= 0) {
                        vx = 0.5;
                    }
                } else if (vx < TOP) {
                    vx = Math.min(vx + ACC, TOP);
                }
            } else {
                vx = approachZero(vx, FRICTION);
            }
            if (vx < 0) {
                facing = -1;
            } else if (vx > 0) {
                facing = 1;
            } else if (left) {
                facing = -1;
            } else if (right) {
                facing = 1;
            }
            if (in.down() && !left && !right) {
                if (Math.abs(vx) < 1) {
                    crouching = true;
                    vx = 0;
                } else {
                    rolling = true;
                    Sound.play(Sound.ROLL);
                }
            } else if (in.up() && vx == 0) {
                lookingUp = true;
            }
        }

        if (in.jumpPressed()) {
            if (crouching) {
                crouching = false;
                spinDash = true;
                charge = 2;
                Sound.play(Sound.REV);
            } else {
                vy = JUMP;
                jumping = true;
                rolling = false;
                onGround = false;
                Sound.play(Sound.JUMP);
            }
        }
    }

    private void airControl(Input in) {
        crouching = false;
        lookingUp = false;
        if (in.left() && !in.right()) {
            if (vx > -TOP) {
                vx = Math.max(vx - AIR_ACC, -TOP);
            }
            facing = -1;
        } else if (in.right() && !in.left()) {
            if (vx < TOP) {
                vx = Math.min(vx + AIR_ACC, TOP);
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

    private static double approachZero(double v, double amount) {
        if (v > 0) {
            return Math.max(0, v - amount);
        }
        return Math.min(0, v + amount);
    }

    private void moveX(Level level) {
        x += vx;
        int top = Level.toTile(y);
        int bot = Level.toTile(y + H - 0.01);
        if (vx > 0) {
            int col = Level.toTile(x + W - 0.01);
            for (int r = top; r <= bot; r++) {
                if (level.isWall(col, r)) {
                    x = col * Game.T - W;
                    vx = 0;
                    break;
                }
            }
        } else if (vx < 0) {
            int col = Level.toTile(x);
            for (int r = top; r <= bot; r++) {
                if (level.isWall(col, r)) {
                    x = (col + 1) * Game.T;
                    vx = 0;
                    break;
                }
            }
        }
    }

    private void moveY(Level level, Input in) {
        double prevBottom = y + H;
        boolean wasOnGround = onGround;
        y += vy;
        onGround = false;
        int l = Level.toTile(x);
        int r = Level.toTile(x + W - 0.01);
        if (vy > 0) {
            int row = Level.toTile(y + H - 0.01);
            boolean landed = false;
            boolean spikes = false;
            for (int c = l; c <= r; c++) {
                byte t = level.tile(c, row);
                if (t == Level.SOLID || t == Level.SPIKE
                        || (t == Level.ONEWAY && prevBottom <= row * Game.T + 0.5)) {
                    landed = true;
                    if (t == Level.SPIKE) {
                        spikes = true;
                    }
                }
            }
            if (landed) {
                y = row * Game.T - H;
                vy = 0;
                onGround = true;
                touchedSpikes = spikes;
                if (!wasOnGround) {
                    land(in);
                }
            }
        } else if (vy < 0) {
            int row = Level.toTile(y);
            for (int c = l; c <= r; c++) {
                if (level.isWall(c, row)) {
                    y = (row + 1) * Game.T;
                    vy = 0;
                    break;
                }
            }
        }
    }

    private void land(Input in) {
        jumping = false;
        springing = false;
        combo = 0;
        if (hurt) {
            hurt = false;
            vx = 0;
        }
        if (in.down() && Math.abs(vx) > 1) {
            rolling = true;
        }
    }

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

    void launch(double power) {
        vy = power;
        onGround = false;
        jumping = false;
        rolling = false;
        spinDash = false;
        crouching = false;
        springing = true;
    }

    void knockBack(double fromX) {
        hurt = true;
        jumping = false;
        rolling = false;
        spinDash = false;
        crouching = false;
        springing = false;
        onGround = false;
        vy = -4.5;
        vx = centerX() < fromX ? -2.5 : 2.5;
        invuln = 120;
    }

    void die() {
        if (dead) {
            return;
        }
        dead = true;
        hurt = false;
        rolling = false;
        jumping = false;
        spinDash = false;
        vx = 0;
        vy = -8;
        deadTimer = 0;
        Sound.play(Sound.DIE);
    }

    // ------------------------------------------------------------------ dessin

    void draw(Graphics2D g, double camX, int tick) {
        if (invuln > 0 && !dead && (invuln / 3) % 2 == 0) {
            return;
        }
        AffineTransform old = g.getTransform();
        g.translate(x + W / 2.0 - camX, y + H);
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
        g.setTransform(old);
    }

    private void drawBall(Graphics2D g) {
        g.translate(0, -15);
        if (spinDash) {
            g.setColor(new Color(255, 255, 255, 120));
            for (int i = 1; i <= 3; i++) {
                g.fillOval(-facing * i * 7 - 6, 6 - i * 3, 12, 6);
            }
        }
        g.rotate(Math.toRadians(anim) * facing);
        g.setColor(BODY);
        g.fill(new Ellipse2D.Double(-15, -15, 30, 30));
        g.setColor(BODY_DARK);
        g.setStroke(new BasicStroke(3f));
        for (int i = 0; i < 3; i++) {
            g.rotate(Math.PI * 2 / 3);
            g.drawArc(-11, -11, 22, 22, 0, 70);
        }
        g.setColor(SKIN);
        g.fillOval(2, -6, 9, 9);
        g.setColor(SCARF);
        g.fillOval(-10, 4, 8, 6);
        g.setStroke(new BasicStroke(1f));
    }

    private void drawBody(Graphics2D g, int tick) {
        double speed = Math.abs(vx);
        boolean airborne = !onGround && !dead;
        double lean = onGround ? Math.min(speed * 0.025, 0.2) : 0;
        g.rotate(lean, 0, -16);

        // jambes
        double phase = anim * 0.12;
        if (onGround && speed >= 9) {
            // à pleine vitesse, les jambes tournent en roue floue
            g.setColor(new Color(SHOE.getRed(), SHOE.getGreen(), SHOE.getBlue(), 160));
            g.fillOval(-11, -16, 22, 18);
            g.setColor(new Color(255, 255, 255, 120));
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
        g.fillPolygon(new int[] {-3, -3 - tail, -3 - tail + 2, 0},
                new int[] {-27, (int) (-29 + wave), (int) (-23 + wave), -24}, 4);

        // piquants
        g.setColor(BODY);
        Polygon quills = new Polygon(
                new int[] {0, -21, -9, -24, -9, -19, -4},
                new int[] {-40, -42, -33, -29, -24, -15, -19}, 7);
        g.fillPolygon(quills);
        g.setColor(BODY_DARK);
        g.drawPolygon(quills);

        // corps
        g.setColor(BODY);
        g.fillOval(-8, -27, 16, 17);
        g.setColor(SKIN);
        g.fillOval(-2, -24, 9, 12);

        // bras
        double arm = airborne ? -1.0 : (onGround ? -Math.sin(phase) * Math.min(speed / TOP, 1) : 0);
        if (hurt || dead) {
            arm = -2.2;
        }
        int hx = (int) (2 + Math.cos(arm + 1.2) * 9);
        int hy = (int) (-22 + Math.sin(arm + 1.2) * 9);
        g.setColor(SKIN);
        g.setStroke(new BasicStroke(3f));
        g.drawLine(1, -22, hx, hy);
        g.setStroke(new BasicStroke(1f));
        g.setColor(Color.WHITE);
        g.fillOval(hx - 3, hy - 3, 7, 7);
        g.setColor(Color.GRAY);
        g.drawOval(hx - 3, hy - 3, 7, 7);

        // écharpe autour du cou
        g.setColor(SCARF);
        g.fillRoundRect(-7, -29, 14, 5, 4, 4);

        // tête
        int lookY = lookingUp ? -3 : 0;
        g.setColor(BODY);
        g.fillOval(-10, -46 + lookY, 22, 21);
        g.fillPolygon(new int[] {-4, 0, 4}, new int[] {-42 + lookY, -51 + lookY, -43 + lookY}, 3);
        g.setColor(SKIN);
        g.fillOval(-1, -42 + lookY, 4, 5);
        g.fillOval(3, -35 + lookY, 12, 9);
        // yeux
        g.setColor(Color.WHITE);
        g.fillOval(1, -43 + lookY, 9, 11);
        g.setColor(Color.BLACK);
        if (dead) {
            g.drawLine(3, -40, 8, -35);
            g.drawLine(8, -40, 3, -35);
        } else {
            g.fillOval(5, -40 + lookY - (lookingUp ? 2 : 0), 4, 6);
            g.setColor(Color.WHITE);
            g.fillRect(6, -39 + lookY, 1, 1);
        }
        // nez et sourire
        g.setColor(Color.BLACK);
        g.fillOval(13, -35 + lookY, 4, 4);
        g.drawArc(6, -34 + lookY, 7, 5, 200, 120);

        g.rotate(-lean, 0, -16);
    }

    private void drawLeg(Graphics2D g, double angle, boolean back) {
        int hipY = -11;
        int fx = (int) (Math.sin(angle) * 10);
        int fy = (int) (hipY + Math.cos(angle) * 10);
        g.setColor(back ? BODY_DARK : BODY);
        g.setStroke(new BasicStroke(3.5f));
        g.drawLine(0, hipY, fx, fy);
        g.setStroke(new BasicStroke(1f));
        g.setColor(back ? SHOE.darker() : SHOE);
        g.fillRoundRect(fx - 5, fy - 4, 14, 7, 6, 6);
        g.setColor(Color.WHITE);
        g.fillRect(fx, fy - 4, 3, 7);
    }
}
