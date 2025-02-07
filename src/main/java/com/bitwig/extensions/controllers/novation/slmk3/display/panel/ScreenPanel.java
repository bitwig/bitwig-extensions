package com.bitwig.extensions.controllers.novation.slmk3.display.panel;

import com.bitwig.extensions.controllers.novation.slmk3.display.ButtonMode;
import com.bitwig.extensions.controllers.novation.slmk3.display.ScreenConfigSource;
import com.bitwig.extensions.controllers.novation.slmk3.display.ScreenLayout;
import com.bitwig.extensions.controllers.novation.slmk3.display.SlRgbState;
import com.bitwig.extensions.controllers.novation.slmk3.display.SysExScreenBuilder;

public abstract class ScreenPanel {
    
    protected final int columnIndex;
    protected ScreenConfigSource parent;
    
    public ScreenPanel(final int columnIndex) {
        this.columnIndex = columnIndex;
    }
    
    public abstract ScreenLayout getLayout();
    
    public int getIndex() {
        return columnIndex;
    }
    
    public void setConfigParent(final ScreenConfigSource configSource) {
        this.parent = configSource;
    }
    
    public abstract void applySubPanel(SelectionSubPanel subPanel);
    
    public abstract void update(SysExScreenBuilder builder);
    
    public void updateSelection(final SysExScreenBuilder builder) {
    
    }
    
    public void updateText(final int index, final String text) {
        if (!parent.isActive()) {
            return;
        }
        final SysExScreenBuilder sb = new SysExScreenBuilder();
        sb.appendText(this.columnIndex, index, text);
        parent.getMidiProcessor().send(sb);
    }
    
    public void updateBool(final int index, final boolean value) {
        if (!parent.isActive()) {
            return;
        }
        final SysExScreenBuilder sb = new SysExScreenBuilder();
        sb.appendValue(this.columnIndex, index, value ? 1 : 0);
        parent.getMidiProcessor().send(sb);
    }
    
    public void updateColor(final int index, final SlRgbState color) {
        if (!parent.isActive()) {
            return;
        }
        final SysExScreenBuilder sb = new SysExScreenBuilder();
        sb.appendRgb(this.columnIndex, index, color);
        parent.getMidiProcessor().send(sb);
    }
    
    public void updateValue(final int index, final int value) {
        if (!parent.isActive()) {
            return;
        }
        final SysExScreenBuilder sb = new SysExScreenBuilder();
        sb.appendValue(this.columnIndex, index, value);
        parent.getMidiProcessor().send(sb);
    }
    
    public abstract void updateSelectText1(final ButtonMode selectionType, final String value);
    
    public abstract void updateSelectText2(final ButtonMode selectionType, final String value);
    
    public abstract void updateSelectColor(final ButtonMode selectionType, final SlRgbState color);
    
    public abstract void updateSelectSelected(final ButtonMode selectionType, final boolean selected);
    
}
