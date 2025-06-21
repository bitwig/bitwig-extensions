package com.bitwig.extensions.controllers.novation.slmk3.value;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import com.bitwig.extensions.controllers.novation.slmk3.display.SlRgbState;

public class ObservableColor implements IObservableValue<SlRgbState> {
    
    private SlRgbState color;
    private SlRgbState dimmedColor;
    protected final List<Consumer<SlRgbState>> listeners = new ArrayList<>();
    
    public ObservableColor() {
        color = SlRgbState.OFF;
        dimmedColor = SlRgbState.OFF;
    }
    
    public ObservableColor(final SlRgbState initColor) {
        color = initColor;
        dimmedColor = initColor.reduced(10);
    }
    
    @Override
    public void set(final SlRgbState value) {
        if (!Objects.equals(this.color, value)) {
            color = value;
            dimmedColor = value.reduced(10);
            listeners.forEach(observer -> observer.accept(color));
        }
    }
    
    public SlRgbState getDimmedColor() {
        return dimmedColor;
    }
    
    @Override
    public String getDisplayString() {
        return null;
    }
    
    @Override
    public void addValueObserver(final Consumer<SlRgbState> observer) {
        listeners.add(observer);
    }
    
    @Override
    public void addDisplayObserver(final Consumer<String> observer) {
    }
    
    @Override
    public SlRgbState get() {
        return color;
    }
}
