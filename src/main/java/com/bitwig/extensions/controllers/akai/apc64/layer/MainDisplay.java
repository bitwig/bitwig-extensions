package com.bitwig.extensions.controllers.akai.apc64.layer;

import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Groove;
import com.bitwig.extension.controller.api.SettableBeatTimeValue;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.akai.apc.common.OrientationFollowType;
import com.bitwig.extensions.controllers.akai.apc.common.led.RgbLightState;
import com.bitwig.extensions.controllers.akai.apc.common.led.VarSingleLedState;
import com.bitwig.extensions.controllers.akai.apc64.Apc64CcAssignments;
import com.bitwig.extensions.controllers.akai.apc64.Apc64MidiProcessor;
import com.bitwig.extensions.controllers.akai.apc64.ApcPreferences;
import com.bitwig.extensions.controllers.akai.apc64.DeviceControl;
import com.bitwig.extensions.controllers.akai.apc64.HardwareElements;
import com.bitwig.extensions.controllers.akai.apc64.Menu;
import com.bitwig.extensions.controllers.akai.apc64.ModifierStates;
import com.bitwig.extensions.controllers.akai.apc64.PadMode;
import com.bitwig.extensions.controllers.akai.apc64.ViewControl;
import com.bitwig.extensions.controllers.akai.apc64.control.OledBacklight;
import com.bitwig.extensions.controllers.akai.apc64.control.SingleLedButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MainDisplay {

    private final Layer mainLayer;
    private final Groove groove;
    private final Menu bitwigMenu;
    private final OledBacklight oledBacklight;
    private final DeviceControl deviceControl;
    private final ApcPreferences preferences;
    private boolean swingModeActive;
    private Screen currentScreen;

    private final double[] FIXED_LENGTH_PRESET_VALUES = {1, 2, 4, 8, 12, 16, 20, 24, 28, 32.0, 40, 48, 56, 64};
    private final String[] RECORD_QUANTIZE = {"OFF", "1/32", "1/16", "1/8", "1/4"};

    private final Map<ScreenMode, Screen> screens = new HashMap<>();

    private final ControllerHost host;

    private final ViewControl viewControl;
    private final Apc64MidiProcessor midiProcessor;
    private long releaseTime = -1;
    private long currentReleaseTime = 1000;
    private final Transport transport;

    private final SettableEnumValue recordQuantizeGrid;
    private SettableEnumValue postRecordingAction;
    private SettableBeatTimeValue postRecordingTimeOffset;
    private final SettableBooleanValue recordQuantizeLength;
    private EncoderMode encoderMode = EncoderMode.TRACK;
    private int touchCount = 0;

    private final ModifierStates modifierStates;

    public enum ScreenMode {
        MAIN,
        PARAMETER,
        METRO,
        FIXED,
        RECORD_QUANTIZE,
        LAUNCH_QUANTIZE,
        MENU(true),
        TEMPO,
        INFO;

        private final boolean hasEmptyBackLight;

        ScreenMode() {
            this(false);
        }

        ScreenMode(final boolean hasEmptyBackLight) {
            this.hasEmptyBackLight = hasEmptyBackLight;
        }
    }

    private enum EncoderMode {
        TRACK,
        FIXED,
        RECORD_QUANTIZE,
        MENU,
        TEMPO
    }

    public class Screen {
        private final String[] rows = {"", "", ""};
        private boolean active;
        private final ScreenMode mode;

        public Screen(final ScreenMode mode) {
            this.mode = mode;
        }

        public ScreenMode getMode() {
            return mode;
        }

        public void setScreen(final String row1, final String row2, final String row3) {
            setRow(0, row1);
            setRow(1, row2);
            setRow(2, row3);
        }

        public void setRow(final int row, final String value) {
            if (!rows[row].equals(value)) {
                rows[row] = value;
                if (active) {
                    midiProcessor.sendText(row, value);
                }
            }
        }

        public void setActive(final boolean active) {
            if (active != this.active) {
                this.active = active;
                if (active) {
                    refresh();
                }
            }
        }

        public void refresh() {
            for (int i = 0; i < 3; i++) {
                midiProcessor.sendText(i, rows[i]);
            }
        }
    }

    public MainDisplay(final Layers layers, final HardwareElements hwElements, final ViewControl viewControl,
                       final Apc64MidiProcessor midiProcessor, final ControllerHost host, final Transport transport,
                       final Application application, final ModifierStates modifierStates,
                       final ApcPreferences preferences) {
        mainLayer = new Layer(layers, "ENCODER_LAYER");
        this.viewControl = viewControl;
        this.midiProcessor = midiProcessor;
        this.host = host;
        this.transport = transport;
        this.modifierStates = modifierStates;
        this.oledBacklight = hwElements.getOledBackLight();
        this.preferences = preferences;

        Arrays.stream(ScreenMode.values()).forEach(mode -> screens.put(mode, new Screen(mode)));
        final Screen mainScreen = screens.get(ScreenMode.MAIN);

        hwElements.getMainEncoder().bind(mainLayer, this::handleEncoder);
        viewControl.getCursorTrack().name().addValueObserver(name -> mainScreen.setRow(0, name));

        deviceControl = viewControl.getDeviceControl();
        deviceControl.getDeviceName().addValueObserver(name -> toScreen(ScreenMode.MAIN, 1, name));
        deviceControl.getPageName().addValueObserver(name -> toScreen(ScreenMode.MAIN, 2, name));
        transport.isMetronomeEnabled()
                .addValueObserver(metroActive -> toScreen(ScreenMode.METRO, 2, metroActive ? "On" : "Off"));


        recordQuantizeGrid = application.recordQuantizationGrid();
        recordQuantizeGrid.addValueObserver(value -> toScreen(ScreenMode.RECORD_QUANTIZE, 2, value));
        recordQuantizeLength = application.recordQuantizeNoteLength();
        recordQuantizeLength.markInterested();
        this.groove = host.createGroove();
        this.groove.getEnabled().markInterested();

        currentScreen = mainScreen;
        currentScreen.setActive(true);
        bitwigMenu = new Menu(screens.get(ScreenMode.MENU));
        initBitwigMenu();
        hwElements.getOledBackLight().bind(mainLayer, () -> RgbLightState.of(viewControl.getCursorTrackColor()));
        hwElements.getEncoderPress().bindIsPressed(mainLayer, this::handleBasicClick);
        host.scheduleTask(this::handlePing, 100);
        initFixedLengthEdit(hwElements);
        initQuantizeEdit(hwElements);
        initTempoEdit(hwElements);

        midiProcessor.addModeChangeListener(newMode -> {
            if (newMode == PadMode.DRUM) {
                currentScreen.refresh();
                midiProcessor.activateDawMode(true);
            }
        });
    }

    public void refresh() {
        currentScreen.refresh();
    }

    public void initBitwigMenu() {
        transport.automationWriteMode().markInterested();
        transport.isArrangerAutomationWriteEnabled().markInterested();
        final String[] autoWriteModeValues = {"latch", "touch", "write"};
        // TODO Exit with long press
        // TODO Improved Value Handling int/double.
        bitwigMenu.addMenuItem(new Menu.HoldMenuItem("ALT Modifier", modifierStates.getAltActive()));
        bitwigMenu.addMenuItem(new Menu.EnumMenuItem("Auto W.Mode", transport.automationWriteMode(),
                List.of(new Menu.EnumMenuValue("latch", "LATCH"),
                        new Menu.EnumMenuValue("touch", "TOUCH"),
                        new Menu.EnumMenuValue("write", "WRITE"))));
        bitwigMenu.addMenuItem(
                new Menu.BooleanToggleMenuItem("Arrange Auto", transport.isArrangerAutomationWriteEnabled()));
        bitwigMenu.addMenuItem(
                new Menu.BooleanToggleMenuItem("Launch Auto", transport.isClipLauncherAutomationWriteEnabled()));
        bitwigMenu.addMenuItem(new Menu.BooleanToggleMenuItem("SHIFT as ALT", preferences.getAltModeWithShift()));
        //bitwigMenu.addMenuItem(new Menu.BooleanMenuItem("Groove Enabled", ));
        bitwigMenu.addMenuItem(new Menu.EnumMenuItem("Grid Layout", preferences.getGridLayoutSettings(),
                List.of(new Menu.EnumMenuValue(OrientationFollowType.AUTOMATIC.getLabel(),
                                OrientationFollowType.AUTOMATIC.getShortLabel()),
                        new Menu.EnumMenuValue(OrientationFollowType.FIXED_VERTICAL.getLabel(),
                                OrientationFollowType.FIXED_VERTICAL.getShortLabel()),
                        new Menu.EnumMenuValue(OrientationFollowType.FIXED_HORIZONTAL.getLabel(),
                                OrientationFollowType.FIXED_HORIZONTAL.getShortLabel()))));

        bitwigMenu.init();
    }

    @Activate
    public void init() {
        mainLayer.setIsActive(true);
    }

    private void toScreen(final ScreenMode mode, final int row, final String value) {
        screens.get(mode).setRow(row, value);
    }

    private void initQuantizeEdit(final HardwareElements hwElements) {
        final SingleLedButton quantizeButton = hwElements.getButton(Apc64CcAssignments.QUANTIZE);

        quantizeButton.bindIsPressed(mainLayer, this::handleQuantizePressed);
        quantizeButton.bindLightPressed(mainLayer,
                pressed -> pressed ? VarSingleLedState.FULL : VarSingleLedState.LIGHT_10);
    }

    private void handleQuantizePressed(final boolean pressed) {
        modifierStates.getQuantizeActive().set(pressed);
        if (modifierStates.isShift()) {
            if (pressed) {
                activatePageDisplay(ScreenMode.RECORD_QUANTIZE, "RecordQuantize", recordQuantizeGrid.get());
                encoderMode = EncoderMode.RECORD_QUANTIZE;
            } else {
                returnToMain();
                encoderMode = EncoderMode.TRACK;
            }
        } else {
            // No Quantize Value available via API
        }
    }

    private void initFixedLengthEdit(final HardwareElements hwElements) {
        final SingleLedButton fixedLengthButton = hwElements.getButton(Apc64CcAssignments.FIXED);
        postRecordingTimeOffset = transport.getClipLauncherPostRecordingTimeOffset();
        postRecordingAction = transport.clipLauncherPostRecordingAction();
        postRecordingAction.markInterested();
        postRecordingTimeOffset.markInterested();
        postRecordingTimeOffset.addValueObserver(v -> {
            toScreen(ScreenMode.FIXED, 2, beatValueToString(v));
        });
        fixedLengthButton.bindDelayedHold(mainLayer, this::toggleFixedMode, this::editFixedLength, 500);
        fixedLengthButton.bindLight(mainLayer, pressed -> postRecordingAction.get()
                .equals("play_recorded") ? (pressed ? VarSingleLedState.PULSE_4 : VarSingleLedState.FULL) : VarSingleLedState.LIGHT_10);
    }

    private void initTempoEdit(final HardwareElements hwElements) {
        final SingleLedButton button = hwElements.getButton(Apc64CcAssignments.TEMPO);
        modifierStates.getShiftActive().addValueObserver(active -> {
            if (!active && swingModeActive) {
                setSwingActive(false);
            }
        });
        //button.bindDelayedHold(mainLayer, () -> transport.tapTempo(), this::handleTempoPressed, 400);
        button.bindIsPressed(mainLayer, this::handleTempoPressed);
        transport.tempo().value().markInterested();
        transport.tempo().displayedValue().markInterested();
        transport.tempo().displayedValue().addValueObserver(value -> toScreen(ScreenMode.TEMPO, 2, value));
    }

    private void handleTempoPressed(final boolean pressed) {
        if (pressed) {
            if (modifierStates.isShift()) {
                setSwingActive(true);
            } else {
                transport.tapTempo();
                encoderMode = EncoderMode.TEMPO;
                activatePageDisplay(ScreenMode.TEMPO, "Tempo", transport.tempo().displayedValue().get(), 500);
            }
        } else {
            setSwingActive(false);
            encoderMode = EncoderMode.TRACK;
            notifyRelease();
        }
    }

    private void setSwingActive(final boolean active) {
        if (this.swingModeActive != active) {
            this.swingModeActive = active;
            if (active) {
                midiProcessor.exitSessionMode();
                midiProcessor.sendMidi(0x96, 0x48, 0x7f);
            } else {
                midiProcessor.enterSessionMode();
                midiProcessor.sendMidi(0x96, 0x48, 0x00);
            }
        }
    }

    public void notifyRelease() {
        releaseTime = System.currentTimeMillis();
    }

    private void editFixedLength(final boolean held) {
        if (held) {
            activatePageDisplay(ScreenMode.FIXED, "Fixed Length", beatValueToString(postRecordingTimeOffset.get()));
            encoderMode = EncoderMode.FIXED;
        } else {
            returnToMain();
            encoderMode = EncoderMode.TRACK;
        }
    }

    private void toggleFixedMode() {
        if (postRecordingAction.get().equals("play_recorded")) {
            postRecordingAction.set("off");
        } else {
            postRecordingAction.set("play_recorded");
        }
    }

    private void handlePing() {
        if (releaseTime != -1 && (System.currentTimeMillis() - releaseTime) > currentReleaseTime) {
            changeScreenMode(stashedMode == null ? ScreenMode.MAIN : stashedMode);
            releaseTime = -1;
            stashedMode = null;
            if (!midiProcessor.modeHasTextControl() && midiProcessor.isSessionModeState()) {
                midiProcessor.exitSessionMode();
            }
        }
        host.scheduleTask(this::handlePing, 100);
    }

    private void handleBasicClick(final boolean pressed) {
        if (encoderMode == EncoderMode.TRACK) {
            handleClickMainMode(pressed);
        } else if (encoderMode == EncoderMode.MENU) {
            handleMenuClick(pressed);
        } else if (encoderMode == EncoderMode.RECORD_QUANTIZE) {
            if (pressed) {
                recordQuantizeLength.toggle();
                activatePageDisplay(ScreenMode.RECORD_QUANTIZE, "RecordQ.Len",
                        recordQuantizeLength.get() ? "OFF" : "ON");
            } else {
                activatePageDisplay(ScreenMode.RECORD_QUANTIZE, "RecordQuantize", recordQuantizeGrid.get());
            }
        }
    }

    private void handleClickMainMode(final boolean pressed) {
        if (modifierStates.isShift()) {
            if (pressed) {
                if (currentScreen.getMode() == ScreenMode.MAIN && encoderMode != EncoderMode.MENU) {
                    encoderMode = EncoderMode.MENU;
                    changeScreenMode(ScreenMode.MENU);
                }
            }
        } else {
            if (pressed) {
                activatePageDisplay(ScreenMode.METRO, "Metronome", "");
                transport.isMetronomeEnabled().toggle();
            } else {
                releaseToMain(500);
            }
        }
    }

    private String beatValueToString(final double v) {
        final double bars = v / 4;
        if (bars == 1.0) {
            return "1 Bar";
        } else if (bars > 1) {
            return "%d Bars".formatted((int) bars);
        } else {
            return "%d Beats".formatted((int) v);
        }
    }

    private void handleEncoder(final int dir) {
        if (encoderMode == EncoderMode.TRACK) {
            if (dir < 0) {
                viewControl.getCursorTrack().selectPrevious();
            } else {
                viewControl.getCursorTrack().selectNext();
            }
        } else if (encoderMode == EncoderMode.FIXED) {
            final int current = valueIndex(postRecordingTimeOffset.get(), FIXED_LENGTH_PRESET_VALUES);
            final int next = current + dir;
            if (next >= 0 && next < FIXED_LENGTH_PRESET_VALUES.length) {
                postRecordingTimeOffset.set(FIXED_LENGTH_PRESET_VALUES[next]);
            }
        } else if (encoderMode == EncoderMode.RECORD_QUANTIZE) {
            final int current = valueIndex(recordQuantizeGrid.get(), RECORD_QUANTIZE);
            final int next = current + dir;
            if (next >= 0 && next < RECORD_QUANTIZE.length) {
                recordQuantizeGrid.set(RECORD_QUANTIZE[next]);
            }
        } else if (encoderMode == EncoderMode.TEMPO) {
            double value = transport.tempo().getRaw();
            value += dir;
            transport.tempo().setRaw(value);
        } else if (encoderMode == EncoderMode.MENU) {
            handleMenuEncoder(dir);
        }
    }

    private void handleMenuEncoder(final int dir) {
        bitwigMenu.handleInc(dir);
    }

    private void handleMenuClick(final boolean pressed) {
        if (modifierStates.isShift() && pressed) {
            encoderMode = EncoderMode.TRACK;
            returnToMain();
        } else {
            bitwigMenu.handEncoderClick(pressed);
        }
    }

    private int valueIndex(final String value, final String[] values) {
        for (int i = 0; i < values.length; i++) {
            if (value.equals(values[i])) {
                return i;
            }
        }
        return -1;
    }

    private int valueIndex(final double value, final double[] values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == value) {
                return i;
            }
        }
        for (int i = 0; i < values.length - 1; i++) {
            final double v1 = values[i];
            final double v2 = values[i + 1];
            if (v1 < value && value < v2) {
                if (Math.abs(v1 - value) < Math.abs(v2 - value)) {
                    return i;
                } else {
                    return i + 1;
                }
            }
        }
        return values.length - 1;
    }

    public void setParameterValue(final String value) {
        toScreen(ScreenMode.PARAMETER, 2, value);
    }

    public void activatePageDisplay(final ScreenMode mode, final String parameterName, final String value) {
        final Screen screen = screens.get(mode);
        screen.setRow(0, "");
        screen.setRow(1, parameterName);
        screen.setRow(2, value);
        changeScreenMode(mode);
        midiProcessor.enterSessionMode();
        releaseTime = -1;
    }

    public void activatePageDisplay(final ScreenMode mode, final String parameterName, final String value,
                                    final long releaseTime) {
        activatePageDisplay(mode, parameterName, value);
        midiProcessor.enterSessionMode();
        currentReleaseTime = releaseTime;
    }

    public void enterMode(final ScreenMode mode, final String parameterName, final String value) {
        final Screen screen = screens.get(mode);
        screen.setRow(0, "");
        screen.setRow(1, parameterName);
        screen.setRow(2, value);
        changeScreenMode(mode);
        releaseToMain(2000);
    }

    public void returnToMain() {
        changeScreenMode(ScreenMode.MAIN);
    }

    private ScreenMode stashedMode = null;

    public void touchParameter(final String destination, final String parameterName, final String value) {
        final Screen screen = screens.get(ScreenMode.PARAMETER);

        screen.setRow(0, destination);
        screen.setRow(1, parameterName);
        screen.setRow(2, value);
        midiProcessor.enterSessionMode();
        if (currentScreen.getMode() != ScreenMode.PARAMETER) {
            stashedMode = currentScreen.getMode();
            changeScreenMode(ScreenMode.PARAMETER);
        }
        touchCount++;
        releaseTime = -1;
    }

    public void releaseToMain(final long waitTime) {
        releaseTime = System.currentTimeMillis();
        currentReleaseTime = waitTime;
    }

    public void releaseTouchParameter(final int sliderIndex) {
        touchCount--;
        if (touchCount <= 0) {
            releaseToMain(1500);
            touchCount = 0;
        }
    }

    public void changeScreenMode(final ScreenMode mode) {
        if (mode != currentScreen.getMode()) {
            currentScreen.setActive(false);
            final boolean changeBackLight = mode.hasEmptyBackLight != currentScreen.getMode().hasEmptyBackLight;
            currentScreen = screens.get(mode);
            if (changeBackLight) {
                if (mode.hasEmptyBackLight) {
                    midiProcessor.sendMidi(0xB0, 0x59, 0);
                } else {
                    midiProcessor.sendMidi(0xB0, 0x59, oledBacklight.getState());
                }
            }
            currentScreen.setActive(true);
        }
    }

}
