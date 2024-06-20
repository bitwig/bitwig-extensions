package com.bitwig.extensions.controllers.akai.apc64;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.Project;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.akai.apc.common.MidiProcessor;
import com.bitwig.extensions.controllers.akai.apc.common.led.VarSingleLedState;
import com.bitwig.extensions.controllers.akai.apc64.control.SingleLedButton;
import com.bitwig.extensions.controllers.akai.apc64.layer.OverviewLayer;
import com.bitwig.extensions.controllers.akai.apc64.layer.PadLayer;
import com.bitwig.extensions.controllers.akai.apc64.layer.SessionLayer;
import com.bitwig.extensions.controllers.akai.apc64.layer.TrackAndSceneLayer;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Context;
import com.bitwig.extensions.framework.values.FocusMode;

import java.time.LocalDateTime;

public class Apc64Extension extends ControllerExtension {
    private static ControllerHost debugHost;
    private HardwareSurface surface;
    private Apc64MidiProcessor midiProcessor;
    private Layer mainLayer;
    private Layer shiftLayer;
    private Transport transport;
    private ViewControl viewControl;
    private FocusClip focusClip;
    private Project project;
    private SessionLayer sessionLayer;
    private OverviewLayer overviewLayer;
    private ApcPreferences preferences;
    private TrackAndSceneLayer sceneAndTrackLayer;
    private PadLayer padLayer;
    private ModifierStates modifierSection;

    public static void println(final String format, final Object... args) {
        if (debugHost != null) {
            final LocalDateTime now = LocalDateTime.now();
            debugHost.println(format.formatted(args));
        }
    }

    protected Apc64Extension(final Apc64ExtensionDefinition definition, final ControllerHost host) {
        super(definition, host);
    }

    @Override
    public void init() {
        debugHost = getHost();
        this.project = getHost().getProject();
        final Context diContext = new Context(this);
        mainLayer = new Layer(diContext.getService(Layers.class), "MAIN_LAYER");
        surface = diContext.getService(HardwareSurface.class);
        initMidi(diContext);
        sessionLayer = diContext.create(SessionLayer.class);
        sceneAndTrackLayer = diContext.create(TrackAndSceneLayer.class);
        overviewLayer = diContext.create(OverviewLayer.class);
        shiftLayer = new Layer(diContext.getService(Layers.class), "SHIFT_LAYER");
        viewControl = diContext.getService(ViewControl.class);
        modifierSection = diContext.getService(ModifierStates.class);

        initMainSection(diContext);
        initTransport(diContext);
        midiProcessor.setHwElements(diContext.getService(HardwareElements.class));
        focusClip = diContext.getService(FocusClip.class);
        preferences = diContext.getService(ApcPreferences.class);
        padLayer = diContext.getService(PadLayer.class);
        sessionLayer.activate();
        sceneAndTrackLayer.activate();
        diContext.activate();
        mainLayer.setIsActive(true);
        midiProcessor.addModeChangeListener(this::handleModeChange);
    }

    private void handleModeChange(final PadMode mode) {
        sessionLayer.setIsActive(mode == PadMode.SESSION);
        overviewLayer.setIsActive(mode == PadMode.OVERVIEW);
        padLayer.setIsActive(mode.isKeyRelated());
    }

    private void initMainSection(final Context context) {
        final HardwareElements hwElements = context.getService(HardwareElements.class);
        final Application application = context.getService(Application.class);

        final SingleLedButton shiftButton = hwElements.getButton(Apc64CcAssignments.SHIFT);
        shiftButton.bindIsPressed(mainLayer, shiftActive -> {
            modifierSection.setShift(shiftActive);
            shiftLayer.setIsActive(shiftActive);
            if (preferences.useShiftForAltMode()) {
                modifierSection.getAltActive().set(shiftActive);
            }
        });

        final SingleLedButton clearButton = hwElements.getButton(Apc64CcAssignments.CLEAR);
        clearButton.bindIsPressed(mainLayer, pressed -> modifierSection.setClear(pressed));
        clearButton.bindLightPressed(mainLayer,
                pressed -> pressed ? VarSingleLedState.FULL : VarSingleLedState.LIGHT_10);

        final SingleLedButton duplicateButton = hwElements.getButton(Apc64CcAssignments.DUPLICATE);
        duplicateButton.bindIsPressed(mainLayer, this::handleDuplicatePressed);
        duplicateButton.bindLightPressed(mainLayer,
                pressed -> pressed ? VarSingleLedState.FULL : VarSingleLedState.LIGHT_10);

        application.canUndo().markInterested();
        application.canRedo().markInterested();

        final SingleLedButton undoButton = hwElements.getButton(Apc64CcAssignments.UNDO);
        undoButton.bindPressed(mainLayer, () -> application.undo());
        undoButton.bindLightPressed(mainLayer, pressed -> {
            if (application.canUndo().get()) {
                return pressed ? VarSingleLedState.FULL : VarSingleLedState.LIGHT_60;
            }
            return VarSingleLedState.OFF;
        });
        undoButton.bindPressed(shiftLayer, () -> application.redo());
        undoButton.bindLightPressed(shiftLayer, pressed -> {
            if (application.canRedo().get()) {
                return pressed ? VarSingleLedState.FULL : VarSingleLedState.LIGHT_60;
            }
            return VarSingleLedState.OFF;
        });
    }


    private void handleDuplicatePressed(final boolean pressed) {
        modifierSection.setDuplicate(pressed);
        if (padLayer.isActive() && modifierSection.isShift() & pressed) {
            padLayer.duplicateContent();
        }
    }

    private void initTransport(final Context diContext) {
        final HardwareElements hwElements = diContext.getService(HardwareElements.class);
        final FocusClip focusClip = diContext.getService(FocusClip.class);
        transport = diContext.getService(Transport.class);
        transport.isPlaying().markInterested();
        transport.isArrangerRecordEnabled().markInterested();
        transport.isClipLauncherOverdubEnabled().markInterested();
        transport.isArrangerOverdubEnabled().markInterested();

        final SingleLedButton playButton = hwElements.getButton(Apc64CcAssignments.PLAY);
        playButton.bindPressed(mainLayer, () -> transport.play());
        playButton.bindLight(mainLayer,
                () -> transport.isPlaying().get() ? VarSingleLedState.FULL : VarSingleLedState.LIGHT_10);

        final SingleLedButton stopButton = hwElements.getButton(Apc64CcAssignments.STOP);
        final Track rootTrack = getHost().getProject().getRootTrackGroup();
        stopButton.bindPressed(mainLayer, () -> transport.stop());
        stopButton.bindLight(mainLayer,
                () -> transport.isPlaying().get() ? VarSingleLedState.FULL : VarSingleLedState.LIGHT_10);
        stopButton.bindPressed(shiftLayer, () -> rootTrack.stop());

        final SingleLedButton recButton = hwElements.getButton(Apc64CcAssignments.REC);
        recButton.bindPressed(mainLayer, () -> handleRecordButton(transport, focusClip));
        recButton.bindLight(mainLayer, () -> recordActive(transport));
        recButton.bindPressed(shiftLayer, () -> handleRecordButtonShift(transport));
        recButton.bindLight(mainLayer, () -> recordActiveShift(transport));
    }

    private void handleRecordButton(final Transport transport, final FocusClip focusClip) {
        if (preferences.getRecordFocusMode() == FocusMode.LAUNCHER) {
            focusClip.invokeRecord();
        } else {
            if (transport.isPlaying().get()) {
                transport.isArrangerRecordEnabled().toggle();
            } else {
                transport.isArrangerRecordEnabled().set(true);
                transport.play();
            }
        }
    }

    private void handleRecordButtonShift(final Transport transport) {
        if (preferences.getRecordFocusMode() == FocusMode.LAUNCHER) {
            transport.isClipLauncherOverdubEnabled().toggle();
        } else {
            transport.isArrangerOverdubEnabled().toggle();
        }
    }

    private VarSingleLedState recordActive(final Transport transport) {
        if (preferences.getRecordFocusMode() == FocusMode.LAUNCHER) {
            return transport.isClipLauncherOverdubEnabled().get() ? VarSingleLedState.FULL : VarSingleLedState.LIGHT_10;
        }
        return transport.isArrangerRecordEnabled().get() ? VarSingleLedState.FULL : VarSingleLedState.LIGHT_10;
    }

    private VarSingleLedState recordActiveShift(final Transport transport) {
        if (preferences.getRecordFocusMode() == FocusMode.LAUNCHER) {
            return transport.isClipLauncherOverdubEnabled().get() ? VarSingleLedState.FULL : VarSingleLedState.LIGHT_10;
        }
        return transport.isArrangerOverdubEnabled().get() ? VarSingleLedState.FULL : VarSingleLedState.LIGHT_10;
    }

    protected void initMidi(final Context diContext) {
        final ControllerHost host = diContext.getService(ControllerHost.class);
        final MidiIn midiIn = host.getMidiInPort(0);
        final MidiIn midiIn2 = host.getMidiInPort(1);
//        midiIn2.setMidiCallback((msg, d1,d2)-> {
//            Apc64Extension.println("IN2 = %02X %02X %02X",msg,d1,d2);
//        });
        final MidiOut midiOut = host.getMidiOutPort(0);
        midiProcessor = new Apc64MidiProcessor(host, midiIn, midiOut, diContext.getService(ModifierStates.class));
        diContext.registerService(MidiProcessor.class, midiProcessor);
        diContext.registerService(Apc64MidiProcessor.class, midiProcessor);
        final NoteInput noteInput = midiIn2.createNoteInput("MIDI", "8?????", "9?????", "A?????", "D?????", "B?????");
        noteInput.setShouldConsumeEvents(true);
        midiProcessor.setPrintToClipSeqConsumer(this::handlePrintToClip);
        midiProcessor.start();
    }

    int ptcCount = 1;

    private void handlePrintToClip(final PrintToClipSeq printToClipSeq) {
        if (printToClipSeq.hasNotes()) {
            focusClip.focusOnNextEmpty(slot -> {
                if (slot.exists().get()) {
                    createClipFromPrint(printToClipSeq, slot);
                } else {
                    project.createScene();
                    getHost().scheduleTask(() -> {
                        createClipFromPrint(printToClipSeq, slot);
                    }, 40);
                }
            });
        }
    }

    private void createClipFromPrint(final PrintToClipSeq printToClipSeq, final ClipLauncherSlot slot) {
        slot.select();
        slot.showInEditor();
        slot.createEmptyClip(4);
        getHost().scheduleTask(() -> printToClipSeq.applyToClip(viewControl.getCursorClip(), ptcCount++), 40);
    }

    @Override
    public void flush() {
        surface.updateHardware();
    }

    @Override
    public void exit() {
        midiProcessor.exitSessionMode();
    }
}
