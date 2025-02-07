package com.bitwig.extensions.controllers.novation.slmk3.display;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extensions.controllers.novation.slmk3.display.panel.SelectionSubPanel;

public class ButtonSubPanel {
    private final List<SelectionSubPanel> panels = new ArrayList<>();
    
    public ButtonSubPanel(final ButtonMode buttonMode, final ScreenHandler handler) {
        for (int i = 0; i < 8; i++) {
            panels.add(new SelectionSubPanel(i, buttonMode, handler));
        }
    }
    
    public SelectionSubPanel get(final int index) {
        return panels.get(index);
    }
}

