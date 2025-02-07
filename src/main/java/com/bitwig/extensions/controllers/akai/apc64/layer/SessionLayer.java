package com.bitwig.extensions.controllers.akai.apc64.layer;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.akai.apc.common.AbstractSessionLayer;
import com.bitwig.extensions.controllers.akai.apc.common.PanelLayout;
import com.bitwig.extensions.controllers.akai.apc.common.control.RgbButton;
import com.bitwig.extensions.controllers.akai.apc64.Apc64MidiProcessor;
import com.bitwig.extensions.controllers.akai.apc64.ApcPreferences;
import com.bitwig.extensions.controllers.akai.apc64.FocusClip;
import com.bitwig.extensions.controllers.akai.apc64.HardwareElements;
import com.bitwig.extensions.controllers.akai.apc64.ModifierStates;
import com.bitwig.extensions.controllers.akai.apc64.ViewControl;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Inject;
import com.bitwig.extensions.framework.di.PostConstruct;

public class SessionLayer extends AbstractSessionLayer {
    
    private final ControllerHost host;
    private final ApcPreferences preferences;
    @Inject
    private ViewControl viewControl;
    @Inject
    private Transport transport;
    @Inject
    private ModifierStates modifiers;
    @Inject
    private Apc64MidiProcessor midiProcessor;
    @Inject
    private FocusClip focusClip;
    
    private final Layer horizontalLayer;
    private final Layer verticalLayer;
    private TrackBank trackBank;
    private PanelLayout panelLayout;
    
    public SessionLayer(final Layers layers, final ControllerHost host, final ApcPreferences preferences) {
        super(layers);
        this.horizontalLayer = new Layer(layers, "HORIZONTAL_LAYER");
        this.verticalLayer = new Layer(layers, "VERTICAL_LAYER");
        this.host = host;
        this.preferences = preferences;
        
        panelLayout = this.preferences.getPanelLayout().get();
        this.preferences.getPanelLayout().addValueObserver(this::handlePanelLayoutChange);
    }
    
    private void handlePanelLayoutChange(final PanelLayout layoutNew) {
        panelLayout = layoutNew;
        if (isActive()) {
            horizontalLayer.setIsActive(panelLayout == PanelLayout.HORIZONTAL);
            verticalLayer.setIsActive(panelLayout == PanelLayout.VERTICAL);
        }
    }
    
    @PostConstruct
    public void init(final HardwareElements hwElements) {
        initClipControl(hwElements, 8);
    }
    
    private void initClipControl(final HardwareElements hwElements, final int numberOfScenes) {
        clipLauncherOverdub = transport.isClipLauncherOverdubEnabled();
        clipLauncherOverdub.markInterested();
        transport.isPlaying().markInterested();
        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        
        trackBank = viewControl.getTrackBank();
        markTrackBank(trackBank);
        trackBank.setShouldShowClipLauncherFeedback(true);
        initGridControl(numberOfScenes, hwElements, trackBank);
    }
    
    private void initGridControl(final int numberOfScenes, final HardwareElements hwElements,
        final TrackBank trackBank) {
        for (int i = 0; i < 8; i++) {
            final int trackIndex = i;
            final Track track = trackBank.getItemAt(trackIndex);
            markTrack(track);
            for (int j = 0; j < numberOfScenes; j++) {
                final int sceneIndex = j;
                final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(sceneIndex);
                prepareSlot(slot, sceneIndex, trackIndex);
                
                final RgbButton button = hwElements.getGridButton(sceneIndex, trackIndex);
                button.bindPressed(verticalLayer, () -> handleSlotPressed(slot, track));
                button.bindRelease(verticalLayer, () -> handleSlotReleased(slot));
                button.bindLight(verticalLayer, () -> getState(track, slot, trackIndex, sceneIndex));
                
                final RgbButton horizontalButton = hwElements.getGridButton(trackIndex, sceneIndex);
                horizontalButton.bindPressed(horizontalLayer, () -> handleSlotPressed(slot, track));
                horizontalButton.bindRelease(horizontalLayer, () -> handleSlotReleased(slot));
                horizontalButton.bindLight(horizontalLayer, () -> getState(track, slot, trackIndex, sceneIndex));
            }
        }
    }
    
    @Override
    protected boolean isPlaying() {
        return transport.isPlaying().get();
    }
    
    @Override
    protected boolean isShiftHeld() {
        return modifiers.isShift();
    }
    
    private void handleSlotPressed(final ClipLauncherSlot slot, final Track track) {
        if (modifiers.getAltActive().get()) {
            slot.launchAlt();
        } else if (modifiers.isShift()) {
            if (modifiers.isDuplicate()) {
                track.selectInMixer();
                sequence(slot, () -> focusClip.duplicateContent());
            } else if (modifiers.isClear()) {
                if (slot.hasContent().get()) {
                    track.selectInMixer();
                    sequence(slot, () -> focusClip.clearSteps());
                }
            } else {
                slot.select();
            }
        } else if (modifiers.isClear()) {
            slot.deleteObject();
        } else if (modifiers.isDuplicate()) {
            slot.duplicateClip();
        } else if (modifiers.getQuantizeActive().get()) {
            if (slot.hasContent().get()) {
                track.selectInMixer();
                sequence(slot, () -> focusClip.quantize(1.0));
            }
        } else {
            slot.launch();
        }
    }
    
    private void sequence(final ClipLauncherSlot slot, final Runnable action) {
        slot.select();
        host.scheduleTask(() -> {
            host.scheduleTask(action, 40);
        }, 40);
    }
    
    private void handleSlotReleased(final ClipLauncherSlot slot) {
        if (modifiers.getAltActive().get()) {
            slot.launchReleaseAlt();
        } else {
            slot.launchRelease();
        }
    }
    
    @Override
    protected void onActivate() {
        super.onActivate();
        horizontalLayer.setIsActive(panelLayout == PanelLayout.HORIZONTAL);
        verticalLayer.setIsActive(panelLayout == PanelLayout.VERTICAL);
    }
    
    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        horizontalLayer.setIsActive(false);
        verticalLayer.setIsActive(false);
    }
    
    
}
