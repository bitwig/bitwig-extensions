package com.bitwig.extensions.controllers.mackie;

public class StringUtil {

	private static final char[] SPECIALS = { 'ä', 'ü', 'ö', 'Ä', 'Ü', 'Ö', 'ß', 'é', 'è', 'ê', 'â', 'á', 'à', //
			'û', 'ú', 'ù', 'ô', 'ó', 'ò' };
	private static final String[] REPLACE = { "a", "u", "o", "A", "U", "O", "ss", "e", "e", "e", "a", "a", "a", //
			"u", "u", "u", "o", "o", "o" };

	private StringUtil() {
	}

	public static String padString(final String text, final int pad) {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < pad; i++) {
			sb.append(' ');
		}
		sb.append(text);
		return sb.toString();
	}

	public static String toAsciiDisplay(final String name, final int maxLen) {
		final String s = name.substring(0, Math.min(7, name.length()));
		final StringBuilder b = new StringBuilder();
		for (int i = 0; i < name.length() && i < maxLen; i++) {
			final char c = s.charAt(i);
			if (c < 128) {
				b.append(c);
			} else {
				final int replacement = getReplace(c);
				if (replacement >= 0) {
					b.append(REPLACE[replacement]);
				}
			}
		}
		return b.toString();
	}

	private static int getReplace(final char c) {
		for (int i = 0; i < SPECIALS.length; i++) {
			if (c == SPECIALS[i]) {
				return i;
			}
		}
		return -1;
	}

}
