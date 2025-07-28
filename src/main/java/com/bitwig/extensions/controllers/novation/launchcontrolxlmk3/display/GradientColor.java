package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display;

import java.util.ArrayList;
import java.util.List;

public class GradientColor {
    
    private final List<RgbColor> sequence = new ArrayList<>();
    
    public static final int SEQ_N = 16;
    public final static RgbColor C_TURQUOISE = new RgbColor(0x0, 0x40, 0x7f);
    public final static RgbColor C_ORANGE = new RgbColor(0x7f, 0x37, 0x00);
    public final static RgbColor CENTER = new RgbColor(0x0F, 0x0F, 0x0F);
    
    public final static GradientColor RED = GradientColor.generate(0x7F, 0, 0, SEQ_N);
    public final static GradientColor WHITE = GradientColor.generate(0x7F, 0x7F, 0x7F, SEQ_N);
    public final static GradientColor GREEN = GradientColor.generate(0, 0x7F, 0, SEQ_N);
    public final static GradientColor BLUE = GradientColor.generate(0, 0x0, 0x7F, SEQ_N);
    public final static GradientColor CYAN = GradientColor.generate(0, 0x7F, 0x7F, SEQ_N);
    public final static GradientColor YELLOW = GradientColor.generate(0x7f, 0x7F, 0x00, SEQ_N);
    public final static GradientColor ORANGE = GradientColor.generate(0x7f, 0x37, 0x00, SEQ_N);
    public final static GradientColor DARK_GREEN = GradientColor.generate(0x0B, 0x69, 0x30, SEQ_N);
    public final static GradientColor PURPLE = GradientColor.generate(0x3a, 0x05, 0x7F, SEQ_N);
    public final static GradientColor PINK = GradientColor.generate(0x7F, 0x1A, 0x48, SEQ_N);
    public final static GradientColor PAN = GradientColor.generateBiPolar(C_TURQUOISE, C_ORANGE, 16);
    
    public static GradientColor generate(final int red, final int green, final int blue, final int n) {
        final GradientColor gradient = new GradientColor();
        gradient.sequence.addAll(genSequence(red, green, blue, n, 0));
        return gradient;
    }
    
    private static List<RgbColor> genSequence(final int red, final int green, final int blue, final int n,
        final int offset) {
        final List<RgbColor> colors = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            final double factor = (double) (i + offset) / (n + offset - 1);
            final int r = (int) Math.round(red * factor);
            final int g = (int) Math.round(green * factor);
            final int b = (int) Math.round(blue * factor);
            colors.add(new RgbColor(r, g, b));
        }
        return colors;
    }
    
    public static GradientColor generateBiPolar(final RgbColor leftColor, final RgbColor rightColor, final int n) {
        final GradientColor gradient = new GradientColor();
        final List<RgbColor> left = genSequence(leftColor.red(), leftColor.green(), leftColor.blue(), n, 1);
        final List<RgbColor> right = genSequence(rightColor.red(), rightColor.green(), rightColor.blue(), n, 1);
        gradient.sequence.addAll(left.reversed());
        gradient.sequence.add(CENTER);
        gradient.sequence.addAll(right);
        return gradient;
    }
    
    public int length() {
        return sequence.size();
    }
    
    public RgbColor getColor(final int index) {
        return sequence.get(index);
    }
}
