package com.bitwig.extensions.controllers.mcu;

import com.bitwig.extension.controller.api.SendBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;

public class TrackBankView {
    private final int[] trackColors;
    
    private final TrackBank trackBank;
    private final int numberOfSends;
    private int itemCount;
    private final boolean isExtended;
    private final GlobalStates globalStates;
    private int selectedIndex;
    private int numberOfSendsOverall = 0;
    private String trackType = "";
    
    public TrackBankView(final TrackBank trackBank, final GlobalStates globalStates, final boolean isExtended,
        final int numberOfSends) {
        this.trackBank = trackBank;
        this.numberOfSends = numberOfSends;
        this.isExtended = isExtended;
        this.globalStates = globalStates;
        trackColors = new int[trackBank.getSizeOfBank()];
        prepareTrackBank();
        trackBank.itemCount().addValueObserver(items -> {
            this.itemCount = items;
            updateSelectedTrackInfo();
        });
        this.globalStates.getGlobalView().addValueObserver(globalView -> updateSelectedTrackInfo());
    }
    
    private void prepareTrackBank() {
        if (numberOfSends > 1) {
            for (int i = 0; i < trackBank.getSizeOfBank(); i++) {
                final Track track = trackBank.getItemAt(i);
                track.sendBank().scrollPosition().markInterested();
                track.sendBank().itemCount().markInterested();
            }
        }
        trackBank.canScrollChannelsDown().markInterested();
        trackBank.canScrollChannelsUp().markInterested();
        for (int i = 0; i < trackBank.getSizeOfBank(); i++) {
            final Track track = trackBank.getItemAt(i);
            configureTrack(track, i);
        }
    }
    
    public void setCursorTrackPosition(final int trackPosition) {
        this.selectedIndex = trackPosition;
        updateSelectedTrackInfo();
    }
    
    public void setTrackType(final String trackType) {
        this.trackType = trackType;
        updateSelectedTrackInfo();
    }
    
    public void setNumberOfSends(final int numberOfSends) {
        this.numberOfSendsOverall = numberOfSends;
        updateSelectedTrackInfo();
    }
    
    private void configureTrack(final Track track, final int index) {
        track.color().addValueObserver((r, g, b) -> trackColors[index] = toColor(r, g, b));
    }
    
    private static int toColor(final double r, final double g, final double b) {
        final int red = (int) (Math.floor(r * 127));
        final int green = (int) (Math.floor(g * 127));
        final int blue = (int) (Math.floor(b * 127));
        return red << 16 | green << 8 | blue;
    }
    
    private void updateSelectedTrackInfo() {
        if (globalStates.getGlobalView().get() != isExtended) {
            return;
        }
        if (trackType.equals("Master")) {
            globalStates.notifySelectedTrackState("MT", isExtended);
        } else if (trackType.equals("Effect")) {
            final int sndPos =
                isExtended ? numberOfSendsOverall - (itemCount - selectedIndex - 1) : selectedIndex - itemCount;
            
            globalStates.notifySelectedTrackState("F%01d".formatted(sndPos + 1), isExtended);
        } else {
            globalStates.notifySelectedTrackState("%02d".formatted(selectedIndex + 1), isExtended);
        }
    }
    
    public TrackBank getTrackBank() {
        return trackBank;
    }
    
    public int[] getColor(final int channelOffset) {
        if (trackColors.length == 8) {
            return trackColors;
        }
        final int[] result = new int[8];
        System.arraycopy(trackColors, 8, result, 0, 8);
        return result;
    }
    
    public void navigateChannels(final int dir) {
        trackBank.scrollBy(dir);
    }
    
    public void navigateToSends(final int index) {
        for (int trackIndex = 0; trackIndex < trackBank.getSizeOfBank(); trackIndex++) {
            final SendBank sendBank = trackBank.getItemAt(trackIndex).sendBank();
            if (index < sendBank.getSizeOfBank()) {
                sendBank.scrollPosition().set(index);
            }
        }
    }
    
    public void navigateSends(final int dir) {
        if (numberOfSends == 1) {
            for (int trackIndex = 0; trackIndex < trackBank.getSizeOfBank(); trackIndex++) {
                final SendBank sendBank = trackBank.getItemAt(trackIndex).sendBank();
                if (dir > 0) {
                    sendBank.scrollForwards();
                } else {
                    sendBank.scrollBackwards();
                }
            }
        } else {
            navigateSendsBank(dir);
        }
    }
    
    private void navigateSendsBank(final int dir) {
        final SendBank firstBank = trackBank.getItemAt(0).sendBank();
        final int index = firstBank.scrollPosition().get();
        final int newIndex = index + dir;
        
        if (newIndex >= 0 && newIndex < firstBank.itemCount().get()) {
            for (int i = 0; i < trackBank.getSizeOfBank(); i++) {
                final SendBank bank = trackBank.getItemAt(i).sendBank();
                bank.scrollPosition().set(newIndex);
            }
        }
    }
    
    
}
