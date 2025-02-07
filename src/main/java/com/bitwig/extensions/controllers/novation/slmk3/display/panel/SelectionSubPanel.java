package com.bitwig.extensions.controllers.novation.slmk3.display.panel;

import java.util.Objects;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.novation.slmk3.display.ButtonMode;
import com.bitwig.extensions.controllers.novation.slmk3.display.ScreenHandler;
import com.bitwig.extensions.controllers.novation.slmk3.display.SlRgbState;
import com.bitwig.extensions.framework.values.ValueObject;

public class SelectionSubPanel {
    private String row1 = "";
    private String row2 = "";
    private boolean selected = false;
    private SlRgbState color = SlRgbState.OFF;
    private final int columnIndex;
    private final ScreenHandler handler;
    private final ButtonMode selectionType;
    
    public SelectionSubPanel(final int columnIndex, final ButtonMode selectionType, final ScreenHandler handler) {
        this.columnIndex = columnIndex;
        this.handler = handler;
        this.selectionType = selectionType;
    }
    
    public void setRow1(final String value) {
        final String setValue = value == null ? "" : value;
        if (!row1.equals(setValue)) {
            row1 = setValue;
            handler.updateSelectText1(this.columnIndex, selectionType, value);
        }
    }
    
    public void setRow2(final String value) {
        final String setValue = value == null ? "" : value;
        if (!row2.equals(setValue)) {
            row2 = setValue;
            handler.updateSelectText2(this.columnIndex, selectionType, value);
        }
    }
    
    public void setSelected(final boolean selected) {
        if (this.selected != selected) {
            this.selected = selected;
            handler.updateSelectSelected(this.columnIndex, selectionType, selected);
        }
    }
    
    public void setColor(final SlRgbState bottomBarColor) {
        if (!Objects.equals(this.color, bottomBarColor)) {
            this.color = bottomBarColor;
            handler.updateSelectColor(this.columnIndex, selectionType, color);
        }
    }
    
    public String getRow1() {
        return row1;
    }
    
    public String getRow2() {
        return row2;
    }
    
    public boolean isSelected() {
        return selected;
    }
    
    public SlRgbState getColor() {
        return color;
    }
    
    public void bindVariable(final StringValue row1, final StringValue row2, final BooleanValue selected,
        final ValueObject<SlRgbState> color) {
        setRow1(row1.get());
        setRow2(row2.get());
        selected.markInterested();
        setSelected(selected.get());
        setColor(selected.get() ? color.get() : color.get().reduced(20));
        
        row1.addValueObserver(newValue -> setRow1(newValue));
        row2.addValueObserver(newValue -> setRow2(newValue));
        color.addValueObserver((newValue -> {
            setColor(selected.get() ? newValue : newValue.reduced(20));
        }));
        selected.addValueObserver(enabled -> {
            setColor(enabled ? color.get() : color.get().reduced(20));
            setSelected(enabled);
        });
    }
    
    public void bindValueWithSelect(final String row1, final String row2, final BooleanValue value,
        final SlRgbState onColor, final SlRgbState offColor) {
        setRow1(row1);
        setRow2(row2);
        value.markInterested();
        setSelected(value.get());
        setColor(value.get() ? onColor : offColor);
        value.addValueObserver(enabled -> {
            setColor(enabled ? onColor : offColor);
            setSelected(enabled);
        });
    }
    
    public void bindValue(final String row1, final String row2, final BooleanValue value, final SlRgbState onColor,
        final SlRgbState offColor) {
        setRow1(row1);
        setRow2(row2);
        value.markInterested();
        setColor(value.get() ? onColor : offColor);
        value.addValueObserver(enabled -> {
            setColor(enabled ? onColor : offColor);
        });
    }
}
