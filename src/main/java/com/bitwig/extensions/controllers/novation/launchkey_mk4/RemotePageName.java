package com.bitwig.extensions.controllers.novation.launchkey_mk4;

import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.StringValue;

public class RemotePageName {
    private String[] pages;
    private int currentIndex = 0;
    private final StringValue title;
    
    public RemotePageName(final CursorRemoteControlsPage remotes, final StringValue title) {
        remotes.pageNames().addValueObserver(pages -> this.pages = pages);
        remotes.selectedPageIndex().addValueObserver(pageIndex -> {
            this.currentIndex = pageIndex;
        });
        this.title = title;
        this.title.markInterested();
    }
    
    public String get(final int offset) {
        final int index = currentIndex + offset;
        if (index >= 0 && index < pages.length) {
            return pages[index];
        }
        if (currentIndex >= 0 && currentIndex < pages.length) {
            return pages[currentIndex];
        }
        return "";
    }
    
    public String getTitle() {
        return title.get();
    }
}
