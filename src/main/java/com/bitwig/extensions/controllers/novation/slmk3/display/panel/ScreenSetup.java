package com.bitwig.extensions.controllers.novation.slmk3.display.panel;

import java.util.function.Supplier;

import com.bitwig.extensions.controllers.novation.slmk3.MidiProcessor;
import com.bitwig.extensions.controllers.novation.slmk3.display.ButtonSubPanel;
import com.bitwig.extensions.controllers.novation.slmk3.display.ScreenConfigSource;
import com.bitwig.extensions.controllers.novation.slmk3.display.ScreenLayout;
import com.bitwig.extensions.controllers.novation.slmk3.display.SysExScreenBuilder;

public class ScreenSetup<P extends ScreenPanel> implements ScreenConfigSource {
    
    private final P[] panels;
    private final ScreenLayout layout;
    private boolean active = false;
    private final MidiProcessor midiProcessor;
    private final String name;
    
    public ScreenSetup(final String name, final Supplier<P[]> creator, final ScreenLayout layout,
        final MidiProcessor midiProcessor) {
        this.layout = layout;
        this.name = name;
        this.midiProcessor = midiProcessor;
        this.panels = creator.get();
        for (int i = 0; i < this.panels.length; i++) {
            this.panels[i].setConfigParent(this);
        }
    }
    
    public void setActive(final boolean active) {
        this.active = active;
        if (this.active) {
            midiProcessor.setScreenLayout(this.layout);
            refresh();
        }
    }
    
    public void applySubPanels(final ButtonSubPanel subPanels) {
        for (int i = 0; i < 8; i++) {
            panels[i].applySubPanel(subPanels.get(i));
        }
    }
    
    @Override
    public boolean isActive() {
        return active;
    }
    
    @Override
    public MidiProcessor getMidiProcessor() {
        return midiProcessor;
    }
    
    public P getPanel(final int index) {
        return panels[index];
    }
    
    public void refresh() {
        final SysExScreenBuilder builder = new SysExScreenBuilder();
        for (int i = 0; i < this.panels.length; i++) {
            this.panels[i].update(builder);
        }
        this.midiProcessor.send(builder);
        
        final SysExScreenBuilder builderForSelectionPart = new SysExScreenBuilder();
        for (int i = 0; i < this.panels.length; i++) {
            this.panels[i].updateSelection(builderForSelectionPart);
        }
        if (builderForSelectionPart.hasData()) {
            this.midiProcessor.send(builderForSelectionPart);
        }
    }
    
    public String getName() {
        return name;
    }
}
