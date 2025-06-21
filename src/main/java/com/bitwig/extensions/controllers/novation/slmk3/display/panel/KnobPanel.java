package com.bitwig.extensions.controllers.novation.slmk3.display.panel;

import java.util.Arrays;
import java.util.Objects;

import com.bitwig.extensions.controllers.novation.slmk3.display.ButtonMode;
import com.bitwig.extensions.controllers.novation.slmk3.display.ScreenLayout;
import com.bitwig.extensions.controllers.novation.slmk3.display.SlRgbState;
import com.bitwig.extensions.controllers.novation.slmk3.display.SysExScreenBuilder;

public class KnobPanel extends ScreenPanel {
    private final String[] rows = new String[4];
    private final SlRgbState[] colors = new SlRgbState[3];
    private int knobValue = 0;
    private boolean selected;
    
    public KnobPanel(final int columnIndex) {
        super(columnIndex);
        Arrays.fill(rows, "");
        Arrays.fill(colors, SlRgbState.OFF);
    }
    
    @Override
    public void update(final SysExScreenBuilder builder) {
        for (int i = 0; i < rows.length; i++) {
            builder.appendText(this.columnIndex, i, rows[i]);
        }
        for (int i = 0; i < colors.length; i++) {
            builder.appendRgb(this.columnIndex, i, colors[i]);
        }
    }
    
    public void updateSelection(final SysExScreenBuilder builder) {
        builder.appendValue(this.columnIndex, 0, knobValue);
        builder.appendValue(this.columnIndex, 1, selected ? 1 : 0);
    }
    
    @Override
    public ScreenLayout getLayout() {
        return ScreenLayout.KNOB;
    }
    
    @Override
    public void applySubPanel(final SelectionSubPanel subPanel) {
        setText(2, subPanel.getRow1());
        setText(3, subPanel.getRow2());
        setSelected(subPanel.isSelected());
        setColor(2, subPanel.getColor());
    }
    
    public void setText(final int rowIndex, final String text) {
        final String setValue = text == null ? "" : text;
        if (!Objects.equals(rows[rowIndex], setValue)) {
            rows[rowIndex] = text;
            updateText(rowIndex, text);
        }
    }
    
    public void setKnobValue(final int knobValue) {
        if (this.knobValue != knobValue) {
            this.knobValue = knobValue;
            updateValue(0, this.knobValue);
        }
    }
    
    public void setColor(final int rowIndex, final SlRgbState color) {
        if (!Objects.equals(colors[rowIndex], color)) {
            colors[rowIndex] = color;
            updateColor(rowIndex, color);
        }
    }
    
    public void setSelected(final boolean selected) {
        if (this.selected != selected) {
            this.selected = selected;
            updateBool(1, selected);
        }
    }
    
    public void setTopBarColor(final SlRgbState topBarColor) {
        setColor(0, topBarColor);
    }
    
    public void setKnobIconColor(final SlRgbState knobIconColor) {
        setColor(1, knobIconColor);
    }
    
    public void updateSelectText1(final ButtonMode selectionType, final String value) {
        setText(2, value);
    }
    
    public void updateSelectText2(final ButtonMode selectionType, final String value) {
        setText(3, value);
    }
    
    public void updateSelectColor(final ButtonMode selectionType, final SlRgbState color) {
        setColor(2, color);
    }
    
    public void updateSelectSelected(final ButtonMode selectionType, final boolean selected) {
        setSelected(selected);
    }
    
}
