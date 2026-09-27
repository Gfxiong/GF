package com.gfxiong.gf.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** First baseline: dark, separated objects on a light background. No spacing assumption. */
public final class Counter {
    public static final int MAX_SIDE = 960;
    private static final int DARK_THRESHOLD = 190;
    private static final double MIN_AREA_FRACTION = 0.0008;

    public enum Warning { CLIPPED_OBJECT, LARGE_REGION }

    /** Coordinates are inclusive and refer to the resized input image. */
    public static final class Box {
        public final int left, top, right, bottom, area;
        Box(int left, int top, int right, int bottom, int area) {
            this.left = left; this.top = top; this.right = right;
            this.bottom = bottom; this.area = area;
        }
    }

    public static final class Result {
        public final List<Box> boxes;
        public final List<Warning> warnings;
        Result(List<Box> boxes, List<Warning> warnings) {
            this.boxes = Collections.unmodifiableList(boxes);
            this.warnings = Collections.unmodifiableList(warnings);
        }
        public int count() { return boxes.size(); }
        public boolean needsReview() { return !warnings.isEmpty(); }
    }

    public Result count(int[] argb, int width, int height) {
        if (argb == null || width <= 0 || height <= 0
                || width > MAX_SIDE || height > MAX_SIDE
                || argb.length != width * height) {
            throw new IllegalArgumentException("Use a valid ARGB image, longest side <= 960.");
        }
        int n = argb.length;
        boolean[] foreground = new boolean[n];
        int[] queue = new int[n];
        for (int i = 0; i < n; i++) {
            int p = argb[i];
            int alpha = (p >>> 24) & 255;
            int light = (299 * ((p >>> 16) & 255) + 587 * ((p >>> 8) & 255)
                    + 114 * (p & 255)) / 1000;
            // Composite transparent input over white before thresholding.
            light = (light * alpha + 255 * (255 - alpha)) / 255;
            foreground[i] = light < DARK_THRESHOLD;
        }
        int minArea = Math.max(12, (int) Math.ceil(n * MIN_AREA_FRACTION));
        List<Box> boxes = new ArrayList<>();
        List<Warning> warnings = new ArrayList<>();
        for (int seed = 0; seed < n; seed++) {
            if (!foreground[seed]) continue;
            int head = 0, tail = 1;
            queue[0] = seed;
            foreground[seed] = false;
            int left = width, top = height, right = 0, bottom = 0;
            while (head < tail) {
                int p = queue[head++], x = p % width, y = p / width;
                left = Math.min(left, x); right = Math.max(right, x);
                top = Math.min(top, y); bottom = Math.max(bottom, y);
                // Eight-connected components: inspect pixels, never estimate a repeated gap.
                for (int yy = Math.max(0, y - 1); yy <= Math.min(height - 1, y + 1); yy++) {
                    for (int xx = Math.max(0, x - 1); xx <= Math.min(width - 1, x + 1); xx++) {
                        int q = yy * width + xx;
                        if (foreground[q]) {
                            foreground[q] = false;
                            queue[tail++] = q;
                        }
                    }
                }
            }
            if (tail < minArea) continue; // Ignore only tiny specks, not narrow gaps.
            boxes.add(new Box(left, top, right, bottom, tail));
            if (left == 0 || top == 0 || right == width - 1 || bottom == height - 1) {
                addWarning(warnings, Warning.CLIPPED_OBJECT);
            }
            if (tail > n * 0.45) addWarning(warnings, Warning.LARGE_REGION);
        }
        boxes.sort(Comparator.comparingInt((Box b) -> b.top).thenComparingInt(b -> b.left));
        return new Result(boxes, warnings);
    }

    private static void addWarning(List<Warning> warnings, Warning warning) {
        if (!warnings.contains(warning)) warnings.add(warning);
    }
}
