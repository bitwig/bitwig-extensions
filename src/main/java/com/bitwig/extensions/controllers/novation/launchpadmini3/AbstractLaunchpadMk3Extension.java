package com.bitwig.extensions.controllers.novation.launchpadmini3;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.novation.commonsmk3.LaunchpadDeviceConfig;
import com.bitwig.extensions.controllers.novation.commonsmk3.MidiProcessor;
import com.bitwig.extensions.controllers.novation.launchpadmini3.layers.*;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Context;
import com.bitwig.extensions.framework.di.TrackerRegistration;
import com.bitwig.extensions.framework.di.ViewTracker;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public abstract class AbstractLaunchpadMk3Extension extends ControllerExtension {

    protected HardwareSurface surface;
    protected MidiOut midiOut;
    protected SessionLayer sessionLayer;
    protected OverviewLayer overviewLayer;
    protected LpMode mode = LpMode.SESSION;
    protected final LaunchpadDeviceConfig deviceConfig;
    protected Layer currentLayer;
    protected MidiProcessor midiProcessor;
    protected HwElements hwElements;
    private TrackerRegistration trackerRegistration;

    protected AbstractLaunchpadMk3Extension(final ControllerExtensionDefinition definition, final ControllerHost host,
                                            final LaunchpadDeviceConfig deviceConfig) {
        super(definition, host);
        this.deviceConfig = deviceConfig;
    }

    protected Context initContext() {
        DebugMini.registerHost(getHost());
        final Context diContext = new Context(this);
        surface = diContext.getService(HardwareSurface.class);
        diContext.registerService(LaunchpadDeviceConfig.class, deviceConfig);
        final ControllerHost host = diContext.getService(ControllerHost.class);
        initMidi(diContext, host);
        sessionLayer = diContext.create(SessionLayer.class);
        overviewLayer = diContext.create(OverviewLayer.class);
        hwElements = diContext.getService(HwElements.class);
        diContext.create(NotePlayingLayer.class);
        createControlLayers(diContext);
        setUpTracking(diContext);
        return diContext;
    }

    private void setUpTracking(final Context diContext) {
        final ViewTracker tracker = diContext.getViewTracker();
        final ViewCursorControl viewCursor = diContext.getService(ViewCursorControl.class);
        trackerRegistration = tracker.registerViewPositionListener(deviceConfig.getDeviceId(), (src, trackIndex) -> {
            DebugMini.println(" Received Track Change %s %d", src, trackIndex);
            viewCursor.getTrackBank().scrollPosition().set(trackIndex);
        });
        viewCursor.getTrackBank()
                .scrollPosition()
                .addValueObserver(trackIndex -> tracker.fireViewChanged(deviceConfig.getDeviceId(), trackIndex));
    }

    private void createControlLayers(final Context diContext) {
        sessionLayer.registerControlLayer(ControlMode.VOLUME, diContext.create(VolumeSliderLayer.class));
        sessionLayer.registerControlLayer(ControlMode.PAN, diContext.create(PanSliderLayer.class));
        sessionLayer.registerControlLayer(ControlMode.SENDS, diContext.create(SendsSliderLayer.class));
        sessionLayer.registerControlLayer(ControlMode.DEVICE, diContext.create(DeviceSliderLayer.class));
    }

    protected void initMidi(final Context diContext, final ControllerHost host) {
        final MidiIn midiIn = host.getMidiInPort(0);
        final MidiIn midiIn2 = host.getMidiInPort(1);
        midiIn.setMidiCallback((ShortMidiMessageReceivedCallback) this::onMidi0);

        midiIn2.setMidiCallback((ShortMidiMessageReceivedCallback) this::onMidi1);
        // midiIn2.setSysexCallback(this::handleSysEx2);
        midiOut = host.getMidiOutPort(0);
        midiProcessor = new MidiProcessor(host, midiIn, midiOut, deviceConfig);
        diContext.registerService(MidiProcessor.class, midiProcessor);
        final NoteInput noteInput = midiIn2.createNoteInput("MIDI", "8?????", "9?????", "A?????", "D?????");
        //noteInput.setShouldConsumeEvents(false);
        midiIn.setSysexCallback(this::handleSysEx);
        midiProcessor.start();
    }

    protected abstract void handleSysEx(String sysEx);

    public void onMidi0(final ShortMidiMessage msg) {
        DebugMini.println("MIDI %02X %02X %02X", msg.getStatusByte(), msg.getData1(), msg.getData2());
    }

    public void onMidi1(final ShortMidiMessage msg) {
        DebugMini.println("MIDI 2 -> %02X %02X %02X", msg.getStatusByte(), msg.getData1(), msg.getData2());
    }

    private void shutDownController(final CompletableFuture<Boolean> shutdown) {
        midiProcessor.enableDawMode(false);
        try {
            Thread.sleep(300);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
        shutdown.complete(true);
    }

    @Override
    public void flush() {
        surface.updateHardware();
    }

    @Override
    public void exit() {
        if (trackerRegistration != null) {
            trackerRegistration.unregister();
        }
        final CompletableFuture<Boolean> shutdown = new CompletableFuture<>();
        Executors.newSingleThreadExecutor().execute(() -> shutDownController(shutdown));
        try {
            shutdown.get();
        } catch (final InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }


}
