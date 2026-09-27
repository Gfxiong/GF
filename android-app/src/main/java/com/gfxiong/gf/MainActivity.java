package com.gfxiong.gf;

import android.content.ActivityNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.gfxiong.gf.core.Counter;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** One screen, one photograph, one locally calculated estimate. */
public final class MainActivity extends ComponentActivity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private ImageView preview;
    private TextView status;
    private Button takeButton, pickButton, sampleButton;
    private Uri pendingPhoto, currentPhoto;
    private boolean showingSample;
    private int jobId;

    private final ActivityResultLauncher<Uri> takePhoto = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), saved -> {
                if (saved && pendingPhoto != null) {
                    currentPhoto = pendingPhoto;
                    showingSample = false;
                    analyzeCurrent();
                } else {
                    status.setText("已取消拍照。可以重新拍照或选择图片。");
                    setBusy(false);
                }
                pendingPhoto = null;
            });

    private final ActivityResultLauncher<String> pickPhoto = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    currentPhoto = uri;
                    showingSample = false;
                    analyzeCurrent();
                } else {
                    status.setText("已取消选择。可以重新选择图片。");
                    setBusy(false);
                }
            });

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(padding + bars.left, padding + bars.top,
                    padding + bars.right, padding + bars.bottom);
            return windowInsets;
        });
        TextView title = new TextView(this);
        title.setText("GF 拍照计数 · 首版");
        title.setTextSize(24);
        root.addView(title);
        TextView guidance = new TextView(this);
        guidance.setText("请单列摆放，使用浅色纯背景。每个物品要完整、互不接触；间距可以不同。结果请逐框核对。");
        root.addView(guidance);
        takeButton = button(root, "拍照计数", view -> launchCamera());
        pickButton = button(root, "选择照片", view -> {
            setBusy(true);
            try { pickPhoto.launch("image/*"); }
            catch (ActivityNotFoundException exception) {
                status.setText("没有可用的图片选择器，请尝试拍照。");
                setBusy(false);
            }
        });
        sampleButton = button(root, "运行 7 个物品示例", view -> {
            currentPhoto = null;
            showingSample = true;
            analyzeCurrent();
        });
        status = new TextView(this);
        status.setTextSize(18);
        status.setText("先运行示例，再拍一张自己的照片。");
        root.addView(status);
        preview = new ImageView(this);
        preview.setContentDescription("计数图片，每个检测到的轮廓有方框和编号");
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        root.addView(preview, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        if (state != null) {
            pendingPhoto = parseUri(state.getString("pendingPhoto"));
            currentPhoto = parseUri(state.getString("currentPhoto"));
            showingSample = state.getBoolean("showingSample");
            if (pendingPhoto == null && (currentPhoto != null || showingSample)) analyzeCurrent();
        }
    }

    private Button button(LinearLayout root, String text, View.OnClickListener action) {
        Button button = new Button(this);
        button.setText(text);
        button.setOnClickListener(action);
        root.addView(button);
        return button;
    }

    private void launchCamera() {
        try {
            File dir = new File(getCacheDir(), "photos");
            if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Cannot create photo cache");
            // Only retain the latest capture. The camera writes a full-resolution image here.
            File file = new File(dir, "capture.jpg");
            pendingPhoto = FileProvider.getUriForFile(this, getPackageName() + ".photos", file);
            setBusy(true);
            status.setText("等待拍照…");
            takePhoto.launch(pendingPhoto);
        } catch (IOException | ActivityNotFoundException | SecurityException exception) {
            pendingPhoto = null;
            status.setText("无法打开相机，请尝试选择照片。");
            setBusy(false);
        }
    }

    private void analyzeCurrent() {
        final int thisJob = ++jobId;
        final Uri uri = currentPhoto;
        final boolean sample = showingSample;
        setBusy(true);
        preview.setImageDrawable(null);
        status.setText("正在计数…");
        worker.execute(() -> {
            try {
                ImageDecoder.Source source = sample
                        ? ImageDecoder.createSource(getAssets(), "uneven-7.png")
                        : ImageDecoder.createSource(getContentResolver(), uri);
                // ImageDecoder applies EXIF orientation. Bound memory before decoding pixels.
                Bitmap bitmap = ImageDecoder.decodeBitmap(source, (decoder, info, src) -> {
                    int width = info.getSize().getWidth(), height = info.getSize().getHeight();
                    double scale = Math.min(1.0, Counter.MAX_SIDE / (double) Math.max(width, height));
                    decoder.setTargetSize(Math.max(1, (int) Math.round(width * scale)),
                            Math.max(1, (int) Math.round(height * scale)));
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                });
                int width = bitmap.getWidth(), height = bitmap.getHeight();
                int[] pixels = new int[width * height];
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
                Counter.Result result = new Counter().count(pixels, width, height);
                Bitmap marked = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                bitmap.recycle();
                Canvas canvas = new Canvas(marked);
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setStrokeWidth(3);
                paint.setTextSize(24);
                int number = 0;
                for (Counter.Box box : result.boxes) {
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setColor(Color.rgb(0, 150, 110));
                    canvas.drawRect(box.left, box.top, box.right + 1, box.bottom + 1, paint);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(Color.BLACK);
                    canvas.drawText(String.valueOf(++number), box.left + 3, box.top + 24, paint);
                }
                runOnUiThread(() -> {
                    if (isDestroyed() || thisJob != jobId) { marked.recycle(); return; }
                    preview.setImageBitmap(marked);
                    if (result.needsReview()) {
                        status.setText("需要重拍：检测到 " + result.count()
                                + " 个轮廓，存在贴边物品或大面积暗区。请保留四周空白并使用浅色背景。");
                    } else if (result.count() == 0) {
                        status.setText("未检测到物品，不代表照片中一定没有物品。请检查背景和光线后重拍。");
                    } else {
                        status.setText((sample ? "合成示例：" : "估计数量：") + result.count()
                                + " 个\n请核对编号；接触、透明或遮挡可能导致误数。");
                    }
                    setBusy(false);
                });
            } catch (IOException | RuntimeException exception) {
                runOnUiThread(() -> {
                    if (isDestroyed() || thisJob != jobId) return;
                    status.setText("无法读取图片，请重新拍照或选择有效的 JPG / PNG 图片。");
                    setBusy(false);
                });
            }
        });
    }

    private void setBusy(boolean busy) {
        takeButton.setEnabled(!busy);
        pickButton.setEnabled(!busy);
        sampleButton.setEnabled(!busy);
    }

    private static Uri parseUri(String value) { return value == null ? null : Uri.parse(value); }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (pendingPhoto != null) out.putString("pendingPhoto", pendingPhoto.toString());
        if (currentPhoto != null) out.putString("currentPhoto", currentPhoto.toString());
        out.putBoolean("showingSample", showingSample);
    }

    @Override protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }
}
