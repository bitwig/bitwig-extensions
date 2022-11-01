package com.bitwig.extensions.controllers.novation.launchpad_pro_mk3;

import java.util.ArrayList;

import com.bitwig.extension.controller.api.Clip;

public class PrintToClip {

    // Print to Clip Constants
    private final int START_SLICE = 2;
    private final int LENGTH_INDEX = 2;
    private final int LENGTH_SLICE = 2;
    private final int PITCH_INDEX = 4;
    private final int VELOCITY_INDEX = 5;
    private final int PAYLOAD_OFFSET = 21;
    private final int NOTE_BYTES = 6;

    private final int MICROSTEPS = 6;
    private final double TIMEFACTOR = 500.0;

    private double notes[][];
    private ArrayList<double[][]> notesList = new ArrayList<double[][]>();

    // CONSTRUCTOR
    public PrintToClip() {

    }


    private double absoluteBeatTime(byte[] b, int startindex, int length) {
        double x = 0.0;
        for (int i = 0; i < length; i++) {
            x += b[(startindex + length) - i - 1] << (i * 7);
        }
        x = x / TIMEFACTOR;
        return Math.abs(x);
    }

    private void sysexToNotes(String sysex) {
        byte[] s = new byte[sysex.length() / 2];
        for (int i = 0; i < s.length; i++) {
            int index = i * 2;
            int val = Integer.parseInt(sysex.substring(index, index + 2), 16);
            s[i] = (byte) val;
        }

        notes = new double[(int) ((s.length - 20) / NOTE_BYTES)][4];
        notesList.add(notes);

        for (int i = 0; i < (s.length - 20) / NOTE_BYTES; i++) {
            int offset = PAYLOAD_OFFSET + i * NOTE_BYTES;
            double packetOffset = absoluteBeatTime(s, 18, 3);
            double start = absoluteBeatTime(s, offset, START_SLICE) + packetOffset;
            double length = absoluteBeatTime(s, offset + LENGTH_INDEX, LENGTH_SLICE);
            int pitch = s[offset + PITCH_INDEX];
            int velocity = s[offset + VELOCITY_INDEX];
            notes[i][0] = start;
            notes[i][1] = pitch;
            notes[i][2] = velocity;
            notes[i][3] = length;
        }
    }

    private void printToClip(LaunchpadProMK3Workflow driver, String sysex) {
        if (notesList.size() == 0)
            return;

        byte[] s = new byte[sysex.length() / 2];
        for (int i = 0; i < s.length; i++) {
            int index = i * 2;
            int val = Integer.parseInt(sysex.substring(index, index + 2), 16);
            s[i] = (byte) val;
        }

        double beatTime = absoluteBeatTime(s, 18, 3);
        double stepSize = notesList.get(0)[0][3] / MICROSTEPS;
        driver.mHost.println("Beattime: " + String.valueOf(beatTime));

        driver.mCursorTrack.createNewLauncherClip(0);
        Clip clip = driver.mCursorClip;
        
        clip.getPlayStop().set(beatTime);
        clip.getLoopLength().set(beatTime);
        clip.setStepSize(stepSize);

        for (int j = 0; j < notesList.size(); j++) {
            for (int i = 0; i < notesList.get(j).length; i++) {
                clip.setStep((int) Math.round(notesList.get(j)[i][0] / stepSize), (int) notesList.get(j)[i][1],
                        (int) notesList.get(j)[i][2], notesList.get(j)[i][3]);
            }
        }

        notes = null;
        notesList.clear();
        driver.mHost.println("size: " + Integer.toString(notesList.size()));
    }

    public void print(LaunchpadProMK3Workflow driver, String sysex) {
        if (sysex.length() == 44)
                printToClip(driver, sysex);
            else
                sysexToNotes(sysex);
    }

}
