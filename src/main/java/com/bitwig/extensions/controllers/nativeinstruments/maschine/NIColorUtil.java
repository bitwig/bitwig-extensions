package com.bitwig.extensions.controllers.nativeinstruments.maschine;

import java.util.HashMap;
import java.util.Map;

import com.bitwig.extension.controller.api.SettableColorValue;

public class NIColorUtil {

	private static final Hsb BLACK_HSB = new Hsb(0, 0, 0);
	private static final Map<Integer, Integer> fixedColorTable = new HashMap<Integer, Integer>();
	private static final Map<Integer, Integer> colorTable = new HashMap<Integer, Integer>();

	static {
		fixedColorTable.put(13016944, 16);
		fixedColorTable.put(5526612, 68);
		fixedColorTable.put(8026746, 68);
		fixedColorTable.put(13224393, 68);
		fixedColorTable.put(8817068, 52);
		fixedColorTable.put(10713411, 12);
		fixedColorTable.put(5726662, 48);
		fixedColorTable.put(8686304, 48);
		fixedColorTable.put(9783755, 52);
		fixedColorTable.put(14235761, 60);
		fixedColorTable.put(14233124, 4);
		fixedColorTable.put(16733958, 8);
		fixedColorTable.put(14261520, 16);
		fixedColorTable.put(7575572, 24);
		fixedColorTable.put(40263, 28);
		fixedColorTable.put(42644, 32);
		fixedColorTable.put(39385, 44);
		fixedColorTable.put(12351216, 52);
		fixedColorTable.put(14771857, 64);
		fixedColorTable.put(15491415, 12);
		fixedColorTable.put(16745278, 12);
		fixedColorTable.put(14989134, 16);
		fixedColorTable.put(10534988, 24);
		fixedColorTable.put(4111202, 32);
		fixedColorTable.put(4444857, 36);
		fixedColorTable.put(4507903, 40);
		fixedColorTable.put(8355711, 68);
	}

	public static int convertColor(final SettableColorValue color) {
		return convertColor(color.red(), color.green(), color.blue());
	}

	public static int convertColor(final float red, final float green, final float blue) {
		if (red == 0 && green == 0 && blue == 0) {
			return 0;
		}

		final int rv = (int) Math.floor(red * 255);
		final int gv = (int) Math.floor(green * 255);
		final int bv = (int) Math.floor(blue * 255);
		final int lookupIndex = rv << 16 | gv << 8 | bv;
		if (fixedColorTable.containsKey(lookupIndex)) {
			return fixedColorTable.get(lookupIndex);
		}
		if (colorTable.containsKey(lookupIndex)) {
			return colorTable.get(lookupIndex);
		}

		final Hsb hsb = rgbToHsb(red, green, blue);
		if (hsb.bright < 1 || hsb.sat < 3) {
			colorTable.put(lookupIndex, 68);
			return colorTable.get(lookupIndex);
		}
		int off = 0;
		if (hsb.bright + hsb.sat < 22) {
			off = 1;
		}
		if (2 <= hsb.hue && hsb.hue <= 6 && hsb.bright < 13) {
			off = 2;
		}
		final int color_index = Math.min(hsb.hue + off + 1, 16);
		final int color = color_index << 2;
		colorTable.put(lookupIndex, color);
		return color;
	}

	private static Hsb rgbToHsb(final float rv, final float gv, final float bv) {
		final float rgb_max = Math.max(Math.max(rv, gv), bv);
		final float rgb_min = Math.min(Math.min(rv, gv), bv);
		final int bright = (int) rgb_max;
		if (bright == 0) {
			return BLACK_HSB; // Dark Dark Black
		}
		final int sat = (int) (255 * (rgb_max - rgb_min) / bright);
		if (sat == 0) {
			return BLACK_HSB; // White
		}
		float hue = 0;
		if (rgb_max == rv) {
			hue = 0 + 43 * (gv - bv) / (rgb_max - rgb_min);
		} else if (rgb_max == gv) {
			hue = 85 + 43 * (bv - rv) / (rgb_max - rgb_min);
		} else {
			hue = 171 + 43 * (rv - gv) / (rgb_max - rgb_min);
		}
		if (hue < 0) {
			hue = 256 + hue;
		}
		return new Hsb((int) Math.floor(hue / 16.0 + 0.3), sat >> 4, bright >> 4);
	}

	public static class Hsb {
		private final int hue;
		private final int sat;
		private final int bright;

		public Hsb(final int hue, final int sat, final int bright) {
			super();
			this.hue = hue;
			this.sat = sat;
			this.bright = bright;
		}

	}
}
