package com.bitwig.extensions.controllers.novation.launchpadpromk3;

import com.bitwig.extension.controller.api.Clip;

import java.util.ArrayList;
import java.util.List;

public class PrintToClipData {
    private static final int HEX_CHAR_NUM = 58;
    private static final int HEX_CHAR_0 = 48;
    private static final int HEX_CHAR_A = 87;
    private static final int DATA_OFFSET = 13;
    private static final double TIME_FACTOR = 500.0;
    private static final int NOTE_BYTES = 6;
    private static int patternCount = 1;

    private static final double[] RESOLUTIONS = {1.0, 0.666666, 0.5, 0.33333, 0.25, 0.1666666, 0.125, 0.0833333};
    private static final double[] STEP_RES = {500.0, 333.3333, 250.0, 166.666666, 125.0, 83.3333333, 62.5, 41.6666666666};

    public static class Note {
        private final int pitch;
        private final int velocity;
        private final int startTime;
        private final double length;

        private Note(final int pitch, final int velocity, final int startTime, final double length) {
            this.pitch = pitch;
            this.velocity = velocity;
            this.length = length;
            this.startTime = startTime;
        }

        public static Note fromData(final int offset, final byte[] data, final int packetOffset) {
            final int startTime = beatTimeNote(data, offset) + packetOffset;
            final double length = Math.abs(beatTimeNote(data, offset + 2) / TIME_FACTOR);
            final int pitch = data[offset + 4];
            final int velocity = data[offset + 5];
            return new Note(pitch, velocity, startTime, length);
        }

        int calcResolutionMatches() {
            final int lengthResIndex = lengthMatch();

            for (int resolutionIndex = 0; resolutionIndex < STEP_RES.length; resolutionIndex++) {
                final double xPositionDouble = startTime / STEP_RES[resolutionIndex];
                if (isValidXPosition(xPositionDouble)) {
                    return resolutionIndex;
                }
            }
            return lengthResIndex;
        }

        private int lengthMatch() {
            for (int resolutionIndex = 0; resolutionIndex < STEP_RES.length; resolutionIndex++) {
                if (equalsEpsilon(length, RESOLUTIONS[resolutionIndex])) {
                    return resolutionIndex;
                }
            }
            return -1;
        }

        private static int beatTimeNote(final byte[] data, final int offset) {
            return (data[offset] << 7) + data[offset + 1];
        }

        private static boolean isValidXPosition(final double value) {
            final int x = (int) Math.round(value);
            return x < 32 && Math.abs((double) x - value) < 0.01;
        }

        private static boolean equalsEpsilon(final double val1, final double val2) {
            return Math.abs(val1 - val2) < 0.001;
        }

        public int getPitch() {
            return pitch;
        }

        public int getVelocity() {
            return velocity;
        }

        public double getStart() {
            return Math.abs(startTime / TIME_FACTOR);
        }

        public double getLength() {
            return length;
        }

        @Override
        public String toString() {
            return String.format("s=%f l=%f %02d %02d s=%d", getStart(), length, pitch, velocity, startTime);
        }
    }

    private int track;
    private final List<Note> notes = new ArrayList<>();
    private double patternLength;

    void setData(final int type, final String sysExBlock) {
        final int n = sysExBlock.length();
        switch (type) {
            case 1:
                track = Integer.parseInt(sysExBlock.substring(n - 4, n - 2), 16);
                break;
            case 2:
                readNoteData(toDataBlock(sysExBlock));
                break;
            case 3:
                // DebugOut.println("## Len = %f", patternLength);
                break;
            default:
        }
    }

    private double calculateStepSize() {
        int selectRes = -1;
        for (final Note note : notes) {
            selectRes = Math.max(selectRes, note.calcResolutionMatches());
        }
        DebugOutLp.println(" OVERALL Resolution = %f", RESOLUTIONS[selectRes]);
        return RESOLUTIONS[selectRes];
    }

    public void applyToClip(final Clip clip) {
        clip.getPlayStop().set(patternLength);
        clip.getLoopLength().set(patternLength);
        DebugOutLp.println(" PATTERN length = %f", patternLength);
        if (notes.isEmpty()) {
            return;
        }
        final double stepSize = calculateStepSize();
        clip.setStepSize(stepSize / 6);
        for (final Note note : notes) {
            final int x = (int) Math.round(note.getStart() / stepSize * 6);
            final int page = x / 192;
            final int xp = x % 192;
            final int y = note.getPitch();
            clip.scrollToStep(page * 192);
            clip.setStep(xp, y, note.getVelocity(), note.getLength());
        }
        clip.setName(String.format("Lpp#%d - %d", patternCount++, track));
    }

    private void readNoteData(final byte[] data) {
        final int len = data.length - DATA_OFFSET;
        final int packOffset = beatTimePattern(data);
        patternLength = Math.abs((double) packOffset / TIME_FACTOR);
        for (int i = 0; i < len; i += NOTE_BYTES) {
            notes.add(Note.fromData(DATA_OFFSET + i, data, packOffset));
        }
    }

    private byte[] toDataBlock(final String sysEx) {
        final byte[] data = new byte[sysEx.length() / 2];
        for (int i = 0; i < data.length; i++) {
            data[i] = toHex(sysEx.charAt(i * 2), sysEx.charAt(i * 2 + 1));
        }
        return data;
    }

    private static byte toHex(final int c1, final int c2) {
        return (byte) (((c1 < HEX_CHAR_NUM ? c1 - HEX_CHAR_0 : c1 - HEX_CHAR_A) << 4) | (c2 < HEX_CHAR_NUM ? c2 - HEX_CHAR_0 : c2 - 87));
    }

    private static int beatTimePattern(final byte[] data) {
        return (data[PrintToClipData.DATA_OFFSET - 3] << 14) + (data[PrintToClipData.DATA_OFFSET - 2] << 7) + data[PrintToClipData.DATA_OFFSET - 1];
    }

}
