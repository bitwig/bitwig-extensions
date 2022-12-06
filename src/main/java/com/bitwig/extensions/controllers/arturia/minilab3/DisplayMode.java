package com.bitwig.extensions.controllers.arturia.minilab3;

enum DisplayMode {
    TRACK("CHANNEL RACK"),
    PARAM,
    PARAM_PAGE,
    SCENE,
    BROWSER("BROWSER"),
    BROWSER_INFO,
    LOOP_VALUE,
    STATE_INFO,
    INIT,
    TEMPO;

    private final String text;

    DisplayMode() {
        text = name();
    }

    DisplayMode(final String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
