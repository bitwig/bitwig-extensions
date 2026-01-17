package com.bitwig.extensions.controllers.akai.mpkmk4.display;

import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.StringValue;

public class MenuEntry {
    private final int index;
    private final String title;
    private final StringValue value;
    private IntConsumer incrementHandler;
    private Runnable clickHandler;
    
    public MenuEntry(final int index, final String title, final StringValue value, final IntConsumer incrementHandler) {
        this.index = index;
        this.title = title;
        this.value = value;
        this.incrementHandler = incrementHandler;
    }
    
    public MenuEntry(final int index, final String title, final Runnable clickHandler) {
        this.index = index;
        this.title = title;
        this.value = null;
        this.clickHandler = clickHandler;
    }
    
    public int getIndex() {
        return index;
    }
    
    public String getTitle() {
        return title;
    }
    
    public StringValue getValue() {
        return value;
    }
    
    public IntConsumer getIncrementHandler() {
        return incrementHandler;
    }
    
    public Runnable getClickHandler() {
        return clickHandler;
    }
}
