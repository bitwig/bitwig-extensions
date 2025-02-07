package com.bitwig.extensions.controllers.novation.slmk3.display;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.SettableStringValue;
import com.bitwig.extensions.controllers.novation.slmk3.GlobalStates;
import com.bitwig.extensions.controllers.novation.slmk3.MidiProcessor;
import com.bitwig.extensions.controllers.novation.slmk3.ViewControl;
import com.bitwig.extensions.controllers.novation.slmk3.display.panel.BoxPanel;
import com.bitwig.extensions.controllers.novation.slmk3.display.panel.CenterScreenPanel;
import com.bitwig.extensions.controllers.novation.slmk3.display.panel.KnobPanel;
import com.bitwig.extensions.controllers.novation.slmk3.display.panel.ScreenPanel;
import com.bitwig.extensions.controllers.novation.slmk3.display.panel.ScreenSetup;
import com.bitwig.extensions.controllers.novation.slmk3.layer.GridMode;
import com.bitwig.extensions.controllers.novation.slmk3.layer.SoftButtonMode;
import com.bitwig.extensions.controllers.novation.slmk3.value.ObservableValue;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.BasicStringValue;

@Component
public class ScreenHandler {
    
    private final CenterScreenPanel centerScreen;
    private final ScreenSetup<BoxPanel> optionDeviceScreen;
    private final ScreenSetup<BoxPanel> optionShiftScreen;
    private final ScreenSetup<BoxPanel> sequencerScreen;
    
    private final Map<KnobMode, ScreenSetup<KnobPanel>> knobScreens = new HashMap<>();
    
    private ScreenSetup<? extends ScreenPanel> currentPanel;
    private KnobMode knobMode = KnobMode.DEVICE;
    private final Map<ButtonMode, ButtonSubPanel> buttonPanels = new HashMap<>();
    
    private SlRgbState cursorTrackColor;
    private final SettableStringValue remotesPageName = new BasicStringValue("");
    private String[] remotePages;
    private final SettableStringValue trackRemotesPageName = new BasicStringValue("");
    private String[] trackRemotePages;
    private final SettableStringValue projectRemotesPageName = new BasicStringValue("");
    private String[] projectRemotePages;
    
    private final SettableStringValue drumSendsName = new BasicStringValue();
    
    private String deviceName = "";
    private String currentSendsName = "";
    private final ObservableValue<ButtonMode> buttonMode;
    private final ObservableValue<SequencerButtonSubMode> seqSubMode;
    
    public ScreenHandler(final MidiProcessor midiProcessor, final ViewControl viewControl,
        final GlobalStates globalStates) {
        Arrays.stream(ButtonMode.values()).forEach(mode -> buttonPanels.put(mode, new ButtonSubPanel(mode, this)));
        this.buttonMode = globalStates.getButtonMode();
        this.buttonMode.addValueObserver(mode -> applyButtonLayer(mode));
        this.seqSubMode = globalStates.getSequencerSubMode();
        this.seqSubMode.addValueObserver(subMode -> applySubMode(subMode));
        optionDeviceScreen = new ScreenSetup<>("OPTION", this::createBoxPanels, ScreenLayout.BOX, midiProcessor);
        sequencerScreen = new ScreenSetup<>("SEQUENCER", this::createBoxPanels, ScreenLayout.BOX, midiProcessor);
        optionShiftScreen = new ScreenSetup<>("OPTION_SHIFT", this::createBoxPanels, ScreenLayout.BOX, midiProcessor);
        
        Arrays.stream(KnobMode.values()).forEach(mode -> knobScreens.put(mode,
            new ScreenSetup<>(mode.toString(), this::createKnobPanels, ScreenLayout.KNOB, midiProcessor)));
        
        currentPanel = knobScreens.get(KnobMode.DEVICE);
        
        centerScreen = new CenterScreenPanel(midiProcessor);
        setUpCenterScreenControl(viewControl);
    }
    
    public void notifyMessage(final String text1, final String text2) {
        centerScreen.notifyMessage(text1, text2);
    }
    
    private void setUpCenterScreenControl(final ViewControl viewControl) {
        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        cursorTrack.color().addValueObserver((r, g, b) -> {
            cursorTrackColor = SlRgbState.get(r, g, b);
            centerScreen.setLeftColorBar(cursorTrackColor);
        });
        final CursorRemoteControlsPage remotes = viewControl.getPrimaryRemotes();
        remotesPageName.addValueObserver(pageName -> updateCenterScreen());
        trackRemotesPageName.addValueObserver(pageName -> updateCenterScreen());
        projectRemotesPageName.addValueObserver(pageName -> updateCenterScreen());
        drumSendsName.addValueObserver(name -> {
            if (knobMode == KnobMode.DRUM_SENDS) {
                updateCenterScreen();
            }
        });
        remotes.pageNames().addValueObserver(pages -> {
            this.remotePages = pages;
            final int pageIndex = remotes.selectedPageIndex().get();
            if (pageIndex != -1 && this.remotePages != null && pageIndex < this.remotePages.length) {
                remotesPageName.set(this.remotePages[pageIndex]);
            } else {
                remotesPageName.set("");
            }
        });
        remotes.selectedPageIndex().addValueObserver(pageIndex -> {
            if (pageIndex != -1 && this.remotePages != null && pageIndex < this.remotePages.length) {
                remotesPageName.set(this.remotePages[pageIndex]);
            } else {
                remotesPageName.set("");
            }
        });
        
        final CursorRemoteControlsPage trackRemotes = viewControl.getTrackRemotes();
        trackRemotes.pageNames().addValueObserver(pages -> {
            this.trackRemotePages = pages;
            final int pageIndex = trackRemotes.selectedPageIndex().get();
            if (pageIndex != -1 && this.trackRemotePages != null && pageIndex < this.trackRemotePages.length) {
                trackRemotesPageName.set(this.trackRemotePages[pageIndex]);
            } else {
                trackRemotesPageName.set("");
            }
        });
        trackRemotes.selectedPageIndex().addValueObserver(pageIndex -> {
            if (pageIndex != -1 && this.trackRemotePages != null && pageIndex < this.trackRemotePages.length) {
                trackRemotesPageName.set(this.trackRemotePages[pageIndex]);
            } else {
                trackRemotesPageName.set("");
            }
        });
        
        final CursorRemoteControlsPage projectRemotes = viewControl.getProjectRemotes();
        projectRemotes.pageNames().addValueObserver(pages -> {
            this.projectRemotePages = pages;
            final int pageIndex = trackRemotes.selectedPageIndex().get();
            if (pageIndex != -1 && this.projectRemotePages != null && pageIndex < this.projectRemotePages.length) {
                projectRemotesPageName.set(this.projectRemotePages[pageIndex]);
            } else {
                projectRemotesPageName.set("");
            }
        });
        projectRemotes.selectedPageIndex().addValueObserver(pageIndex -> {
            if (pageIndex != -1 && this.projectRemotePages != null && pageIndex < this.projectRemotePages.length) {
                projectRemotesPageName.set(this.projectRemotePages[pageIndex]);
            } else {
                projectRemotesPageName.set("");
            }
        });
        
        viewControl.getDeviceDescriptor().addValueObserver(name -> {
            deviceName = name;
            updateCenterScreen();
        });
    }
    
    public void setCurrentSendsName(final String currentSendsName) {
        this.currentSendsName = currentSendsName;
        updateCenterScreen();
    }
    
    public SettableStringValue getDrumSendsName() {
        return drumSendsName;
    }
    
    private void updateCenterScreen() {
        switch (knobMode) {
            case DEVICE -> {
                centerScreen.setLeftRow1(deviceName);
                centerScreen.setLeftRow2(remotesPageName.get());
            }
            case PAN -> {
                centerScreen.setLeftRow1("Mixer");
                centerScreen.setLeftRow2("Pan");
            }
            case SEND -> {
                centerScreen.setLeftRow1("Mixer");
                centerScreen.setLeftRow2(currentSendsName);
            }
            case TRACK -> {
                centerScreen.setLeftRow1("Tr.Remote");
                centerScreen.setLeftRow2(trackRemotesPageName.get());
            }
            case PROJECT -> {
                centerScreen.setLeftRow1("Pr.Remote");
                centerScreen.setLeftRow2(projectRemotesPageName.get());
            }
            case DRUM_VOLUME -> {
                centerScreen.setLeftRow1("Drum Vol");
                centerScreen.setLeftRow2("");
            }
            case DRUM_PAN -> {
                centerScreen.setLeftRow1("Drum Pan");
                centerScreen.setLeftRow2("");
            }
            case DRUM_SENDS -> {
                centerScreen.setLeftRow1("Drum Send");
                centerScreen.setLeftRow2(drumSendsName.get());
            }
        }
    }
    
    public ScreenSetup<KnobPanel> getScreen(final KnobMode mode) {
        return knobScreens.get(mode);
    }
    
    public ButtonSubPanel getSubPanel(final ButtonMode mode) {
        return buttonPanels.get(mode);
    }
    
    @Activate
    public void activate() {
        applyButtonLayer(buttonMode.get());
        currentPanel.setActive(true);
        centerScreen.setActive(true);
    }
    
    private KnobPanel[] createKnobPanels() {
        final KnobPanel[] panels = new KnobPanel[8];
        for (int i = 0; i < panels.length; i++) {
            panels[i] = new KnobPanel(i);
        }
        return panels;
    }
    
    private BoxPanel[] createBoxPanels() {
        final BoxPanel[] panels = new BoxPanel[8];
        for (int i = 0; i < panels.length; i++) {
            panels[i] = new BoxPanel(i);
        }
        return panels;
    }
    
    public ScreenSetup<BoxPanel> getOptionDeviceScreen() {
        return optionDeviceScreen;
    }
    
    public ScreenSetup<BoxPanel> getSequencerScreen() {
        return sequencerScreen;
    }
    
    public ScreenSetup<BoxPanel> getOptionShiftScreen() {
        return optionShiftScreen;
    }
    
    private void applySubMode(final SequencerButtonSubMode subMode) {
        if (this.buttonMode.get() == ButtonMode.SEQUENCER2 || this.buttonMode.get() == ButtonMode.SEQUENCER) {
            this.buttonMode.set(
                subMode == SequencerButtonSubMode.MODE_1 ? ButtonMode.SEQUENCER : ButtonMode.SEQUENCER2);
        }
    }
    
    public void setMode(final KnobMode knobMode, final GridMode gridMode, final boolean shiftActive) {
        this.knobMode = knobMode;
        if (shiftActive) {
            this.buttonMode.set(ButtonMode.SHIFT);
        } else if (gridMode == GridMode.OPTION) {
            this.buttonMode.set(ButtonMode.OPTION);
        } else if (gridMode == GridMode.SEQUENCER || gridMode == GridMode.SELECT) {
            this.buttonMode.set(
                this.seqSubMode.get() == SequencerButtonSubMode.MODE_1 ? ButtonMode.SEQUENCER : ButtonMode.SEQUENCER2);
        } else {
            this.buttonMode.set(ButtonMode.TRACK);
        }
        
        if (gridMode == GridMode.OPTION) {
            if (Objects.requireNonNull(knobMode) == KnobMode.DEVICE) {
                selectCurrentScreen(optionDeviceScreen);
            } else {
                selectCurrentScreen(getScreen(knobMode));
            }
        } else if (gridMode == GridMode.OPTION_SHIFT) {
            selectCurrentScreen(optionShiftScreen);
        } else if (gridMode == GridMode.SEQUENCER || gridMode == GridMode.SELECT) {
            selectCurrentScreen(sequencerScreen);
        } else {
            selectCurrentScreen(getScreen(knobMode));
        }
    }
    
    private void selectCurrentScreen(final ScreenSetup<? extends ScreenPanel> screenPanel) {
        if (this.currentPanel == screenPanel) {
            return;
        }
        this.currentPanel.setActive(false);
        this.currentPanel = screenPanel;
        applyButtonLayer(this.buttonMode.get());
        this.currentPanel.setActive(true);
        updateCenterScreen();
    }
    
    private void applyButtonLayer(final ButtonMode mode) {
        this.currentPanel.applySubPanels(buttonPanels.get(mode));
    }
    
    public void updateSelectText1(final int colIndex, final ButtonMode selectionType, final String value) {
        if (selectionType != this.buttonMode.get()) {
            return;
        }
        this.currentPanel.getPanel(colIndex).updateSelectText1(selectionType, value);
    }
    
    public void updateSelectText2(final int colIndex, final ButtonMode selectionType, final String value) {
        if (selectionType != this.buttonMode.get()) {
            return;
        }
        this.currentPanel.getPanel(colIndex).updateSelectText2(selectionType, value);
    }
    
    public void updateSelectColor(final int colIndex, final ButtonMode selectionType, final SlRgbState color) {
        if (selectionType != this.buttonMode.get()) {
            return;
        }
        this.currentPanel.getPanel(colIndex).updateSelectColor(selectionType, color);
    }
    
    public void updateSelectSelected(final int colIndex, final ButtonMode selectionType, final boolean selected) {
        if (selectionType != this.buttonMode.get()) {
            return;
        }
        this.currentPanel.getPanel(colIndex).updateSelectSelected(selectionType, selected);
    }
    
    public void setSoftMode(final SoftButtonMode mode) {
        centerScreen.setRightRow1(mode.getItem1());
        centerScreen.setRightRow2(mode.getItem2());
        centerScreen.setRightTopColorBar(mode.getColor1());
        centerScreen.setRightBottomColorBar(mode.getColor2());
    }
    
}
