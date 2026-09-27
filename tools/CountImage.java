import com.gfxiong.gf.core.Counter;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/** Desktop entry point; only counter-core is included in Android. */
public final class CountImage {
    public static BufferedImage read(File file) throws IOException {
        BufferedImage raw = ImageIO.read(file);
        if (raw == null) throw new IOException("Unsupported or damaged image: " + file);
        double scale = Math.min(1.0, Counter.MAX_SIDE / (double) Math.max(raw.getWidth(), raw.getHeight()));
        int w = Math.max(1, (int) Math.round(raw.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(raw.getHeight() * scale));
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, w, h);
        graphics.drawImage(raw, 0, 0, w, h, null);
        graphics.dispose();
        return image;
    }

    public static Counter.Result count(BufferedImage image) {
        int w = image.getWidth(), h = image.getHeight();
        return new Counter().count(image.getRGB(0, 0, w, h, null, 0, w), w, h);
    }

    public static void annotate(BufferedImage image, Counter.Result result, File output) throws IOException {
        Graphics2D graphics = image.createGraphics();
        graphics.setStroke(new BasicStroke(3));
        graphics.setFont(new Font("SansSerif", Font.BOLD, 24));
        int number = 0;
        for (Counter.Box box : result.boxes) {
            graphics.setColor(new Color(0, 150, 110));
            graphics.drawRect(box.left, box.top, box.right - box.left + 1, box.bottom - box.top + 1);
            graphics.setColor(Color.BLACK);
            graphics.drawString(String.valueOf(++number), box.left + 4, box.top + 24);
        }
        graphics.dispose();
        if (output.getParentFile() != null) output.getParentFile().mkdirs();
        if (!ImageIO.write(image, "png", output)) throw new IOException("PNG writer unavailable");
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: java -cp build/smoke CountImage input.png [build/result.png]");
            System.exit(2);
        }
        BufferedImage image = read(new File(args[0]));
        Counter.Result result = count(image);
        File output = new File(args.length == 2 ? args[1] : "build/result.png");
        annotate(image, result, output);
        System.out.println("count=" + result.count() + ", warnings=" + result.warnings);
        System.out.println("Review every numbered box: " + output);
        System.out.println("An empty warning list is NOT a guarantee of accuracy.");
    }
}
