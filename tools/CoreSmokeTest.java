import com.gfxiong.gf.core.Counter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CoreSmokeTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        int passed = 0, limitations = 0;
        // The label file is ground truth. Unsupported cases are reported separately.
        for (String row : Files.readAllLines(Path.of("test-images/labels.txt"))) {
            if (row.isBlank() || row.startsWith("#")) continue;
            String[] columns = row.split("\\s+");
            String file = columns[0], group = columns[2], warning = columns[3];
            int truth = Integer.parseInt(columns[1]);
            Counter.Result result = CountImage.count(CountImage.read(new File("test-images/synthetic", file)));
            if (group.equals("supported")) {
                check(result.count() == truth, file + ": expected " + truth + ", got " + result.count());
                check(result.warnings.isEmpty(), file + ": unexpected warning");
                passed++;
            } else if (group.equals("guard")) {
                check(result.warnings.stream().anyMatch(w -> w.name().equals(warning)), file + ": missing warning");
                passed++;
            } else if (group.equals("limitation")) {
                limitations++;
                System.out.println("LIMITATION " + file + ": ground truth=" + truth + ", observed=" + result.count());
            } else throw new AssertionError("Unknown group: " + group);
        }
        Counter counter = new Counter();
        check(counter.count(new int[100 * 100], 100, 100).count() == 0, "Transparent pixels must be white");
        try {
            counter.count(new int[1], 100, 100);
            throw new AssertionError("Invalid size accepted");
        } catch (IllegalArgumentException expected) { }
        try {
            counter.count(new int[1000], 1000, 1);
            throw new AssertionError("Unbounded input accepted");
        } catch (IllegalArgumentException expected) { }
        System.out.println("PASS " + passed + " image checks + 3 input checks; "
                + limitations + " limitations reported (not accuracy passes).");
    }
}
