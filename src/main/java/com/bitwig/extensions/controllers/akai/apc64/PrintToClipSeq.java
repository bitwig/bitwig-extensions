package com.bitwig.extensions.controllers.akai.apc64;

import com.bitwig.extension.controller.api.Clip;

import java.util.ArrayList;
import java.util.List;

public class PrintToClipSeq {
    public static final double PPQ_RESOLUTION = 96.0;
    private static int[] STEP_DIVISOR = {96, 64, 48, 32, 24, 16, 12, 8};
    private static final double[] RESOLUTIONS = {1.0, 0.666666, 0.5, 0.33333, 0.25, 0.1666666, 0.125, 0.0833333};

    private List<StepNote> notes = new ArrayList<>();
    private int length;
    private int headValue;

    public record StepNote(int start, int end, int note, int vel, int block, int tail) {
        public StepNote(String sysEx) {
            this(fromHexValueMask(sysEx, 1), fromHexValueMask(sysEx, 5), fromHexValue(sysEx, 3), fromHexValue(sysEx, 4),
                    fromHexValue(sysEx, 0), fromHexValue(sysEx, 7));
        }
    }

    public PrintToClipSeq(int length) {
        this.length = length;
    }

    public void setNotes(List<StepNote> notes) {
        this.notes = notes;
    }

    public void setHeadValue(int headValue) {
        this.headValue = headValue;
    }

    public int getLength() {
        return length;
    }

    public int getHeadValue() {
        return headValue;
    }

    public double getClipLen() {
        return (double) length / PPQ_RESOLUTION;
    }

    private static int fromHexValue(String overall, int offset) {
        return Integer.parseInt(overall.substring(offset * 2, offset * 2 + 2), 16);
    }

    public boolean hasNotes() {
        return !notes.isEmpty();
    }

    private static int fromHexValueMask(String overall, int offset) {
        String hex = overall.substring(offset * 2, offset * 2 + 4);
        int v1 = Integer.parseInt(hex.substring(0, 2), 16);
        int v2 = Integer.parseInt(hex.substring(2, 4), 16);
        return ((v1 & 0x3F) << 7) | v2;
    }

    public void addNoteData(String data) {
        int nrOfNotes = data.length() / 16;
        for (int i = 0; i < nrOfNotes; i++) {
            int offset = i * 16;
            String noteData = data.substring(offset, offset + 16);
            notes.add(new StepNote(noteData));
        }
    }

    public int getFittingIndex(int position) {
        for (int i = 0; i < STEP_DIVISOR.length; i++) {
            if (position % STEP_DIVISOR[i] == 0) {
                return i;
            }
        }
        return -1;
    }

    private int calculateResolutionIndex() {
        int res = 0;
        for (StepNote note : notes) {
            res = Math.max(res, getFittingIndex(note.start));
        }
        return res;
    }

    public void applyToClip(final Clip clip, int count) {
        double clipLen = getClipLen();
        clip.getPlayStop().set(clipLen);
        clip.getLoopLength().set(clipLen);
        clip.setName(String.format("SEQ APC %d".formatted(count)));
        final int resIndex = calculateResolutionIndex();
        clip.setStepSize(RESOLUTIONS[resIndex]);
        for (StepNote note : notes) {
            int x = note.start / STEP_DIVISOR[resIndex];
            int y = note.note;
            double len = (note.end - note.start) / PPQ_RESOLUTION;
            clip.setStep(x, y, note.vel, len);
        }
    }
}
