package com.bitwig.extensions.controllers.akai.mpkmk4.display;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.StringValue;

public class MenuList {
    private static final Color SELECT_BG = Color.fromRGB255(0 << 3, 10 << 2, 31 << 3);
    private static final Color SELECT_BG_PARAM = Color.fromRGB255(0 << 3, 0, 0x15 << 3);
    private static final Color SELECT_FG = Color.fromRGB255(255, 255, 255);
    public static final Color BACKGROUND = Color.fromRGB255(255, 255, 220);
    public static final Color FOREGROUND = Color.fromRGB255(0, 0, 0);
    
    private final List<MenuEntry> entries = new ArrayList<>();
    private int scrollPos = 0;
    private boolean valueFocus = false;
    
    public void add(final String title, final Runnable clickHandler) {
        this.entries.add(new MenuEntry(entries.size(), title, clickHandler));
    }
    
    public void add(final String title, final StringValue value, final IntConsumer incrementHandler) {
        this.entries.add(new MenuEntry(entries.size(), title, value, incrementHandler));
    }
    
    public MenuEntry get(final int index) {
        return entries.get(index);
    }
    
    public boolean increment(final int inc) {
        final int nextEntry = scrollPos + inc;
        if (nextEntry >= 0 && nextEntry < entries.size()) {
            scrollPos = nextEntry;
            return true;
        }
        return false;
    }
    
    public MenuEntry getCurrent() {
        return entries.get(scrollPos);
    }
    
    public void updateDisplay(final LineDisplay display) {
        final int scrollOffset = Math.max(0, scrollPos - 2);
        for (int i = 0; i < 3; i++) {
            final int index = i + scrollOffset;
            if (index < entries.size()) {
                updateMenuEntry(entries.get(index), display);
            }
        }
    }
    
    public void updateMenuEntry(final MenuEntry entry, final LineDisplay menuDisplay) {
        final int i = entry.getIndex() - Math.max(0, scrollPos - 2);
        final String value = entry.getValue() != null ? entry.getValue().get() : "";
        if (entry.getIndex() == scrollPos) {
            menuDisplay.setMenuLine(i, MpkDisplayFont.PT24, 0, SELECT_FG, valueFocus ? SELECT_BG_PARAM : SELECT_BG);
            if (entry.getValue() == null) {
                menuDisplay.setText(i, "%s".formatted(entry.getTitle()));
            } else {
                menuDisplay.setText(i, "%s%s: %s".formatted(valueFocus ? ">" : "", entry.getTitle(), value));
            }
        } else {
            menuDisplay.setMenuLine(i, MpkDisplayFont.PT24, 0, FOREGROUND, BACKGROUND);
            if (entry.getTitle() == null) {
                menuDisplay.setText(i, "%s".formatted(entry.getTitle()));
            } else {
                menuDisplay.setText(i, "%s: %s".formatted(entry.getTitle(), value));
            }
        }
    }
    
    public void toggleValueFocus() {
        this.valueFocus = !valueFocus;
    }
    
    public void resetValueFocus() {
        this.valueFocus = false;
    }
    
    public boolean onValue() {
        return valueFocus;
    }
}
