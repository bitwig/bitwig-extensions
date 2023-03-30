package com.bitwig.extensions.controllers.novation.launchpadpromk3.sliderlayers;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.novation.commonsmk3.LabeledButton;
import com.bitwig.extensions.controllers.novation.commonsmk3.MidiProcessor;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.controllers.novation.commonsmk3.SliderBinding;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.*;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.layers.ControlMode;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.values.BooleanValueObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VolumeSliderLayer extends TrackSliderLayer {

    private int volumeMode = 0;
    private final int[] vuLevels = new int[8];
    private final boolean[] masterExists = new boolean[8];
    private final Layer volumeLayer;
    private final Layer masterLayer;
    private final Layer sceneButtonLayer;
    private Layer currentLayer;
    private final List<SliderBinding> masterBindings = new ArrayList<>();
    private final Map<Integer, Layer> modeLayers = new HashMap<>();
    private final BooleanValueObject touchActive = new BooleanValueObject();

    public VolumeSliderLayer(final ViewCursorControl viewCursorControl, final ModifierStates modifierStates,
                             final HardwareSurface controlSurface, final Layers layers,
                             final MidiProcessor midiProcessor, final SysExHandler sysExHandler,
                             final ControllerHost host, final HwElements hwElements) {
        super("VOL", ControlMode.VOLUME, controlSurface, layers, midiProcessor, sysExHandler);
        volumeLayer = new Layer(layers, "VOL_VOL");
        masterLayer = new Layer(layers, "VOL_MASTER");
        final Layer levelLayer = new Layer(layers, "VOL_LEVEL");
        sceneButtonLayer = new Layer(layers, "VOL_SCENE_BUTTONS");
        modeLayers.put(0, volumeLayer);
        modeLayers.put(1, masterLayer);
        modeLayers.put(2, levelLayer);

        modifierStates.getShiftActive().addValueObserver(shift -> switchSceneLayer(!shift));
        currentLayer = volumeLayer;
        final MasterTrack masterTrack = host.createMasterTrack(1);
        final TrackBank effectTrackBank = host.createEffectTrackBank(7, 1);
        bind(viewCursorControl.getTrackBank());
        bindMaster(masterTrack, effectTrackBank);
        initSceneButtons(hwElements);
    }

    private void switchSceneLayer(final boolean deactivate) {
        if (isActive()) {
            sceneButtonLayer.setIsActive(deactivate);
        }
    }

    private void bindMaster(final MasterTrack masterTrack, final TrackBank effectBank) {
        for (int i = 0; i < 7; i++) {
            final Track track = effectBank.getItemAt(i);
            final int index = i;
            track.exists().addValueObserver(effExists -> masterExists[index] = effExists);
            final SliderBinding binding = new SliderBinding(mode.getCcNr(), track.volume(), sliders[i], i,
                    midiProcessor);
            masterLayer.addBinding(binding);
            masterBindings.add(binding);
        }
        final SliderBinding binding = new SliderBinding(mode.getCcNr(), masterTrack.volume(), sliders[7], 7,
                midiProcessor);
        masterLayer.addBinding(binding);
        masterBindings.add(binding);
    }

    @Override
    protected void bind(final TrackBank trackBank) {
        final MidiOut midiOut = midiProcessor.getMidiOut();
        for (int i = 0; i < 8; i++) {
            final Track track = trackBank.getItemAt(i);
            final int index = i;
            //volumeLayer.bind(sliders[i], track.volume().value());
            final SliderBinding binding = new SliderBinding(mode.getCcNr(), track.volume(), sliders[i], i,
                    midiProcessor);
           volumeLayer.addBinding(binding);
           valueBindings.add(binding);
        }
        touchActive.addValueObserver(touched -> {
            for (int i = 0; i < 8; i++) {
                final Track track = trackBank.getItemAt(i);
                track.volume().touch(touched);
            }
        });
    }

    private void handleVu(final int index, final int value) {
        if (isActive() && volumeMode == 2) {
            vuLevels[index] = value;
            midiProcessor.sendMidi(0xB4, mode.getCcNr() + index, value);
        }
    }

    private void initSceneButtons(final HwElements hwElements) {
        for (int index = 0; index < 8; index++) {
            final LabeledButton sceneButton = hwElements.getSceneLaunchButtons().get(index);
            sceneButton.disable(sceneButtonLayer);
        }
    }

    private void initSceneButtons_advanced(final HwElements hwElements) {
        for (int i = 0; i < 7; i++) {
            final int index = i;
            final LabeledButton sceneButton = hwElements.getSceneLaunchButtons().get(index);
            sceneButton.bindPressed(sceneButtonLayer, pressed -> handleSelect(pressed, index));
            sceneButton.bindLight(sceneButtonLayer, () -> getModeState_advanced(index));
        }
        final LabeledButton sceneButton = hwElements.getSceneLaunchButtons().get(7);
        sceneButton.bindPressed(sceneButtonLayer, touchActive::set);
        sceneButton.bindLight(sceneButtonLayer, () -> touchActive.get() ? RgbState.of(9) : RgbState.of(11));
    }

    private RgbState getModeState_advanced(final int index) {
        if (index < 3) {
            switch (index) {
                case 0:
                    return volumeMode == 0 ? RgbState.of(25) : RgbState.of(27);
                case 1:
                    return volumeMode == 1 ? RgbState.of(33) : RgbState.of(35);
                case 2:
                    return volumeMode == 2 ? RgbState.of(41) : RgbState.of(43);
            }
        }
        return RgbState.OFF;
    }

    private void handleSelect(final boolean pressed, final int index) {
        if (!pressed || volumeMode == index) {
            return;
        }
        final Layer nextLayer = modeLayers.get(index);
        if (nextLayer == null) {
            return;
        }
        currentLayer.setIsActive(false);
        currentLayer = nextLayer;
        currentLayer.setIsActive(true);
        volumeMode = index;
        updateFaderState();
    }

    @Override
    protected void updateFaderState() {
        if (isActive()) {
            refreshTrackColors();
            modeHandler.setFaderBank(0, mode, tracksExistsColors);
            switch (volumeMode) {
                case 0:
                    valueBindings.forEach(SliderBinding::update);
                    break;
                case 1:
                    masterBindings.forEach(SliderBinding::update);
                    break;
                case 2:
                    for (int i = 0; i < 8; i++) {
                        midiProcessor.sendMidi(0xB4, mode.getCcNr() + i, vuLevels[i]);
                    }
            }

        }
    }


    @Override
    protected void refreshTrackColors() {
        final boolean[] exists = trackState.getExists();
        final int[] colorIndex = trackState.getColorIndex();

        for (int i = 0; i < 8; i++) {
            switch (volumeMode) {
                case 0:
                    tracksExistsColors[i] = exists[i] ? mode.getColor() : 0;
                    break;
                case 1:
                    if (i == 7) {
                        tracksExistsColors[i] = 1;
                    } else {
                        tracksExistsColors[i] = masterExists[i] ? 4 : 0;
                    }
                    break;
                case 2:
                    tracksExistsColors[i] = exists[i] ? colorIndex[i] : 0;
                    break;
            }
        }
    }

    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        currentLayer.setIsActive(false);
        sceneButtonLayer.setIsActive(false);
        updateFaderState();
    }

    @Override
    protected void onActivate() {
        super.onActivate();
        DebugOutLp.println("Activate Volume Layer");
        refreshTrackColors();
        currentLayer.setIsActive(true);
        sceneButtonLayer.setIsActive(true);
        modeHandler.changeMode(LpBaseMode.FADER, mode.getBankId());
        modeHandler.setFaderBank(0, mode, tracksExistsColors);
        updateFaderState();
    }

}
