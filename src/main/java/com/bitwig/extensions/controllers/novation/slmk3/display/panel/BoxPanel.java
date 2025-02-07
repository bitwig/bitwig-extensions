package com.bitwig.extensions.controllers.novation.slmk3.display.panel;

import java.util.Arrays;
import java.util.Objects;

import com.bitwig.extensions.controllers.novation.slmk3.display.ButtonMode;
import com.bitwig.extensions.controllers.novation.slmk3.display.ScreenLayout;
import com.bitwig.extensions.controllers.novation.slmk3.display.SlRgbState;
import com.bitwig.extensions.controllers.novation.slmk3.display.SysExScreenBuilder;

public class BoxPanel extends ScreenPanel {
    
    private final String[] rows = new String[6];
    private final SlRgbState[] colors = new SlRgbState[3];
    private boolean topSelected;
    private boolean centerSelected;
    private boolean bottomSelected;
    
    public BoxPanel(final int columnIndex) {
        super(columnIndex);
        Arrays.fill(rows, "");
    }
    
    @Override
    public void update(final SysExScreenBuilder builder) {
        for (int i = 0; i < rows.length; i++) {
            builder.appendText(this.columnIndex, i, rows[i]);
        }
        for (int i = 0; i < colors.length; i++) {
            if (colors[i] != null) {
                builder.appendRgb(this.columnIndex, i, colors[i]);
            }
        }
    }
    
    public void updateSelection(final SysExScreenBuilder builder) {
        builder.appendValue(this.columnIndex, 0, topSelected ? 1 : 0);
        builder.appendValue(this.columnIndex, 1, centerSelected ? 1 : 0);
        builder.appendValue(this.columnIndex, 2, bottomSelected ? 1 : 0);
    }
    
    public void updateSelectText1(final ButtonMode selectionType, final String value) {
        setText(4, value);
    }
    
    public void updateSelectText2(final ButtonMode selectionType, final String value) {
        setText(5, value);
    }
    
    public void updateSelectColor(final ButtonMode selectionType, final SlRgbState color) {
        setColor(2, color);
    }
    
    public void updateSelectSelected(final ButtonMode selectionType, final boolean selected) {
        setBottomSelected(selected);
    }
    
    public void applySubPanel(final SelectionSubPanel subPanel) {
        setText(4, subPanel.getRow1());
        setText(5, subPanel.getRow2());
        setBottomSelected(subPanel.isSelected());
        setColor(2, subPanel.getColor());
    }
    
    public void setText(final int rowIndex, final String text) {
        final String setValue = text == null ? "" : text;
        if (!Objects.equals(rows[rowIndex], setValue)) {
            rows[rowIndex] = text;
            updateText(rowIndex, text);
        }
    }
    
    public void setColor(final int rowIndex, final SlRgbState color) {
        if (!Objects.equals(colors[rowIndex], color)) {
            colors[rowIndex] = color;
            updateColor(rowIndex, color);
        }
    }
    
    public void setTopSelected(final boolean topSelected) {
        if (this.topSelected != topSelected) {
            this.topSelected = topSelected;
            updateBool(0, topSelected);
        }
    }
    
    public void setCenterSelected(final boolean centerSelected) {
        if (this.centerSelected != centerSelected) {
            this.centerSelected = centerSelected;
            updateBool(1, centerSelected);
        }
    }
    
    public void setBottomSelected(final boolean bottomSelected) {
        if (this.bottomSelected != bottomSelected) {
            this.bottomSelected = bottomSelected;
            updateBool(2, bottomSelected);
        }
    }
    
    @Override
    public ScreenLayout getLayout() {
        return ScreenLayout.BOX;
    }
    
    
}
