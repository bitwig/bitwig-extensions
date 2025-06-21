package com.bitwig.extensions.controllers.novation.launchkey_mk4;

import com.bitwig.extension.controller.api.Application;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.bitwig.extensions.framework.values.LayoutType;
import com.bitwig.extensions.framework.values.ValueObject;

@Component
public class GlobalStates {
    private RgbState trackColor;
    private RgbState trackColorOff;
    private final BooleanValueObject shiftState = new BooleanValueObject();
    private final PadSlot[] padSlots = new PadSlot[16];
    private final ValueObject<LayoutType> panelLayout = new ValueObject<>(LayoutType.ARRANGER);
    
    public GlobalStates(final Application application, final ViewControl viewControl) {
        application.panelLayout().addValueObserver(layout -> {
            this.panelLayout.set(LayoutType.toType(layout));
        });
        viewControl.getCursorTrack().color().addValueObserver((r, g, b) -> {
            trackColor = RgbState.get(r, g, b);
            trackColorOff = RgbState.get(r, g, b).dim();
        });
    }
    
    public void setPadSlot(final int index, final PadSlot padSlot) {
        this.padSlots[index] = padSlot;
    }
    
    public PadSlot getPadSlot(final int index) {
        return padSlots[index];
    }
    
    public RgbState getPadColor(final int index, final boolean active) {
        final RgbState state;
        if (padSlots[index].getColor() == RgbState.OFF) {
            state = getTrackColor(active);
        } else {
            state = active ? padSlots[index].getColor() : padSlots[index].getColorOff();
        }
        if (padSlots[index].isSelected()) {
            return active ? state : RgbState.WHITE;
        }
        return state;
    }
    
    public BooleanValueObject getShiftState() {
        return shiftState;
    }
    
    public ValueObject<LayoutType> getPanelLayout() {
        return panelLayout;
    }
    
    public RgbState getTrackColor(final boolean active) {
        return active ? trackColor : trackColorOff;
    }
    
    public RgbState getTrackColor() {
        return trackColor;
    }
    
    public RgbState getTrackColorOff() {
        return trackColorOff;
    }
}
