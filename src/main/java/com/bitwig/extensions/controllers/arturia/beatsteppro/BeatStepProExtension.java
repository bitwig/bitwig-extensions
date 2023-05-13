package com.bitwig.extensions.controllers.arturia.beatsteppro;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.values.Midi;

import java.util.ArrayList;
import java.util.List;

public class BeatStepProExtension extends ControllerExtension {

    public static final String SYSEX_PARAM_CHANGE = "F000206B7F420200%02x%02x%02xF7";
    public static final String DEVICE_INQUIRY = "F07E7F0601F7";
    public static final String EXPECTED_DEVICE_RESPONSE = "f07e7f06020020";
    public static final int STEP_BUTTON_CC_BASE = 70;
    private Layers layers;

    private MidiIn midiIn;
    private MidiOut midiOut;
    private MidiIn mcuMidiIn;
    private MidiOut mcuMidiOut;

    private HardwareSurface surface;
    private ControllerHost host;

    private TrackBank trackBank;
    private CursorTrack cursorTrack;
    
    private Layer mainLayer;
    private Layer shiftLayer;
    private Layer transposeLayer;
    private Layer helpLayer;
    private PadLayer padLayer;
    private boolean expectDeviceResponse = true;
    private RepeatTask repeatWhileHoldTask = null;

    protected BeatStepProExtension(final BeatStepProExtensionDefinition definition, final ControllerHost host) {
        super(definition, host);
    }

    @Override
    public void init() {
        host = getHost();
        layers = new Layers(this);
        midiIn = host.getMidiInPort(0);
        midiIn.setMidiCallback((ShortMidiMessageReceivedCallback) msg -> onMidi0(msg));
        midiIn.setSysexCallback(this::onSysEx);
        midiOut = host.getMidiOutPort(0);
        mcuMidiIn = host.getMidiInPort(1);
        mcuMidiOut = host.getMidiOutPort(1);
        mcuMidiIn.setMidiCallback((ShortMidiMessageReceivedCallback) msg -> onMidi1(msg));

        midiOut.sendSysex(DEVICE_INQUIRY);
        //mcuMidiIn.setSysexCallback(sysEx -> onSysEx(sysEx));
        surface = host.createHardwareSurface();
        setUpPreferences();
        Transport transport = host.createTransport();
        final NoteInput noteInput = midiIn.createNoteInput("MIDI", getMask(0xC));
        noteInput.setShouldConsumeEvents(false);
        cursorTrack = host.createCursorTrack(2, 1);
        PinnableCursorDevice primaryDevice =
            cursorTrack.createCursorDevice("DrumDetection", "Pad Device", 16, CursorDeviceFollowMode.FIRST_INSTRUMENT);
        trackBank = host.createTrackBank(8, 1, 1);

        mainLayer = new Layer(layers, "Main");
        shiftLayer = new Layer(layers, "Shift");
        helpLayer = new Layer(layers, "Help");
        transposeLayer = new Layer(layers, "Transpose");
        padLayer = new PadLayer(this, primaryDevice);

        initTrackMcuControl();
        setUpPadButtons();

        mainLayer.activate();
        padLayer.activate();
        host.showPopupNotification("BeatStep Pro Initialized");
        host.scheduleTask(this::handlePing, 50);
    }

    private String[] getMask(final int excludeChannel) {
        final List<String> masks = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            if (i != excludeChannel) {
                masks.add(String.format("8%01x????", i));
                masks.add(String.format("9%01x????", i));
            }
        }
        masks.add("A?????"); // Poly Aftertouch
        masks.add("D?????"); // Channel Aftertouch
        masks.add("E?????"); // Pitchbend
        return masks.toArray(String[]::new);
    }

    private void handlePing() {
        if (repeatWhileHoldTask != null) {
            repeatWhileHoldTask.ping();
        }
        host.scheduleTask(this::handlePing, 50);
    }

    public Layers getLayers() {
        return layers;
    }

    public MidiIn getMidiIn() {
        return midiIn;
    }

    private void setUpPreferences() {
        final Preferences preferences = getHost().getPreferences(); // THIS
        final SettableBooleanValue initOnStartUp =
            preferences.getBooleanSetting("Send Mappings on Startup", "Mappings", false);
        initOnStartUp.markInterested();
        final Signal signal = preferences.getSignalSetting("Configure CC Mappings", "Mappings",
                "Bitwig Mappings >> " + "Beatstep");
        signal.addSignalObserver(this::initCcModeMapping);
        if(initOnStartUp.get()) {
            host.println(" ... sending Bitwig Mappping to Beat Step Pro ...");
            initCcModeMapping();
        }
    }

    private void initCcModeMapping() {
        for (int i = 0; i < 16; i++) {
            configureCCEncoder(i, 0, 10 + i);
            wait(2);
            configureStepButtons(i, 0, STEP_BUTTON_CC_BASE + i);
            wait(2);
            configurePadButtons(i, 12, 36 + i);
            wait(2);
        }
    }

    private static void wait(int timeInMs) {
        try {
            Thread.sleep(timeInMs);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void configureCCEncoder(final int encoderIndex, final int channel, final int ccNr) {
        final int itemId = 0x20 + encoderIndex;
        midiOut.sendSysex(String.format(SYSEX_PARAM_CHANGE, 1, itemId, 1)); // Type CC
        midiOut.sendSysex(String.format(SYSEX_PARAM_CHANGE, 2, itemId, channel)); // Channel No
        midiOut.sendSysex(String.format(SYSEX_PARAM_CHANGE, 3, itemId, ccNr)); // CC No
        midiOut.sendSysex(String.format(SYSEX_PARAM_CHANGE, 6, itemId, 1)); // Mode Rel #1
        midiOut.sendSysex(String.format(SYSEX_PARAM_CHANGE, 7, itemId, 3)); // Acceleration
        midiOut.sendSysex(String.format(SYSEX_PARAM_CHANGE, 8, itemId, 1)); // just USB
    }

    private void configureStepButtons(final int buttonIndex, final int channel, final int ccNr) {
        final int itemId = 0x30 + buttonIndex;
        midiOut.sendSysex(String.format(SYSEX_PARAM_CHANGE, 1, itemId, 8)); // Type CC for pad
        midiOut.sendSysex(String.format(SYSEX_PARAM_CHANGE, 2, itemId, channel)); // Channel No
        midiOut.sendSysex(String.format(SYSEX_PARAM_CHANGE, 3, itemId, ccNr)); // CC No
        midiOut.sendSysex(String.format(SYSEX_PARAM_CHANGE, 4, itemId, 0)); // Min Value
        midiOut.sendSysex(String.format(SYSEX_PARAM_CHANGE, 5, itemId, 127)); // Max Value
        midiOut.sendSysex(String.format(SYSEX_PARAM_CHANGE, 6, itemId, 1)); // Gate
        midiOut.sendSysex(String.format(SYSEX_PARAM_CHANGE, 8, itemId, 1)); // just USB
        //midiOut.sendSysex(String.format(SYSEX_PARAM_CHANGE, 0x10, itemId, 127)); // Color
    }

    private void configurePadButtons(final int buttonIndex, final int channel, final int noteNr) {
        final int itemId = 0x70 + buttonIndex;
        midiOut.sendSysex(String.format(SYSEX_PARAM_CHANGE, 1, itemId, 9)); // Type CC for pad
        midiOut.sendSysex(String.format(SYSEX_PARAM_CHANGE, 2, itemId, channel)); // Channel No
        midiOut.sendSysex(String.format(SYSEX_PARAM_CHANGE, 3, itemId, noteNr)); // CC No
        midiOut.sendSysex(String.format(SYSEX_PARAM_CHANGE, 4, itemId, 0)); // Min Value
        midiOut.sendSysex(String.format(SYSEX_PARAM_CHANGE, 5, itemId, 127)); // Max Value
        midiOut.sendSysex(String.format(SYSEX_PARAM_CHANGE, 6, itemId, 1)); // Gate
        midiOut.sendSysex(String.format(SYSEX_PARAM_CHANGE, 8, itemId, 1)); // just USB
        //midiOut.sendSysex(String.format(SYSEX_PARAM_CHANGE, 0x10, itemId, 127)); // Color
    }

    private RelativeHardwareKnob createAcceleratedEncoder(final String name, final int channel, final int ccNr) {
        final RelativeHardwareKnob encoder = surface.createRelativeHardwareKnob(name);
        final RelativeHardwareValueMatcher matcher = mcuMidiIn.createRelativeSignedBitCCValueMatcher(channel, ccNr,
                100);
        encoder.setAdjustValueMatcher(matcher);
        encoder.setStepSize(1);
        encoder.setSensitivity(1);
        return encoder;
    }

    private RelativeHardwareKnob createEncoder(final String name, final int channel, final int ccNr) {
        final RelativeHardwareKnob encoder = surface.createRelativeHardwareKnob(name);

        final RelativeHardwareValueMatcher matcher = midiIn.createRelative2sComplementValueMatcher(
                String.format("(status == 176 && data1 == %d )", ccNr), "data2-64", 8, 1);
        encoder.setAdjustValueMatcher(matcher);
        return encoder;
    }

    private void initTrackMcuControl() {
        final PinnableCursorDevice cursorDevice = cursorTrack.createCursorDevice();
        final CursorRemoteControlsPage parameterBank = cursorDevice.createCursorRemoteControlsPage(8);

        final HardwareButton[] muteMcuButtons = new HardwareButton[8];
        final HardwareButton[] soloMcuButtons = new HardwareButton[8];
        final McuSlider[] sliders = new McuSlider[8];
        final RelativeHardwareKnob[] mcuEncoders = new RelativeHardwareKnob[8];

        for (int i = 0; i < 8; i++) {
            muteMcuButtons[i] = createMcuButtonWLight("MUTE_" + (i + 1), 8 + i);
            soloMcuButtons[i] = createMcuButtonWLight("SOLO_" + (i + 1), 16 + i);
            sliders[i] = new McuSlider(i, surface, mcuMidiIn, mcuMidiOut);
            mcuEncoders[i] = createAcceleratedEncoder("PAN_" + (i + 1), 0, 16 + i);
        }

        final HardwareButton trackLeftButton = createMcuButtonWLight("MCU_NAV_LEFT", 46);
        final HardwareButton trackRightButton = createMcuButtonWLight("MCU_NAV_RIGHT", 47);
        mainLayer.bindPressed(trackLeftButton, () -> cursorTrack.selectPrevious());
        mainLayer.bindPressed(trackRightButton, () -> cursorTrack.selectNext());

        trackBank.followCursorTrack(cursorTrack);

        for (int i = 0; i < 8; i++) {
            final Track track = trackBank.getItemAt(i);
            sliders[i].bindParameter(mainLayer, track.volume());
            mainLayer.bindToggle(muteMcuButtons[i], track.mute());
            mainLayer.bindToggle(soloMcuButtons[i], track.solo());
            mainLayer.bind(mcuEncoders[i], track.pan().value());
        }

        final HardwareButton mcuNavLeft = createMcuButton("NAV_LEFT", 46);
        final HardwareButton mcuNavRight = createMcuButton("NAV_RIGHT", 47);
        mainLayer.bindPressed(mcuNavLeft, trackBank::scrollPageBackwards);
        mainLayer.bindPressed(mcuNavRight, trackBank::scrollPageForwards);

        for (int i = 0; i < 8; i++) {
            final RelativeHardwareKnob encoder = createEncoder("CC_TOP_" + (i + 1), 0, 10 + i);
            final RemoteControl parameter = parameterBank.getParameter(i);
            mainLayer.bind(encoder, parameter);
            encoder.setStepSize(1);
            encoder.setSensitivity(0.02);
        }

        final RelativeHardwareKnob volumeEncoder = createEncoder("CC_BOTTOM_1", 0, 18);
        bindEncoderToParameter(mainLayer, volumeEncoder, cursorTrack.volume());
        final RelativeHardwareKnob panEncoder = createEncoder("CC_BOTTOM_2", 0, 19);
        bindEncoderToParameter(mainLayer, panEncoder, cursorTrack.pan());
        final RelativeHardwareKnob send1Encoder = createEncoder("CC_BOTTOM_3", 0, 20);
        bindEncoderToParameter(mainLayer, send1Encoder, cursorTrack.sendBank().getItemAt(0));
        final RelativeHardwareKnob send2Encoder = createEncoder("CC_BOTTOM_4", 0, 21);
        bindEncoderToParameter(mainLayer, send2Encoder, cursorTrack.sendBank().getItemAt(1));

        setUpStepButtons(cursorDevice, parameterBank);
    }

    private void setUpPadButtons() {
        final HardwareButton[] padsButtons = new HardwareButton[16];
        for (int i = 0; i < 16; i++) {
            padsButtons[i] = createNoteButton("PAD_" + (i + 1), 36 + i);
        }
        transposeLayer.bindPressed(padsButtons[0], () -> padLayer.setBaseNote(0));
        transposeLayer.bindPressed(padsButtons[1], () -> padLayer.setBaseNote(2));
        transposeLayer.bindPressed(padsButtons[2], () -> padLayer.setBaseNote(4));
        transposeLayer.bindPressed(padsButtons[3], () -> padLayer.setBaseNote(5));
        transposeLayer.bindPressed(padsButtons[4], () -> padLayer.setBaseNote(7));
        transposeLayer.bindPressed(padsButtons[5], () -> padLayer.setBaseNote(9));
        transposeLayer.bindPressed(padsButtons[6], () -> padLayer.setBaseNote(11));
        transposeLayer.bindPressed(padsButtons[7], () -> padLayer.setBaseNote(0));
        transposeLayer.bindPressed(padsButtons[8], () -> padLayer.setBaseNote(1));
        transposeLayer.bindPressed(padsButtons[9], () -> padLayer.setBaseNote(3));
        transposeLayer.bindPressed(padsButtons[11], () -> padLayer.setBaseNote(6));
        transposeLayer.bindPressed(padsButtons[12], () -> padLayer.setBaseNote(8));
        transposeLayer.bindPressed(padsButtons[13], () -> padLayer.setBaseNote(10));
        transposeLayer.bindPressed(padsButtons[14], () -> padLayer.changeOctave(-1));
        transposeLayer.bindPressed(padsButtons[15], () -> padLayer.changeOctave(1));
        shiftLayer.bindPressed(padsButtons[8], () -> padLayer.setScale(Scale.CHROMATIC));
        shiftLayer.bindPressed(padsButtons[9], () -> padLayer.setScale(Scale.MAJOR));
        shiftLayer.bindPressed(padsButtons[10], () -> padLayer.setScale(Scale.MINOR));
        shiftLayer.bindPressed(padsButtons[11], () -> padLayer.setScale(Scale.DORIAN));
        shiftLayer.bindPressed(padsButtons[12], () -> padLayer.setScale(Scale.MIXOLYDIAN));
        shiftLayer.bindPressed(padsButtons[13], () -> padLayer.setScale(Scale.HARM_MINOR));
        shiftLayer.bindPressed(padsButtons[14], () -> padLayer.setScale(Scale.BLUES));
        shiftLayer.bindPressed(padsButtons[15], () -> padLayer.setScale(Scale.DRUM));
        for (int i = 0; i < 16; i++) {
            final int index = i;
            transposeLayer.bindReleased(padsButtons[i], () -> padLayer.notifyRelease(36 + index));
            shiftLayer.bindReleased(padsButtons[i], () -> padLayer.notifyRelease(36 + index));
        }
    }

    private void bindEncoderToParameter(final Layer layer, final RelativeHardwareKnob encoder,
                                        final Parameter parameter) {
        layer.bind(encoder, parameter);
        encoder.setStepSize(1);
        encoder.setSensitivity(0.02);
    }

    private void setUpStepButtons(final PinnableCursorDevice cursorDevice,
                                  final CursorRemoteControlsPage parameterBank) {
        final int base = STEP_BUTTON_CC_BASE;
        final HardwareButton navParamLeftButton = createCcButton("MCU_STEP_BUTTON_1", base + 0);
        final HardwareButton navParamRightButton = createCcButton("MCU_STEP_BUTTON_2", base + 1);
        mainLayer.bindPressed(navParamLeftButton, parameterBank::selectPrevious);
        mainLayer.bindPressed(navParamRightButton, parameterBank::selectNext);

        final HardwareButton navDeviceLeftButton = createCcButton("MCU_STEP_BUTTON_3", base + 2);
        final HardwareButton navDeviceRightButton = createCcButton("MCU_STEP_BUTTON_4", base + 3);
        mainLayer.bindPressed(navDeviceLeftButton, cursorDevice::selectPrevious);
        mainLayer.bindPressed(navDeviceRightButton, cursorDevice::selectNext);

        final HardwareButton navTrackLeftButton = createCcButton("MCU_STEP_BUTTON_5", base + 4);
        final HardwareButton navTrackRightButton = createCcButton("MCU_STEP_BUTTON_6", base + 5);
        mainLayer.bindIsPressed(navTrackLeftButton, pressed -> doRepeat(pressed, cursorTrack::selectPrevious));
        mainLayer.bindIsPressed(navTrackRightButton, pressed -> doRepeat(pressed, cursorTrack::selectNext));

        helpLayer.bindPressed(navParamLeftButton, () -> host.showPopupNotification("STEP 1: previous parameter page"));
        helpLayer.bindPressed(navParamRightButton, () -> host.showPopupNotification("STEP 2: next parameter page"));
        helpLayer.bindPressed(navTrackLeftButton, () -> host.showPopupNotification("STEP 3: previous track"));
        helpLayer.bindPressed(navTrackRightButton, () -> host.showPopupNotification("STEP 4: next track"));
        helpLayer.bindPressed(navDeviceLeftButton, () -> host.showPopupNotification("STEP 3: previous device"));
        helpLayer.bindPressed(navDeviceRightButton, () -> host.showPopupNotification("STEP 4: next device"));


        final HardwareButton helpButton = createCcButton("MCU_STEP_BUTTON_14", base + 13);
        //mainLayer.bindIsPressed(helpButton, pressed -> helpLayer.setIsActive(pressed));
        final HardwareButton shiftButton = createCcButton("MCU_STEP_BUTTON_15", base + 14);
        mainLayer.bindIsPressed(shiftButton, pressed -> {
            shiftLayer.setIsActive(pressed);
            padLayer.setIsActive(!pressed);
        });
        final HardwareButton transposeButton = createCcButton("MCU_STEP_BUTTON_16", base + 15);
        mainLayer.bindIsPressed(transposeButton, pressed -> {
            transposeLayer.setIsActive(pressed);
            padLayer.setIsActive(!pressed);
        });
        helpLayer.bindPressed(transposeButton, () -> host.showPopupNotification(
                "STEP 16: Transpose Modifier | Current: " + padLayer.getCurrentScale()));
        helpLayer.bindPressed(shiftButton, () -> host.showPopupNotification("STEP 15: Shift"));
    }

    private void onSysEx(final String sysEx) {
        if (sysEx.startsWith(EXPECTED_DEVICE_RESPONSE) && expectDeviceResponse) {
            host.println("Device Beatstep confirmed " + sysEx);
            expectDeviceResponse = false;
        } else {
            host.println("Other SysEx : " + sysEx);
        }
    }

    private void doRepeat(final boolean pressed, final Runnable action) {
        if (pressed) {
            action.run();
            repeatWhileHoldTask = new RepeatTask(800, 100, action);
        } else {
            repeatWhileHoldTask = null;
        }
    }

    private HardwareButton createMcuButtonWLight(final String name, final int noteNr) {
        final HardwareButton button = surface.createHardwareButton(name);
        button.pressedAction().setActionMatcher(mcuMidiIn.createNoteOnActionMatcher(0, noteNr));
        button.releasedAction().setActionMatcher(mcuMidiIn.createNoteOffActionMatcher(0, noteNr));
        final OnOffHardwareLight light = surface.createOnOffHardwareLight("MCU_BUTTON_LIGHT_" + name);
        button.setBackgroundLight(light);
        light.onUpdateHardware(() -> mcuMidiOut.sendMidi(Midi.NOTE_ON, noteNr, light.isOn().currentValue() ? 127 : 0));
        return button;
    }

    private HardwareButton createMcuButton(final String name, final int noteNr) {
        final HardwareButton button = surface.createHardwareButton(name);
        button.pressedAction().setActionMatcher(mcuMidiIn.createNoteOnActionMatcher(0, noteNr));
        return button;
    }

    private HardwareButton createCcButton(final String name, final int ccNr) {
        final HardwareButton button = surface.createHardwareButton(name);
        button.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0, ccNr, 127));
        button.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(0, ccNr, 0));
        final OnOffHardwareLight light = surface.createOnOffHardwareLight(name + "_LIGHT");
        button.setBackgroundLight(light);
        //light.onUpdateHardware(() -> mcuMidiOut.sendMidi(Midi.CC, noteNr, light.isOn().currentValue() ? 127 : 0));
        return button;
    }

    private HardwareButton createNoteButton(final String name, final int noteNr) {
        final HardwareButton button = surface.createHardwareButton(name);
        button.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(12, noteNr));
        button.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(12, noteNr));
        final OnOffHardwareLight light = surface.createOnOffHardwareLight(name + "_LIGHT");
        button.setBackgroundLight(light);
        //light.onUpdateHardware(() -> mcuMidiOut.sendMidi(Midi.CC, noteNr, light.isOn().currentValue() ? 127 : 0));
        return button;
    }

    private void mainEncoderAction(final int dir) {
        host.println(" Encoder " + dir);
    }

    private void onMidi0(final ShortMidiMessage msg) {
        final int channel = msg.getChannel();
        final int sb = msg.getStatusByte() & (byte) 0xF0;
        if (sb != 160) {
            getHost().println("MIDI " + sb + " <" + channel + "> " + msg.getData1() + " " + msg.getData2());
        }
    }

    private void onMidi1(final ShortMidiMessage msg) {
        final int channel = msg.getChannel();
        final int sb = msg.getStatusByte() & (byte) 0xF0;
        getHost().println("MCU  " + sb + " <" + channel + "> " + msg.getData1() + " " + msg.getData2());
    }

    @Override
    public void exit() {
        host.println("EXIT Arturia BeatStep Pro");
    }

    @Override
    public void flush() {
        surface.updateHardware();
    }


}
