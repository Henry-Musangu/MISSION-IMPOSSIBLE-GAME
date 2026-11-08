package agenda.desktop;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** L'icône de l'application (une page de calendrier avec le jour du mois), dessinée par le code. */
final class Icons {

    private Icons() {
    }

    static BufferedImage calendar(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = Style.smooth(img.createGraphics());
        double s = size / 64.0;
        g.setColor(new Color(0x1D4ED8));
        Style.fillRound(g, 4 * s, 7 * s, 56 * s, 53 * s, 14 * s);
        g.setColor(Color.WHITE);
        Style.fillRound(g, 8 * s, 21 * s, 48 * s, 35 * s, 8 * s);
        g.setColor(new Color(0x93C5FD));
        Style.fillRound(g, 17 * s, 2 * s, 7 * s, 13 * s, 7 * s);
        Style.fillRound(g, 40 * s, 2 * s, 7 * s, 13 * s, 7 * s);
        String day = String.valueOf(LocalDate.now().getDayOfMonth());
        g.setColor(new Color(0x0F172A));
        g.setFont(new Font(Style.FAMILY, Font.BOLD, Math.max(6, (int) (25 * s))));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(day, (float) (size / 2.0 - fm.stringWidth(day) / 2.0), (float) (48 * s));
        g.dispose();
        return img;
    }

    static List<Image> all() {
        List<Image> list = new ArrayList<Image>();
        for (int s : new int[] {16, 24, 32, 48, 64, 128, 256}) list.add(calendar(s));
        return list;
    }
}
