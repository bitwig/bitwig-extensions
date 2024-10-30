package com.bitwig.extensions.controllers.mcu.layer;

import com.bitwig.extension.controller.api.Send;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.mcu.GlobalStates;
import com.bitwig.extensions.controllers.mcu.StringUtil;
import com.bitwig.extensions.controllers.mcu.bindings.VuMeterBinding;
import com.bitwig.extensions.controllers.mcu.bindings.display.DisplayTarget;
import com.bitwig.extensions.controllers.mcu.bindings.display.StringDisplayBinding;
import com.bitwig.extensions.controllers.mcu.control.MixerSectionHardware;
import com.bitwig.extensions.controllers.mcu.control.MotorSlider;
import com.bitwig.extensions.controllers.mcu.control.RingDisplayType;
import com.bitwig.extensions.controllers.mcu.control.RingEncoder;
import com.bitwig.extensions.controllers.mcu.display.DisplayManager;
import com.bitwig.extensions.controllers.mcu.display.DisplayRow;
import com.bitwig.extensions.controllers.mcu.value.IEnumDisplayValue;
import com.bitwig.extensions.controllers.mcu.value.SettableEnumValueSelect;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Context;
import com.bitwig.extensions.framework.values.BasicStringValue;

public class MixingModeLayerCollection {
    private final static SettableEnumValueSelect.Value[] PRE_POST_VALUES = new SettableEnumValueSelect.Value[] {
        new SettableEnumValueSelect.Value("PRE", "PRE", 0), //
        new SettableEnumValueSelect.Value("POST", "POST", 5), //
        new SettableEnumValueSelect.Value("AUTO", "AUTO", 11)
    };
    
    private final ModeLayerGroup volumeLayer;
    private final ModeLayerGroup panLayer;
    private final ModeLayerGroup sendsLayer;
    private final LayerGroup sendsPrePostLayer;
    private final Layer trackDisplayLayer;
    private final Layer buttonLayer;
    private final Layer vuLayer;
    private final GlobalStates globalStates;
    private final TrackBank trackBank;
    private int selectedTrackIndex;
    private final boolean isExtended;
    private final int trackOffset;
    private final int sectionIndex;
    
    public MixingModeLayerCollection(final Context context, final TrackBank trackBank, final boolean extended,
        final int sectionIndex) {
        final Layers layers = context.getService(Layers.class);
        this.globalStates = context.getService(GlobalStates.class);
        this.isExtended = extended;
        this.trackOffset = sectionIndex * 8;
        this.sectionIndex = sectionIndex;
        volumeLayer = new ModeLayerGroup(ControlMode.VOLUME, layers, sectionIndex);
        panLayer = new ModeLayerGroup(ControlMode.PAN, layers, sectionIndex);
        sendsLayer = new ModeLayerGroup(ControlMode.SENDS, layers, sectionIndex);
        sendsPrePostLayer = new LayerGroup(context, "PRE_POST");
        trackDisplayLayer = new Layer(layers, "TRACK_DISPLAY");
        buttonLayer = new Layer(layers, "BUTTON_LAYER");
        vuLayer = new Layer(layers, "VU_LAYER");
        this.trackBank = trackBank;
        trackBank.scrollPosition().markInterested();
        for (int i = 0; i < trackBank.getSizeOfBank(); i++) {
            configureTrack(i);
        }
    }
    
    private void configureTrack(final int index) {
        //        if (usesColor) {
        //            track.color().addValueObserver((r, g, b) -> trackColors[index] = toColor(r, g, b));
        //        }
        // TODO needs to move to a different collection
        final Track track = trackBank.getItemAt(index);
        track.addIsSelectedInMixerObserver(select -> {
            if (select) {
                this.selectedTrackIndex = index;
            } else if (this.selectedTrackIndex == index) {
                this.selectedTrackIndex = -1;
            }
        });
    }
    
    public ModeLayerGroup get(final ControlMode mode) {
        return switch (mode) {
            case PAN -> panLayer;
            case SENDS -> sendsLayer;
            default -> volumeLayer;
        };
    }
    
    public Layer getTrackDisplayLayer() {
        return trackDisplayLayer;
    }
    
    public void setIsActive(final boolean active) {
        buttonLayer.setIsActive(active);
        vuLayer.setIsActive(active);
    }
    
    public void bind(final MixerSectionHardware hwElements, final DisplayManager displayManager,
        final int channelOffset, final boolean hasDedicatedVu) {
        for (int i = 0; i < 8; i++) {
            final int index = i;
            final Track track = trackBank.getItemAt(i + channelOffset);
            
            final MotorSlider slider = hwElements.getSlider(i);
            final RingEncoder encoder = hwElements.getRingEncoder(i);
            
            volumeLayer.bindControls(slider, encoder, RingDisplayType.FILL_LR, track.volume());
            volumeLayer.bindDisplay(
                displayManager, new BasicStringValue("LEVEL"), track.exists(), track.volume(), index);
            panLayer.bindControls(slider, encoder, RingDisplayType.PAN_FILL, track.pan());
            panLayer.bindValue(displayManager, new BasicStringValue(" PAN"), track.exists(), track.pan(), index,
                v -> StringUtil.panToString(v));
            final Send sendItem = track.sendBank().getItemAt(0);
            
            sendsLayer.bindControls(slider, encoder, RingDisplayType.FILL_LR, sendItem);
            sendsLayer.bindDisplay(displayManager, sendItem.name(), track.exists(), sendItem, index);
            
            // ENUM track.monitorMode()
            
            bindSendPrePost(sendsPrePostLayer, hwElements, displayManager, index, sendItem);
            
            final BasicStringValue trackName = setUpTrackNameAggregate(track);
            trackDisplayLayer.addBinding(new StringDisplayBinding(displayManager, ControlMode.VOLUME,
                DisplayTarget.of(DisplayRow.LABEL, index, sectionIndex, trackName), trackName, track.exists(),
                name -> StringUtil.reduceAscii(name, 7)));
            
            assignButtons(hwElements, i, track);
            if (hasDedicatedVu) {
                vuLayer.addBinding(new VuMeterBinding(displayManager, track, index));
            }
        }
    }
    
    public static void bindSendPrePost(final LayerGroup layerGroup, final MixerSectionHardware hwElements,
        final DisplayManager displayManager, final int index, final Send sendItem) {
        final IEnumDisplayValue sendMode = new SettableEnumValueSelect(sendItem.sendMode(), sendItem, PRE_POST_VALUES);
        layerGroup.bindDisplay(displayManager, sendItem.name(), sendMode.getDisplayValue(), index);
        layerGroup.bindEncoderIncrement(hwElements.getRingEncoder(index), sendMode::increment, 0.1);
        layerGroup.bindEncoderPressed(hwElements.getRingEncoder(index), sendMode::stepRoundRobin);
        layerGroup.bindRingValue(hwElements.getRingEncoder(index), sendMode.getRingValue());
    }
    
    private BasicStringValue setUpTrackNameAggregate(final Track track) {
        final BasicStringValue name = new BasicStringValue("");
        track.name().addValueObserver(
            trackName -> name.set(toTrackName(trackName, track.isGroup().get(), track.isGroupExpanded().get())));
        track.isGroup().addValueObserver(
            isGroup -> name.set(toTrackName(track.name().get(), isGroup, track.isGroupExpanded().get())));
        track.isGroupExpanded()
            .addValueObserver(expanded -> name.set((toTrackName(track.name().get(), track.isGroup().get(), expanded))));
        return name;
    }
    
    private void assignButtons(final MixerSectionHardware hwElements, final int index, final Track track) {
        hwElements.getMuteButton(index).bindToggle(buttonLayer, track.mute());
        hwElements.getArmButton(index).bindToggle(buttonLayer, track.arm());
        hwElements.getSoloButton(index).bindLight(buttonLayer, track.solo());
        hwElements.getSoloButton(index).bindIsPressed(buttonLayer, pressed -> handleSolo(pressed, track));
        hwElements.getSelectButton(index).bindPressed(buttonLayer, () -> handleSelect(track));
        hwElements.getSelectButton(index).bindLight(buttonLayer, () -> isTrackSelected(index + trackOffset));
    }
    
    private String toTrackName(final String name, final boolean isGroup, final boolean isExpanded) {
        if (isGroup) {
            return (isExpanded ? ">" : "_") + name;
        }
        return name;
    }
    
    private void handleSolo(final boolean pressed, final Track track) {
        if (pressed) {
            track.solo().toggleUsingPreferences((globalStates.isSoloHeld() || globalStates.getShift().get()));
        }
        globalStates.soloPressed(pressed);
    }
    
    private void handleSelect(final Track track) {
        if (globalStates.getClearHeld().get()) {
            track.deleteObject();
        } else if (globalStates.getDuplicateHeld().get()) {
            track.duplicate();
        } else if (globalStates.isShiftSet()) {
            track.isGroupExpanded().toggle();
        } else {
            track.selectInMixer();
        }
    }
    
    private boolean isTrackSelected(final int index) {
        return index == selectedTrackIndex;
    }
    
    public LayerGroup getSendsPrePostLayer() {
        return sendsPrePostLayer;
    }
}
