package com.bitwig.extensions.controllers.novation.slmk3.display.panel;

import java.util.Objects;

import com.bitwig.extensions.controllers.novation.slmk3.MidiProcessor;
import com.bitwig.extensions.controllers.novation.slmk3.display.ButtonMode;
import com.bitwig.extensions.controllers.novation.slmk3.display.ScreenConfigSource;
import com.bitwig.extensions.controllers.novation.slmk3.display.ScreenLayout;
import com.bitwig.extensions.controllers.novation.slmk3.display.SlRgbState;
import com.bitwig.extensions.controllers.novation.slmk3.display.SysExScreenBuilder;


public class CenterScreenPanel extends ScreenPanel implements ScreenConfigSource {
    
    private String leftRow1 = "";
    private String leftRow2 = "";
    private String rightRow1 = "";
    private String rightRow2 = "";
    private SlRgbState leftColorBar = SlRgbState.OFF;
    private SlRgbState rightTopColorBar = SlRgbState.OFF;
    private SlRgbState rightBottomColorBar = SlRgbState.OFF;
    private final MidiProcessor midiProcessor;
    private boolean active;
    
    public CenterScreenPanel(final MidiProcessor processor) {
        super(8);
        this.midiProcessor = processor;
        setConfigParent(this);
    }
    
    @Override
    public ScreenLayout getLayout() {
        return ScreenLayout.EMPTY;
    }
    
    @Override
    public void applySubPanel(final SelectionSubPanel subPanel) {
    
    }
    
    @Override
    public void update(final SysExScreenBuilder builder) {
        builder.appendText(columnIndex, 0, leftRow1);
        builder.appendText(columnIndex, 1, leftRow2);
        builder.appendText(columnIndex, 2, rightRow1);
        builder.appendText(columnIndex, 3, rightRow2);
        builder.appendRgb(columnIndex, 0, leftColorBar);
        builder.appendRgb(columnIndex, 1, rightTopColorBar);
        builder.appendRgb(columnIndex, 2, rightBottomColorBar);
    }
    
    @Override
    public void updateSelectText1(final ButtonMode selectionType, final String value) {
    
    }
    
    @Override
    public void updateSelectText2(final ButtonMode selectionType, final String value) {
    
    }
    
    @Override
    public void updateSelectColor(final ButtonMode selectionType, final SlRgbState color) {
    
    }
    
    @Override
    public void updateSelectSelected(final ButtonMode selectionType, final boolean selected) {
    
    }
    
    public void setLeftRow1(final String leftRow1) {
        if (!Objects.equals(this.leftRow1, leftRow1)) {
            this.leftRow1 = leftRow1;
            updateText(0, leftRow1);
        }
    }
    
    public void setLeftRow2(final String leftRow2) {
        if (!Objects.equals(this.leftRow2, leftRow2)) {
            this.leftRow2 = leftRow2;
            updateText(1, leftRow2);
        }
    }
    
    public void setRightRow1(final String rightRow1) {
        if (!Objects.equals(this.rightRow1, rightRow1)) {
            this.rightRow1 = rightRow1;
            updateText(2, rightRow1);
        }
    }
    
    public void setRightRow2(final String rightRow2) {
        if (!Objects.equals(this.rightRow2, rightRow2)) {
            this.rightRow2 = rightRow2;
            updateText(3, rightRow2);
        }
    }
    
    public void setLeftColorBar(final SlRgbState leftColorBar) {
        if (!Objects.equals(this.leftColorBar, leftColorBar)) {
            this.leftColorBar = leftColorBar;
            updateColor(0, leftColorBar);
        }
    }
    
    public void setRightTopColorBar(final SlRgbState rightTopColorBar) {
        if (!Objects.equals(this.rightTopColorBar, rightTopColorBar)) {
            this.rightTopColorBar = rightTopColorBar;
            updateColor(1, rightTopColorBar);
        }
    }
    
    public void setRightBottomColorBar(final SlRgbState rightBottomColorBar) {
        if (!Objects.equals(this.rightBottomColorBar, rightBottomColorBar)) {
            this.rightBottomColorBar = rightBottomColorBar;
            updateColor(2, rightBottomColorBar);
        }
    }
    
    public void notifyMessage(final String text1, final String text2) {
        midiProcessor.sendNotification(text1, text2);
    }
    
    @Override
    public boolean isActive() {
        return active;
    }
    
    public void setActive(final boolean active) {
        this.active = active;
        if (active) {
            final SysExScreenBuilder sysExScreenBuilder = new SysExScreenBuilder();
            update(sysExScreenBuilder);
            midiProcessor.send(sysExScreenBuilder);
        }
    }
    
    @Override
    public MidiProcessor getMidiProcessor() {
        return this.midiProcessor;
    }
}
