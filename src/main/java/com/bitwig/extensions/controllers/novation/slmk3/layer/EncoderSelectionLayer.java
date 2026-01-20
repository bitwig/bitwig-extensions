package com.bitwig.extensions.controllers.novation.slmk3.layer;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.Send;
import com.bitwig.extension.controller.api.SendBank;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.novation.slmk3.CcAssignment;
import com.bitwig.extensions.controllers.novation.slmk3.DeviceView;
import com.bitwig.extensions.controllers.novation.slmk3.GlobalStates;
import com.bitwig.extensions.controllers.novation.slmk3.SlMk3HardwareElements;
import com.bitwig.extensions.controllers.novation.slmk3.ViewControl;
import com.bitwig.extensions.controllers.novation.slmk3.bindings.BoxPanelBinding;
import com.bitwig.extensions.controllers.novation.slmk3.bindings.IncrementHandler;
import com.bitwig.extensions.controllers.novation.slmk3.bindings.ParameterPanelBinding;
import com.bitwig.extensions.controllers.novation.slmk3.bindings.SimpleParameterPanelBinding;
import com.bitwig.extensions.controllers.novation.slmk3.control.RgbButton;
import com.bitwig.extensions.controllers.novation.slmk3.control.SlEncoder;
import com.bitwig.extensions.controllers.novation.slmk3.display.ButtonMode;
import com.bitwig.extensions.controllers.novation.slmk3.display.ButtonSubPanel;
import com.bitwig.extensions.controllers.novation.slmk3.display.KnobMode;
import com.bitwig.extensions.controllers.novation.slmk3.display.ScreenHandler;
import com.bitwig.extensions.controllers.novation.slmk3.display.SequencerButtonSubMode;
import com.bitwig.extensions.controllers.novation.slmk3.display.ShiftAction;
import com.bitwig.extensions.controllers.novation.slmk3.display.SlRgbState;
import com.bitwig.extensions.controllers.novation.slmk3.display.panel.BoxPanel;
import com.bitwig.extensions.controllers.novation.slmk3.display.panel.KnobPanel;
import com.bitwig.extensions.controllers.novation.slmk3.display.panel.ScreenSetup;
import com.bitwig.extensions.controllers.novation.slmk3.display.panel.SelectionSubPanel;
import com.bitwig.extensions.controllers.novation.slmk3.value.BufferedObservableValue;
import com.bitwig.extensions.controllers.novation.slmk3.value.ObservableColor;
import com.bitwig.extensions.controllers.novation.slmk3.value.StringValueSet;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.BasicStringValue;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.bitwig.extensions.framework.values.ValueObject;

@Component
public class EncoderSelectionLayer {
    private final SlRgbState[] DEVICE_COLORS = {
        SlRgbState.RED, SlRgbState.ORANGE, SlRgbState.YELLOW, SlRgbState.GREEN, SlRgbState.DARK_GREEN, SlRgbState.BLUE,
        SlRgbState.PURPLE, SlRgbState.PINK
    };
    private final SlRgbState[] trackColors = new SlRgbState[8];
    private final ViewControl viewControl;
    
    private final ValueObject<KnobMode> knobMode = new ValueObject<>(KnobMode.DEVICE);
    private KnobMode stashedKnobMode;
    private final ObservableColor trackColor;
    
    private final GlobalStates globalStates;
    private final Application application;
    private final Transport transport;
    private final ScreenHandler screenHandler;
    private final SlMk3HardwareElements hwElements;
    private final LayerRepo layerRepo;
    
    public EncoderSelectionLayer(final ViewControl viewControl, final ScreenHandler screenHandler,
        final SlMk3HardwareElements hwElements, final GlobalStates globalStates, final Application application,
        final Transport transport, final LayerRepo layerRepo) {
        this.application = application;
        this.transport = transport;
        this.screenHandler = screenHandler;
        this.hwElements = hwElements;
        this.layerRepo = layerRepo;
        
        application.canUndo().markInterested();
        application.canRedo().markInterested();
        
        Arrays.fill(trackColors, SlRgbState.OFF);
        this.viewControl = viewControl;
        this.trackColor = globalStates.getTrackColor();
        final TrackBank trackBank = viewControl.getTrackBank();
        final List<SlEncoder> encoders = hwElements.getEncoders();
        this.globalStates = globalStates;
        globalStates.getShiftState().addValueObserver(shift -> applyMode());
        globalStates.getBaseMode().addValueObserver(mode -> applyMode());
        final BufferedObservableValue<GridMode> baseMode = globalStates.getBaseMode();
        baseMode.addValueObserver(mode -> applyMode());
        this.knobMode.addValueObserver(value -> applyMode());
        
        viewControl.getSelectedTrackIndex().addValueObserver(this::handleTrackSelectionChanged);
        viewControl.getCursorTrack().color().addValueObserver((r, g, b) -> trackColor.set(SlRgbState.get(r, g, b)));
        
        final Layer trackButtonSelectionLayer = layerRepo.getButtonLayer(ButtonMode.TRACK);
        final ButtonSubPanel buttonPanel = screenHandler.getSubPanel(ButtonMode.TRACK);
        for (int i = 0; i < 8; i++) {
            final int index = i;
            final Track track = trackBank.getItemAt(i);
            final SelectionSubPanel panel = buttonPanel.get(i);
            track.exists().markInterested();
            bindTrackToScreens(i, track);
            
            final RgbButton selectButton = hwElements.getSelectButtons().get(i);
            selectButton.bindLight(trackButtonSelectionLayer, () -> getTrackColor(index, track));
            selectButton.bindPressed(trackButtonSelectionLayer, () -> handleTrackPressed(track));
        }
        
        bindRemotes(
            layerRepo.getKnobLayer(KnobMode.DEVICE), screenHandler.getScreen(KnobMode.DEVICE),
            viewControl.getPrimaryRemotes(), i -> DEVICE_COLORS[i]);
        bindRemotes(
            layerRepo.getKnobLayer(KnobMode.TRACK), screenHandler.getScreen(KnobMode.TRACK),
            viewControl.getTrackRemotes(), i -> DEVICE_COLORS[i]);
        bindRemotes(
            layerRepo.getKnobLayer(KnobMode.PROJECT), screenHandler.getScreen(KnobMode.PROJECT),
            viewControl.getProjectRemotes(), i -> DEVICE_COLORS[i]);
        bindPanControl(trackBank, encoders);
        bindSendControl(trackBank, encoders);
        bindDrumsMix(layerRepo, screenHandler);
        bindDeviceSelect();
        bindShiftOption();
        
        bindOptionSelect();
        setupShiftLayer(hwElements);
    }
    
    private void bindShiftOption() {
        final ScreenSetup<BoxPanel> screen = screenHandler.getOptionShiftScreen();
        final Layer encoderLayer = layerRepo.getKnobLayer(KnobMode.OPTION_SHIFT);
        final List<SlEncoder> encoders = hwElements.getEncoders();
        int index = 0;
        final SettableEnumValue recordQuantizeGrid = application.recordQuantizationGrid();
        recordQuantizeGrid.markInterested();
        transport.tempo().markInterested();
        final StringValueSet quantizeValues =
            new StringValueSet().add("OFF").add("1/32").add("1/16").add("1/8").add("1/4")
                .setValueReceiver(v -> recordQuantizeGrid.set(v));
        recordQuantizeGrid.addValueObserver(v -> quantizeValues.setToValue(v));
        quantizeValues.setToValue(recordQuantizeGrid.get());
        
        final StringValueSet preRollValues = new StringValueSet().add("none", "NONE") //
            .add("one_bar", "1") //
            .add("two_bars", "2") //
            .add("four_bars", "4").setValueReceiver(v -> transport.preRoll().set(v));
        transport.preRoll().addValueObserver(v -> preRollValues.setToValue(v));
        
        final StringValueSet postRecordingValues = new StringValueSet().add("off", "Off") //
            .add("play_recorded", "PlayRec") //
            .add("record_next_free_slot", "NextFree") //
            .add("return_to_arrangement", "Ret.Arr") //
            .add("stop", "Stop") //
            .add("return_to_previous_clip", "PlayLast") //
            .add("play_random", "Play Rand") //
            .setValueReceiver(v -> transport.clipLauncherPostRecordingAction().set(v));
        transport.clipLauncherPostRecordingAction().addValueObserver(v -> postRecordingValues.setToValue(v));
        final SlRgbState frameColor = SlRgbState.ORANGE;
        final SettableRangedValue metroVolume = transport.metronomeVolume();
        encoderLayer.addBinding(new BoxPanelBinding(
            transport.tempo().displayedValue(), screen.getPanel(index),
            new BasicStringValue("Tempo"), frameColor));
        encoders.get(index++).bindIncrementAction(
            encoderLayer,
            inc -> incrementBy(transport.tempo(), inc, globalStates.getShiftState().get()));
        
        encoderLayer.addBinding(new BoxPanelBinding(
            quantizeValues.getDisplayValue(), screen.getPanel(index),
            new BasicStringValue("Rec.Qu"), frameColor));
        encoders.get(index++).bindIncrementAction(encoderLayer, new IncrementHandler(quantizeValues::incrementBy, 10));
        
        encoderLayer.addBinding(new BoxPanelBinding(
            preRollValues.getDisplayValue(), screen.getPanel(index),
            new BasicStringValue("PreRoll"), frameColor));
        encoders.get(index++).bindIncrementAction(encoderLayer, new IncrementHandler(preRollValues::incrementBy, 10));
        
        encoderLayer.addBinding(
            new BoxPanelBinding(
                postRecordingValues.getDisplayValue(), screen.getPanel(index), new BasicStringValue("PstRecAc"),
                frameColor));
        encoders.get(index++)
            .bindIncrementAction(encoderLayer, new IncrementHandler(postRecordingValues::incrementBy, 10));
        
        encoderLayer.addBinding(
            new BoxPanelBinding(
                metroVolume.displayedValue(), screen.getPanel(index), new BasicStringValue("Metr.Vol"), frameColor));
        encoders.get(index++).bindParameter(encoderLayer, metroVolume);
    }
    
    public void incrementBy(final Parameter parameter, final int inc, final boolean modifier) {
        final double amount = modifier ? 0.01 * inc : inc;
        final double newValue = Math.max(20, Math.min(666, parameter.getRaw() + amount));
        parameter.setRaw(newValue);
    }
    
    private void bindDeviceSelect() {
        final List<DeviceView> devices = viewControl.getDevices();
        final ScreenSetup<BoxPanel> screen = screenHandler.getOptionDeviceScreen();
        final List<RgbButton> padButtons = hwElements.getPadButtons();
        final Layer deviceSelectionButtonLayer = layerRepo.getPadDeviceSelectionLayer();
        for (int i = 0; i < 16; i++) {
            final int index = i;
            final DeviceView device = devices.get(i);
            final BoxPanel panel = screen.getPanel(i % 8);
            if (i < 8) {
                device.getName().addValueObserver(name -> panel.setText(0, name));
                device.getPageName().addValueObserver(name -> panel.setText(1, name));
                device.getDeviceColor().addValueObserver(color -> panel.setColor(0, color));
                device.getSelected().addValueObserver(selected -> panel.setTopSelected(selected));
            } else {
                device.getName().addValueObserver(name -> panel.setText(2, name));
                device.getPageName().addValueObserver(name -> panel.setText(3, name));
                device.getDeviceColor().addValueObserver(color -> panel.setColor(1, color));
                device.getSelected().addValueObserver(selected -> panel.setCenterSelected(selected));
            }
            final RgbButton button = padButtons.get(i);
            button.bindLight(
                deviceSelectionButtonLayer, () -> device.getSelected().get()
                    ? device.getDeviceColor().get()
                    : device.getDeviceColor().get().reduced(10));
            button.bindPressed(deviceSelectionButtonLayer, () -> handleDeviceSelect(device));
        }
        hwElements.getButton(CcAssignment.PADS_UP).bindDisabled(deviceSelectionButtonLayer);
        hwElements.getButton(CcAssignment.PADS_DOWN).bindDisabled(deviceSelectionButtonLayer);
        hwElements.getButton(CcAssignment.SCENE_LAUNCH_1).bindDisabled(deviceSelectionButtonLayer);
        hwElements.getButton(CcAssignment.SCENE_LAUNCH_2).bindDisabled(deviceSelectionButtonLayer);
    }
    
    private void handleDeviceSelect(final DeviceView device) {
        if (globalStates.getClearState().get()) {
            device.delete();
        } else if (globalStates.getDuplicateState().get()) {
            device.duplicate();
        } else if (globalStates.getShiftState().get()) {
            device.selectNested();
        } else {
            device.select();
        }
    }
    
    private void bindTrackToScreens(final int index, final Track track) {
        final SelectionSubPanel panel = screenHandler.getSubPanel(ButtonMode.TRACK).get(index);
        final KnobPanel panPanel = screenHandler.getScreen(KnobMode.PAN).getPanel(index);
        final KnobPanel sendsPanel = screenHandler.getScreen(KnobMode.SEND).getPanel(index);
        track.name().addValueObserver(name -> {
            panPanel.setText(0, name);
            sendsPanel.setText(0, name);
            panel.setRow2(name);
        });
        track.color().addValueObserver((r, g, b) -> {
            final SlRgbState trackColor = SlRgbState.get(r, g, b);
            panel.setColor(trackColor);
            panPanel.setColor(0, trackColor);
            sendsPanel.setColor(0, trackColor);
            this.trackColors[index] = trackColor;
        });
    }
    
    private void bindOptionSelect() {
        bindOptionSelect(0, "", "Device", KnobMode.DEVICE, SlRgbState.WHITE);
        bindOptionSelect(1, "", "Pan", KnobMode.PAN, SlRgbState.WHITE);
        bindOptionSelect(2, "", "Sends", KnobMode.SEND, SlRgbState.WHITE);
        bindOptionSelect(3, "Track", "Remotes", KnobMode.TRACK, SlRgbState.WHITE);
        bindOptionSelect(4, "Project", "Remotes", KnobMode.PROJECT, SlRgbState.WHITE);
        bindOptionDrum(5);
        bindOptionEmpty(6);
        bindOptionEmpty(7);
        final List<SlEncoder> encoders = hwElements.getEncoders();
        for (int i = 0; i < 8; i++) {
            final SlEncoder encoder = encoders.get(i);
            encoder.bindEmpty(layerRepo.getKnobLayer(KnobMode.OPTION));
        }
    }
    
    private void bindOptionEmpty(final int index) {
        final RgbButton button = hwElements.getSelectButtons().get(index);
        final Layer optionButtonSelectionLayer = layerRepo.getButtonLayer(ButtonMode.OPTION);
        button.bindLight(optionButtonSelectionLayer, () -> SlRgbState.OFF);
        button.bindPressed(optionButtonSelectionLayer, () -> {});
    }
    
    private void bindOptionSelect(final int index, final String name1, final String name, final KnobMode bindMode,
        final SlRgbState color) {
        final ButtonSubPanel optionsPanels = screenHandler.getSubPanel(ButtonMode.OPTION);
        final Layer optionButtonSelectionLayer = layerRepo.getButtonLayer(ButtonMode.OPTION);
        final BooleanValueObject isOnMode = new BooleanValueObject(knobMode.get() == bindMode);
        knobMode.addValueObserver(mode -> isOnMode.set(mode == bindMode));
        final SelectionSubPanel panel = optionsPanels.get(index);
        panel.bindValueWithSelect(name1, name, isOnMode, color, color.reduced(50));
        final RgbButton button = hwElements.getSelectButtons().get(index);
        final SlRgbState colorLedDim = color.reduced(20);
        button.bindLight(optionButtonSelectionLayer, () -> isOnMode.get() ? color : colorLedDim);
        button.bindPressed(optionButtonSelectionLayer, () -> selectKnobMode(bindMode));
    }
    
    private void bindOptionDrum(final int index) {
        final String title = "Drum Mix";
        final ButtonSubPanel optionsPanels = screenHandler.getSubPanel(ButtonMode.OPTION);
        final Layer optionButtonSelectionLayer = layerRepo.getButtonLayer(ButtonMode.OPTION);
        final BooleanValueObject isOnMode = new BooleanValueObject(knobMode.get().isDrumMode());
        knobMode.addValueObserver(mode -> isOnMode.set(mode.isDrumMode()));
        final SelectionSubPanel panel = optionsPanels.get(index);
        final ValueObject<SlRgbState> modeColor = new ValueObject<>(SlRgbState.YELLOW);
        final BasicStringValue typeString = new BasicStringValue("Volume");
        panel.bindVariable(new BasicStringValue(title), typeString, isOnMode, modeColor);
        
        globalStates.getHasDrumPads()
            .addValueObserver(hasDrumPads -> updateDrumModeButton(modeColor, typeString, knobMode.get()));
        knobMode.addValueObserver(newValue -> {
            updateDrumModeButton(modeColor, typeString, newValue);
        });
        
        final RgbButton button = hwElements.getSelectButtons().get(index);
        button.bindLight(
            optionButtonSelectionLayer, () -> isOnMode.get() ? modeColor.get() : modeColor.get().reduced(20));
        button.bindPressed(optionButtonSelectionLayer, () -> selectKnobMode(KnobMode.DRUM_VOLUME));
    }
    
    private void updateDrumModeButton(final ValueObject<SlRgbState> modeColor, final BasicStringValue typeString,
        final KnobMode knobMode) {
        if (globalStates.getHasDrumPads().get()) {
            if (knobMode == KnobMode.DRUM_PAN) {
                typeString.set("Pan");
                modeColor.set(SlRgbState.WHITE);
            } else if (knobMode == KnobMode.DRUM_SENDS) {
                typeString.set("Sends");
                modeColor.set(SlRgbState.WHITE);
            } else {
                typeString.set("Volume");
                modeColor.set(SlRgbState.WHITE);
            }
        } else {
            typeString.set("<No Drums>");
            modeColor.set(SlRgbState.WHITE_DIM);
        }
    }
    
    private void selectKnobMode(final KnobMode bindMode) {
        if (bindMode == KnobMode.DRUM_VOLUME) {
            if (this.knobMode.get() == KnobMode.DRUM_VOLUME) {
                this.knobMode.set(KnobMode.DRUM_PAN);
            } else if (this.knobMode.get() == KnobMode.DRUM_PAN) {
                this.knobMode.set(KnobMode.DRUM_SENDS);
            } else if (this.knobMode.get() == KnobMode.DRUM_SENDS) {
                this.knobMode.set(KnobMode.DRUM_VOLUME);
            } else {
                this.knobMode.set(bindMode);
            }
        } else {
            this.knobMode.set(bindMode);
        }
    }
    
    private void bindDrumsMix(final LayerRepo layerRepo, final ScreenHandler screenHandler) {
        final Layer drumVolumeLayer = layerRepo.getKnobLayer(KnobMode.DRUM_VOLUME);
        final Layer drumPanLayer = layerRepo.getKnobLayer(KnobMode.DRUM_PAN);
        final Layer drumSendsLayer = layerRepo.getKnobLayer(KnobMode.DRUM_SENDS);
        final ScreenSetup<KnobPanel> volumeScreen = screenHandler.getScreen(KnobMode.DRUM_VOLUME);
        final ScreenSetup<KnobPanel> panScreen = screenHandler.getScreen(KnobMode.DRUM_PAN);
        final ScreenSetup<KnobPanel> sendsScreen = screenHandler.getScreen(KnobMode.DRUM_SENDS);
        final List<SlEncoder> encoders = hwElements.getEncoders();
        final DrumPadBank padBank = viewControl.getPadBank();
        final RgbButton prevButton = hwElements.getButton(CcAssignment.SCREEN_UP);
        final RgbButton nextButton = hwElements.getButton(CcAssignment.SCREEN_DOWN);
        final SendBank sendBank = padBank.getItemAt(0).sendBank();
        final Send mainSend = sendBank.getItemAt(0);
        mainSend.name().addValueObserver(sendName -> screenHandler.getDrumSendsName().set(sendName));
        
        sendBank.canScrollForwards().markInterested();
        sendBank.canScrollBackwards().markInterested();
        prevButton.bindLight(
            drumSendsLayer, () -> sendBank.canScrollBackwards().get() ? SlRgbState.WHITE : SlRgbState.OFF);
        nextButton.bindLight(
            drumSendsLayer, () -> sendBank.canScrollForwards().get() ? SlRgbState.WHITE : SlRgbState.OFF);
        prevButton.bindRepeatHold(drumSendsLayer, () -> scrollBackwards(padBank), 500, 100);
        nextButton.bindRepeatHold(drumSendsLayer, () -> scrollForward(padBank), 500, 100);
        
        for (int i = 0; i < 8; i++) {
            final SlEncoder encoder = encoders.get(i);
            final DrumPad pad = padBank.getItemAt(i);
            
            final Send sendItem = pad.sendBank().getItemAt(0);
            encoder.bind(drumVolumeLayer, pad.volume());
            encoder.bind(drumPanLayer, pad.pan());
            encoder.bind(drumSendsLayer, sendItem);
            
            drumVolumeLayer.addBinding(
                new ParameterPanelBinding(
                    pad.volume(), volumeScreen.getPanel(i), pad.name(), SlRgbState.WHITE, trackColor));
            drumPanLayer.addBinding(
                new ParameterPanelBinding(pad.pan(), panScreen.getPanel(i), pad.name(), SlRgbState.ORANGE, trackColor));
            drumSendsLayer.addBinding(
                new ParameterPanelBinding(
                    sendItem, sendsScreen.getPanel(i), pad.name(), SlRgbState.YELLOW, trackColor));
        }
    }
    
    private void bindRemotes(final Layer layer, final ScreenSetup<KnobPanel> knobScreen,
        final CursorRemoteControlsPage remotes, final Function<Integer, SlRgbState> colorProvider) {
        final List<SlEncoder> encoders = hwElements.getEncoders();
        final RgbButton prevButton = hwElements.getButton(CcAssignment.SCREEN_UP);
        final RgbButton nextButton = hwElements.getButton(CcAssignment.SCREEN_DOWN);
        remotes.hasPrevious().markInterested();
        remotes.hasNext().markInterested();
        prevButton.bindLight(layer, () -> remotes.hasPrevious().get() ? SlRgbState.WHITE : SlRgbState.OFF);
        nextButton.bindLight(layer, () -> remotes.hasNext().get() ? SlRgbState.WHITE : SlRgbState.OFF);
        prevButton.bindRepeatHold(layer, remotes::selectPrevious, 500, 100);
        nextButton.bindRepeatHold(layer, remotes::selectNext, 500, 100);
        
        for (int i = 0; i < 8; i++) {
            final SlEncoder encoder = encoders.get(i);
            final RemoteControl parameter = remotes.getParameter(i);
            encoder.bind(layer, parameter);
            encoder.bindEmpty(layer);
            layer.addBinding(
                new ParameterPanelBinding(
                    parameter, knobScreen.getPanel(i), parameter.name(), colorProvider.apply(i), trackColor));
        }
    }
    
    private void bindPanControl(final TrackBank trackBank, final List<SlEncoder> encoders) {
        final ScreenSetup<KnobPanel> screen = screenHandler.getScreen(KnobMode.PAN);
        final RgbButton prevButton = hwElements.getButton(CcAssignment.SCREEN_UP);
        final RgbButton nextButton = hwElements.getButton(CcAssignment.SCREEN_DOWN);
        final Layer layer = layerRepo.getKnobLayer(KnobMode.PAN);
        prevButton.bindLight(layer, () -> SlRgbState.OFF);
        nextButton.bindLight(layer, () -> SlRgbState.OFF);
        prevButton.bindPressed(layer, () -> {});
        nextButton.bindPressed(layer, () -> {});
        
        for (int i = 0; i < 8; i++) {
            final Track track = trackBank.getItemAt(i);
            final SlEncoder encoder = encoders.get(i);
            encoder.bind(layer, track.pan());
            layer.addBinding(new SimpleParameterPanelBinding(track.pan(), screen.getPanel(i), SlRgbState.ORANGE));
        }
    }
    
    private void bindSendControl(final TrackBank trackBank, final List<SlEncoder> encoders) {
        final ScreenSetup<KnobPanel> screen = screenHandler.getScreen(KnobMode.SEND);
        final RgbButton prevButton = hwElements.getButton(CcAssignment.SCREEN_UP);
        final RgbButton nextButton = hwElements.getButton(CcAssignment.SCREEN_DOWN);
        final Layer layer = layerRepo.getKnobLayer(KnobMode.SEND);
        
        final SendBank sendBank = trackBank.getItemAt(0).sendBank();
        final Send mainSend = sendBank.getItemAt(0);
        mainSend.name().addValueObserver(sendName -> screenHandler.setCurrentSendsName(sendName));
        sendBank.canScrollForwards().markInterested();
        sendBank.canScrollBackwards().markInterested();
        
        prevButton.bindLight(layer, () -> sendBank.canScrollBackwards().get() ? SlRgbState.WHITE : SlRgbState.OFF);
        nextButton.bindLight(layer, () -> sendBank.canScrollForwards().get() ? SlRgbState.WHITE : SlRgbState.OFF);
        prevButton.bindRepeatHold(layer, () -> scrollBackwards(trackBank), 500, 100);
        nextButton.bindRepeatHold(layer, () -> scrollForward(trackBank), 500, 100);
        
        for (int i = 0; i < 8; i++) {
            final Track track = trackBank.getItemAt(i);
            final SlEncoder encoder = encoders.get(i);
            final Send sendItem = track.sendBank().getItemAt(0);
            encoder.bind(layer, sendItem);
            layer.addBinding(new SimpleParameterPanelBinding(sendItem, screen.getPanel(i), SlRgbState.YELLOW));
        }
    }
    
    private void scrollForward(final TrackBank trackBank) {
        for (int i = 0; i < 8; i++) {
            trackBank.getItemAt(i).sendBank().scrollForwards();
        }
    }
    
    private void scrollBackwards(final TrackBank trackBank) {
        for (int i = 0; i < 8; i++) {
            trackBank.getItemAt(i).sendBank().scrollBackwards();
        }
    }
    
    private void scrollForward(final DrumPadBank drumPadBank) {
        for (int i = 0; i < drumPadBank.getSizeOfBank(); i++) {
            drumPadBank.getItemAt(i).sendBank().scrollForwards();
        }
    }
    
    private void scrollBackwards(final DrumPadBank drumPadBank) {
        for (int i = 0; i < drumPadBank.getSizeOfBank(); i++) {
            drumPadBank.getItemAt(i).sendBank().scrollBackwards();
        }
    }
    
    
    private void handleTrackPressed(final Track track) {
        if (globalStates.getDuplicateState().get()) {
            track.duplicate();
        } else if (globalStates.getClearState().get()) {
            track.deleteObject();
        } else {
            track.selectInMixer();
        }
    }
    
    private void setupShiftLayer(final SlMk3HardwareElements hwElements) {
        final ButtonSubPanel shiftPanels = screenHandler.getSubPanel(ButtonMode.SHIFT);
        final Layer shiftButtonSelectionLayer = layerRepo.getButtonLayer(ButtonMode.SHIFT);
        for (int i = 0; i < 8; i++) {
            final RgbButton selectButton = hwElements.getSelectButtons().get(i);
            final ShiftAction action = i < ShiftAction.values().length ? ShiftAction.values()[i] : null;
            final SelectionSubPanel panel = shiftPanels.get(i);
            if (action != null) {
                switch (action) {
                    case UNDO -> assignAction(
                        i, action, selectButton, application.canUndo(), application::undo,
                        SlRgbState.BITWIG_ORANGE);
                    case REDO -> assignAction(
                        i, action, selectButton, application.canRedo(), application::redo,
                        SlRgbState.BITWIG_ORANGE);
                    case CLICK -> assignToggleValue(
                        i, action, selectButton, transport.isMetronomeEnabled(),
                        SlRgbState.BITWIG_ORANGE);
                    case CL_OVERDUB ->
                        assignToggleValue(
                            i, action, selectButton, transport.isClipLauncherOverdubEnabled(),
                            SlRgbState.BITWIG_ORANGE);
                    case CL_AUTO ->
                        assignToggleValue(
                            i, action, selectButton, transport.isClipLauncherAutomationWriteEnabled(),
                            SlRgbState.BITWIG_ORANGE);
                    case AR_AUTO ->
                        assignToggleValue(
                            i, action, selectButton, transport.isArrangerAutomationWriteEnabled(),
                            SlRgbState.BITWIG_ORANGE);
                    case AUTO_OVERRIDE ->
                        assignAction(
                            i, action, selectButton, transport.isAutomationOverrideActive(),
                            transport::resetAutomationOverrides, SlRgbState.BITWIG_ORANGE);
                    case FILL -> assignToggleValue(
                        i, action, selectButton, transport.isFillModeActive(),
                        SlRgbState.BITWIG_ORANGE);
                }
            } else {
                panel.setRow1("".formatted(i + 1));
                panel.setRow2("".formatted(i + 1));
                selectButton.bindLight(shiftButtonSelectionLayer, () -> SlRgbState.OFF);
            }
        }
    }
    
    private void assignAction(final int index, final ShiftAction action, final RgbButton selectButton,
        final BooleanValue stateValue, final Runnable pressAction, final SlRgbState color) {
        final SlRgbState onColor = color;
        final SlRgbState offColor = color.reduced(20);
        final SelectionSubPanel panel = screenHandler.getSubPanel(ButtonMode.SHIFT).get(index);
        final Layer layer = layerRepo.getButtonLayer(ButtonMode.SHIFT);
        panel.bindValue(action.getRow1(), action.getRow2(), stateValue, onColor, onColor);
        selectButton.bindPressed(layer, pressAction);
        selectButton.bindLight(layer, () -> stateValue.get() ? onColor : offColor);
    }
    
    private void assignToggleValue(final int index, final ShiftAction action, final RgbButton selectButton,
        final SettableBooleanValue value, final SlRgbState color) {
        final SlRgbState onColor = color;
        final SlRgbState offColor = color.reduced(20);
        final SelectionSubPanel panel = screenHandler.getSubPanel(ButtonMode.SHIFT).get(index);
        final Layer layer = layerRepo.getButtonLayer(ButtonMode.SHIFT);
        panel.bindValueWithSelect(action.getRow1(), action.getRow2(), value, onColor, onColor);
        selectButton.bindPressed(layer, () -> value.toggle());
        selectButton.bindLight(layer, () -> value.get() ? onColor : offColor);
        selectButton.bindLightOnPressed(layer, color, value);
    }
    
    private void handleTrackSelectionChanged(final int value) {
        final ButtonSubPanel trackPanel = screenHandler.getSubPanel(ButtonMode.TRACK);
        for (int i = 0; i < 8; i++) {
            trackPanel.get(i).setSelected(value == i);
        }
    }
    
    private SlRgbState getTrackColor(final int index, final Track track) {
        if (!track.exists().get()) {
            return SlRgbState.OFF;
        }
        if (index == viewControl.getSelectedTrackIndex().get()) {
            return trackColors[index];
        }
        return SlRgbState.WHITE_DIM;
    }
    
    @Activate
    public void doActivate() {
        layerRepo.updateKnobModeLayer(knobMode.get());
        layerRepo.applySelectButtonLayer();
        layerRepo.getPadDeviceSelectionLayer()
            .setIsActive(this.globalStates.getBaseMode().get() == GridMode.OPTION && knobMode.get() == KnobMode.DEVICE);
    }
    
    private void applyMode() {
        final GridMode gridMode = globalStates.getBaseMode().get();
        if (globalStates.getShiftState().get() && gridMode == GridMode.OPTION_SHIFT
            && knobMode.get() != KnobMode.OPTION_SHIFT) {
            stashedKnobMode = knobMode.get();
            knobMode.setDirect(KnobMode.OPTION_SHIFT);
        } else if (gridMode != GridMode.OPTION_SHIFT && stashedKnobMode != null) {
            knobMode.setDirect(stashedKnobMode);
            stashedKnobMode = null;
        }
        
        if (globalStates.getShiftState().get()) {
            layerRepo.applySelectButtonLayer(ButtonMode.SHIFT);
        } else if (gridMode == GridMode.OPTION) {
            layerRepo.applySelectButtonLayer(ButtonMode.OPTION);
        } else {
            layerRepo.applySelectButtonLayer(ButtonMode.TRACK);
        }
        layerRepo.applyPadLayer(gridMode, knobMode.get());
        if (gridMode == GridMode.SEQUENCER || gridMode == GridMode.SELECT) {
            layerRepo.updateKnobModeLayer(KnobMode.SEQUENCER);
            if (globalStates.getShiftState().get()) {
                layerRepo.applySelectButtonLayer(ButtonMode.SHIFT);
            } else {
                layerRepo.applySelectButtonLayer(
                    this.globalStates.getSequencerSubMode().get() == SequencerButtonSubMode.MODE_1
                        ? ButtonMode.SEQUENCER
                        : ButtonMode.SEQUENCER2);
            }
        } else {
            layerRepo.updateKnobModeLayer(knobMode.get());
        }
        screenHandler.setMode(knobMode.get(), gridMode, globalStates.getShiftState().get());
    }
    
}