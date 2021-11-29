package com.bitwig.extensions.controllers.mackie;

import com.bitwig.extensions.controllers.mackie.section.Scale;
import com.bitwig.extensions.controllers.mackie.value.IntValueObject;
import com.bitwig.extensions.controllers.mackie.value.ValueObject;

public class NotePlayingSetup {
	private final static String[] NOTES = { "  C", " C#", "  D", " D#", "  E", "  F", " F#", "  G", " G#", "  A", " A#",
			"  B" };

	private final ValueObject<Scale> scale = new ValueObject<>(Scale.MINOR, NotePlayingSetup::increment,
			NotePlayingSetup::convert);
	private final IntValueObject baseNote = new IntValueObject(0, 0, 11, v -> NOTES[v]);
	private final IntValueObject octaveOffset = new IntValueObject(4, 0, 6);
	private final IntValueObject layoutOffset = new IntValueObject(4, 3, 4);
	private final IntValueObject velocity = new IntValueObject(100, 0, 127);

	public NotePlayingSetup() {
		super();
	}

	private static Scale increment(final Scale current, final int amount) {
		final int ord = current.ordinal();
		final Scale[] values = Scale.values();
		final int newOrd = ord + amount;
		if (newOrd < 0) {
			return values[0];
		}
		if (newOrd >= values.length) {
			return values[values.length - 1];
		}
		return values[newOrd];
	}

	private static String convert(final Scale scale) {
		return scale.getShortName();
	}

	public ValueObject<Scale> getScale() {
		return scale;
	}

	public IntValueObject getBaseNote() {
		return baseNote;
	}

	public IntValueObject getOctaveOffset() {
		return octaveOffset;
	}

	public IntValueObject getLayoutOffset() {
		return layoutOffset;
	}

	public IntValueObject getVelocity() {
		return velocity;
	}

	public void modifyOctave(final int direction) {
		if (direction > 0) {
			if (octaveOffset.get() < 6) {
				octaveOffset.increment(1);
			}
		} else {
			if (octaveOffset.get() > 0) {
				octaveOffset.increment(-1);
			}
		}
	}

}
