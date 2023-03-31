package com.bitwig.extensions.controllers.akai.apcmk2;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.akai.apcmk2.control.HardwareElementsApc;
import com.bitwig.extensions.controllers.akai.apcmk2.control.SingleLedButton;
import com.bitwig.extensions.controllers.akai.apcmk2.layer.AbstractControlLayer;
import com.bitwig.extensions.controllers.akai.apcmk2.layer.SessionLayer;
import com.bitwig.extensions.controllers.akai.apcmk2.layer.TrackControlLayer;
import com.bitwig.extensions.controllers.akai.apcmk2.layer.TrackMode;
import com.bitwig.extensions.controllers.akai.apcmk2.led.SingleLedState;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Context;

public abstract class AbstractAkaiApcExtension extends ControllerExtension {
    protected Layer mainLayer;
    protected Layer shiftLayer;
    protected HardwareSurface surface;
    protected HardwareElementsApc hwElements;
    protected TrackControlLayer trackControlLayer;
    protected AbstractControlLayer controlLayer;
    protected final ApcConfiguration configuration;
    protected Class<? extends AbstractControlLayer> controlLayerClass;

    protected AbstractAkaiApcExtension(final ControllerExtensionDefinition definition, final ControllerHost host,
                                       ApcConfiguration configuration) {
        super(definition, host);
        this.configuration = configuration;
    }

    @Override
    public void init() {
        DebugApc.registerHost(getHost());
        final Context diContext = new Context(this);
        surface = diContext.getService(HardwareSurface.class);
        diContext.registerService(ApcConfiguration.class, configuration);
        initMidi(diContext);
        hwElements = diContext.create(HardwareElementsApc.class);
        mainLayer = diContext.createLayer("MAIN_LAYER");
        Layer sessionLayer = diContext.create(SessionLayer.class);
        controlLayer = diContext.create(controlLayerClass);
        trackControlLayer = diContext.create(TrackControlLayer.class);
        shiftLayer = diContext.createLayer("SHIFT_LAYER");
        assignButtons(diContext);
        assignNavButtons(diContext, configuration.isHasEncoders() ? 0 : 4);
        assignControlButtons(configuration.isHasEncoders() ? 4 : 0);
        init(diContext);

        mainLayer.activate();
        sessionLayer.activate();
        diContext.activate();
    }

    protected abstract void init(Context diContext);

    protected void initMidi(final Context diContext) {
        final ControllerHost host = getHost();
        final MidiIn midiIn = host.getMidiInPort(0);
        final MidiIn midiIn2 = host.getMidiInPort(1);

        MidiOut midiOut = host.getMidiOutPort(0);
        final MidiProcessor midiProcessor = new MidiProcessor(host, midiIn, midiOut);
        diContext.registerService(MidiProcessor.class, midiProcessor);
        final NoteInput noteInput = midiIn2.createNoteInput("MIDI", "8?????", "9?????", "A?????", "D?????", "B?????");
        noteInput.setShouldConsumeEvents(true);
        midiProcessor.start();
    }

    private void assignButtons(Context diContext) {
        ModifierStates modifierStates = diContext.getService(ModifierStates.class);
        final SingleLedButton shiftButton = hwElements.getShiftButton();
        shiftButton.bindPressed(mainLayer, () -> {
            shiftLayer.setIsActive(true);
            modifierStates.setShift(true);
        });
        shiftButton.bindRelease(mainLayer, () -> {
            shiftLayer.setIsActive(false);
            modifierStates.setShift(false);
        });
    }

    private void assignControlButtons(int trackModeOffset) {
        int trackModeButtonIndex = trackModeOffset;
        assignEncoderMode(trackModeButtonIndex++, ControlMode.VOLUME);
        assignEncoderMode(trackModeButtonIndex++, ControlMode.PAN);
        assignEncoderMode(trackModeButtonIndex++, ControlMode.SEND);
        assignEncoderMode(trackModeButtonIndex, ControlMode.DEVICE);

        assignTrackMode(0, TrackMode.STOP);
        assignTrackMode(1, TrackMode.SOLO);
        assignTrackMode(2, TrackMode.MUTE);
        assignTrackMode(3, TrackMode.ARM);
        assignTrackMode(4, TrackMode.SELECT);
    }

    private void assignEncoderMode(int buttonIndex, ControlMode mode) {
        final SingleLedButton button = hwElements.getTrackButton(buttonIndex);
        button.bindPressed(shiftLayer, () -> controlLayer.setMode(mode));
        button.bindLight(shiftLayer, () -> controlLayer.getMode() == mode ? SingleLedState.ON : SingleLedState.OFF);
    }

    private void assignTrackMode(int buttonIndex, TrackMode mode) {
        final SingleLedButton button = hwElements.getSceneButton(buttonIndex);
        button.bindPressed(shiftLayer, () -> trackControlLayer.setMode(mode));
        button.bindLight(shiftLayer,
           () -> trackControlLayer.getMode() == mode ? SingleLedState.ON : SingleLedState.OFF);
    }

    private void assignNavButtons(Context diContext, int navOffset) {
        final ViewControl viewControl = diContext.getService(ViewControl.class);
        final TrackBank trackBank = viewControl.getTrackBank();
        final SceneBank sceneBank = trackBank.sceneBank();
        sceneBank.canScrollForwards().markInterested();
        sceneBank.canScrollBackwards().markInterested();
        trackBank.canScrollForwards().markInterested();
        trackBank.canScrollBackwards().markInterested();

        int navigationButtonIndex = navOffset;

        createNavButton(navigationButtonIndex++, sceneBank.canScrollBackwards(), () -> sceneBank.scrollBy(-1));
        createNavButton(navigationButtonIndex++, sceneBank.canScrollForwards(), () -> sceneBank.scrollBy(1));

        createNavButton(navigationButtonIndex++, trackBank.canScrollBackwards(), () -> trackBank.scrollBy(-1));
        createNavButton(navigationButtonIndex, trackBank.canScrollForwards(), () -> trackBank.scrollBy(1));
    }

    private void createNavButton(int buttonIndex, BooleanValue value, Runnable action) {
        final SingleLedButton button = hwElements.getTrackButton(buttonIndex);
        button.bindLight(shiftLayer, () -> value.get() ? SingleLedState.ON : SingleLedState.OFF);
        button.bindRepeatHold(shiftLayer, action);
    }

    @Override
    public void flush() {
        surface.updateHardware();
    }

    @Override
    public void exit() {
        getHost().showPopupNotification("Exit Akai APC Keys 25 mk2");
    }

}
