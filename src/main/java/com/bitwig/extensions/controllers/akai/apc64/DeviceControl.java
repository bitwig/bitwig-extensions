package com.bitwig.extensions.controllers.akai.apc64;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.values.BasicStringValue;
import com.bitwig.extensions.framework.values.IntValueObject;

import java.util.function.Consumer;

public class DeviceControl {
    private final CursorRemoteControlsPage deviceRemotePages;
    private final CursorRemoteControlsPage trackRemotes;
    private final CursorRemoteControlsPage projectRemotes;
    private final PinnableCursorDevice cursorDevice;
    private final PinnableCursorDevice primaryDevice;
    private final DrumPadBank drumPadBank;
    private Focus currentFocus = Focus.DEVICE;
    private final BasicStringValue deviceName = new BasicStringValue("");
    private final BasicStringValue pageName = new BasicStringValue("");
    private String[] devicePageNames = new String[]{};
    private String deviceRawName = "";
    private int devicePageIndex = 0;
    private String[] trackRemotePageNames = new String[]{};
    private int trackRemotePageIndex = 0;
    private String[] projectRemotePageNames = new String[]{};
    private int projectRemotePageIndex = 0;
    private Consumer<Focus> focusListener = null;
    private String padRawName = "";
    private final IntValueObject selectedPadIndex = new IntValueObject(-1, -1, 15);

    public enum Focus {
        DEVICE,
        TRACK,
        PROJECT
    }

    public DeviceControl(final CursorTrack cursorTrack, final Track rootTrack) {
        cursorDevice = cursorTrack.createCursorDevice();
        cursorDevice.hasDrumPads().markInterested();
        cursorDevice.name().addValueObserver(name -> {
            deviceRawName = name.isBlank() ? "<No Device>" : name;
            if (currentFocus == Focus.DEVICE) {
                deviceName.set(deviceRawName);
            }
        });
        cursorDevice.hasNext().markInterested();
        cursorDevice.hasPrevious().markInterested();
        cursorDevice.hasLayers().markInterested();
        cursorDevice.hasSlots().markInterested();
        cursorDevice.slotNames().markInterested();

        deviceRemotePages = cursorDevice.createCursorRemoteControlsPage(8);
        deviceRemotePages.pageNames().addValueObserver(names -> {
            devicePageNames = names;
            applyCurrentValues(Focus.DEVICE);
        });
        deviceRemotePages.selectedPageIndex().addValueObserver(index -> {
            devicePageIndex = index;
            applyCurrentValues(Focus.DEVICE);
        });
        deviceRemotePages.setHardwareLayout(HardwareControlType.SLIDER, 8);
        primaryDevice = cursorTrack.createCursorDevice("drumdetection", "Pad Device", 8,
                CursorDeviceFollowMode.FIRST_INSTRUMENT);
        primaryDevice.hasDrumPads().markInterested();
        primaryDevice.exists().markInterested();

        trackRemotes = cursorTrack.createCursorRemoteControlsPage("track-remotes", 8, null);
        trackRemotes.setHardwareLayout(HardwareControlType.SLIDER, 8);
        trackRemotes.pageNames().addValueObserver(names -> {
            trackRemotePageNames = names;
            applyCurrentValues(Focus.TRACK);
        });
        trackRemotes.selectedPageIndex().addValueObserver(index -> {
            trackRemotePageIndex = index;
            applyCurrentValues(Focus.TRACK);
        });

        projectRemotes = rootTrack.createCursorRemoteControlsPage("project-remotes", 8, null);
        projectRemotes.setHardwareLayout(HardwareControlType.SLIDER, 8);
        projectRemotes.pageNames().addValueObserver(names -> {
            projectRemotePageNames = names;
            applyCurrentValues(Focus.PROJECT);
        });
        projectRemotes.selectedPageIndex().addValueObserver(index -> {
            projectRemotePageIndex = index;
            applyCurrentValues(Focus.PROJECT);
        });
        drumPadBank = primaryDevice.createDrumPadBank(16);
        for (int i = 0; i < 16; i++) {
            final int index = i;
            final DrumPad pad = drumPadBank.getItemAt(i);
            pad.name().addValueObserver(name -> handlePadNameChanged(index, name));
            pad.addIsSelectedInEditorObserver(selected -> handlePadSelection(selected, index, pad));
        }

        initRemotesPage(deviceRemotePages);
        initRemotesPage(trackRemotes);
        initRemotesPage(projectRemotes);
    }

    private void handlePadNameChanged(final int index, final String name) {
        if (index == selectedPadIndex.get()) {
            padRawName = name;
            if (cursorDevice.hasDrumPads().get()) {
                pageName.set(padRawName);
            }
        }
    }

    private void handlePadSelection(final boolean selected, final int index, final DrumPad pad) {
        if (selected) {
            selectedPadIndex.set(index);
            padRawName = pad.name().get();
            if (cursorDevice.hasDrumPads().get()) {
                pageName.set(padRawName);
            }
        }
    }

    public void setCurrentFocus(final Focus focus) {
        if (this.currentFocus != focus) {
            this.currentFocus = focus;
            applyCurrentValues(focus);
            if (this.focusListener != null) {
                this.focusListener.accept(this.currentFocus);
            }
        }
    }

    public PinnableCursorDevice getPrimaryDevice() {
        return primaryDevice;
    }

    public PinnableCursorDevice getCursorDevice() {
        return cursorDevice;
    }

    public void setFocusListener(final Consumer<Focus> focusListener) {
        this.focusListener = focusListener;
    }

    private void applyCurrentValues(final Focus focus) {
        if (focus != this.currentFocus) {
            return;
        }
        if (this.currentFocus == Focus.DEVICE) {
            deviceName.set(deviceRawName);
            if (devicePageIndex >= 0 && devicePageIndex < devicePageNames.length) {
                pageName.set(devicePageNames[devicePageIndex]);
            } else {
                pageName.set("<No Remotes>");
            }
        } else if (this.currentFocus == Focus.TRACK) {
            deviceName.set("Track Remotes");
            if (trackRemotePageIndex >= 0 && trackRemotePageIndex < trackRemotePageNames.length) {
                pageName.set(trackRemotePageNames[trackRemotePageIndex]);
            } else {
                pageName.set("<No Remotes>");
            }
        } else if (this.currentFocus == Focus.PROJECT) {
            deviceName.set("Project Remotes");
            if (projectRemotePageIndex >= 0 && projectRemotePageIndex < projectRemotePageNames.length) {
                pageName.set(projectRemotePageNames[projectRemotePageIndex]);
            } else {
                pageName.set("<No Remotes>");
            }
        }
        if (cursorDevice.hasDrumPads().get()) {
            pageName.set(padRawName);
        }
    }

    public BasicStringValue getDeviceName() {
        return deviceName;
    }

    public BasicStringValue getPageName() {
        return pageName;
    }

    public void selectDevice(final int dir) {
        switch (currentFocus) {
            case DEVICE -> navigateDevice(dir);
            case TRACK -> navigateTrack(dir);
            case PROJECT -> navigateProject(dir);
        }
    }

    private void navigateTrack(final int dir) {
        if (dir > 0) {
            setCurrentFocus(Focus.DEVICE);
        } else {
            setCurrentFocus(Focus.PROJECT);
        }
    }

    private void navigateProject(final int dir) {
        if (dir > 0) {
            setCurrentFocus(Focus.TRACK);
        }
    }

    public void navigateDevice(final int dir) {
        if (dir > 0) {
            cursorDevice.selectNext();
        } else if (cursorDevice.hasPrevious().get()) {
            cursorDevice.selectPrevious();
        } else {
            setCurrentFocus(Focus.TRACK);
        }
    }

    public boolean canScrollDevices(final int dir) {
        return switch (currentFocus) {
            case DEVICE -> dir <= 0 || cursorDevice.hasNext().get();
            case TRACK -> true;
            case PROJECT -> dir > 0;
        };
    }

    public void selectParameterPage(final int dir) {
        if (dir > 0) {
            getCurrentPage().selectNext();
        } else {
            getCurrentPage().selectPrevious();
        }
    }

    public CursorRemoteControlsPage getCurrentPage() {
        return getPage(currentFocus);
    }

    public CursorRemoteControlsPage getPage(final Focus focus) {
        return switch (focus) {
            case TRACK -> trackRemotes;
            case DEVICE -> deviceRemotePages;
            case PROJECT -> projectRemotes;
        };
    }

    public DrumPadBank getDrumPadBank() {
        return drumPadBank;
    }

    public boolean canScrollParameterPages(final int dir) {
        if (dir > 0) {
            return getCurrentPage().hasNext().get();
        }
        return getCurrentPage().hasPrevious().get();
    }

    public boolean canNavigateIntoDevice(final int dir) {
        if (dir > 0) {
            return cursorDevice.hasLayers().get() || cursorDevice.hasDrumPads().get() || cursorDevice.hasSlots().get();
        }
        return true;
    }

    public void navigateVertical(final int dir) {
        if (dir > 0) {
            if (cursorDevice.hasDrumPads().get()) {
                cursorDevice.selectFirstInKeyPad(36); // to do get from pad
            } else if (cursorDevice.hasLayers().get()) {
                cursorDevice.selectFirstInLayer(0);
            } else if (cursorDevice.hasSlots().get()) {
                final String[] slotNames = cursorDevice.slotNames().get();
                cursorDevice.selectFirstInSlot(slotNames[0]);
            }
        } else {
            cursorDevice.selectParent();
        }
    }

    private void initRemotesPage(final CursorRemoteControlsPage remotesPage) {
        remotesPage.hasPrevious().markInterested();
        remotesPage.hasNext().markInterested();
    }
}
