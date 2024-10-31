package com.bitwig.extensions.controllers.novation.launchkey_mk4.display;

import java.util.Arrays;

public class DisplaySegment {
    private final int targetId;
    private int config;
    private final DisplayControl control;
    
    private final String[] stdLines = new String[2];
    private final String[] paramLines = new String[3];
    
    public DisplaySegment(final int id, final DisplayControl control) {
        targetId = id;
        this.config = 0x61;
        this.control = control;
        Arrays.fill(stdLines, "");
        Arrays.fill(paramLines, "");
    }
    
    public void setLine(final int index, final String text) {
        if (index < stdLines.length) {
            stdLines[index] = text;
        }
    }
    
    public void update2Lines() {
        if (config != 0x61) {
            config = 0x61;
            control.configureDisplay(targetId, config);
        }
        control.setText(targetId, 0, stdLines[0]);
        control.setText(targetId, 1, stdLines[1]);
        control.showDisplay(targetId);
    }
    
    public void update3Lines() {
        if (config != 0x62) {
            config = 0x62;
            control.configureDisplay(targetId, config);
        }
        for (int i = 0; i < paramLines.length; i++) {
            control.setText(targetId, i, paramLines[i]);
        }
        control.showDisplay(targetId);
    }
    
    public void showParamValues(final String title, final String name, final String value) {
        paramLines[0] = title;
        paramLines[1] = name;
        paramLines[2] = value;
        update3Lines();
    }
    
    public void showParamInfo(final String... text) {
        if (config != 0x63) {
            config = 0x63;
            control.configureDisplay(targetId, config);
        }
        for (int i = 0; i < 9; i++) {
            control.setText(targetId, i, i < text.length ? text[i] : "");
        }
        control.showDisplay(targetId);
    }
    
    
}
