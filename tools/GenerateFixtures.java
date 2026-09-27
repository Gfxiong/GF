import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/** Deterministic drawings for regression tests, NOT photographs or training data. */
public final class GenerateFixtures {
    private static final File DIRECTORY = new File("test-images/synthetic");
    private static final int[] UNEVEN = {38, 91, 202, 257, 395, 485, 664};

    private static void save(String name, int[] rows, boolean shifted, boolean noise, Color color)
            throws IOException {
        BufferedImage image = new BufferedImage(400, 760, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(247, 247, 247));
        graphics.fillRect(0, 0, 400, 760);
        for (int i = 0; i < rows.length; i++) {
            int x = 140 + (shifted ? (i % 3 - 1) * 18 : 0);
            graphics.setColor(color);
            graphics.fillRoundRect(x, rows[i], 120, 36, 12, 12);
        }
        if (noise) {
            graphics.setColor(Color.BLACK);
            for (int i = 0; i < 80; i++) graphics.fillRect(20 + i % 8 * 7, 20 + i / 8 * 65, 2, 2);
        }
        graphics.dispose();
        ImageIO.write(image, "png", new File(DIRECTORY, name));
    }

    public static void main(String[] args) throws IOException {
        DIRECTORY.mkdirs();
        Color blue = new Color(37, 90, 160);
        save("empty-0.png", new int[]{}, false, false, blue);
        save("single-1.png", new int[]{320}, false, false, blue);
        save("uniform-7.png", new int[]{40, 140, 240, 340, 440, 540, 640}, false, false, blue);
        save("uneven-7.png", UNEVEN, false, false, blue);
        save("shifted-7.png", UNEVEN, true, false, blue);
        save("noise-7.png", UNEVEN, false, true, blue);
        save("clipped-2.png", new int[]{-12, 200}, false, false, blue);
        save("touching-2.png", new int[]{200, 232}, false, false, blue);
        save("low-contrast-3.png", new int[]{100, 300, 570}, false, false, new Color(225, 225, 225));
        BufferedImage dark = new BufferedImage(400, 760, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(dark, "png", new File(DIRECTORY, "dark-background.png"));
        System.out.println("Wrote 10 synthetic fixtures. Ground truth: test-images/labels.txt");
    }
}
