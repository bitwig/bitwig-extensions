package com.bitwig.extensions.controllers.arturia.keylab.essentialMk3;

import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.color.RgbLightState;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.components.HwElements;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.components.KeyLabEncoderBinding;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.components.KeylabAbsoluteControl;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.components.ViewControl;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.display.LcdDisplay;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.display.LcdDisplayMode;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.values.ValueObject;

public class SliderEncoderControl extends Layer {
    private final ViewControl viewControl;
    private final Layer deviceControlLayer;
    private final Layer deviceControlLayer2;
    private final Layer partModifierLayer;
    private final LcdDisplay lcdDisplay;
    private CursorRemoteControlsPage parameterBank1;
    private String[] devicePageNames = new String[0];
    private TrackBank mixerTrackBank;
    private boolean deviceScrollingOccurred = false;
    private boolean parameterSteppingOccurred = false;
    private boolean trackBankScrollingOccurred = false;
    
    public enum State {
        MIXER,
        DEVICE
    }
    
    private final ValueObject<State> currentState = new ValueObject<>(State.MIXER);
    
    public SliderEncoderControl(final Layers layers, final ViewControl viewControl, final HwElements hwElements,
        final LcdDisplay lcdDisplay) {
        super(layers, "SLIDER_ENCODER_LAYER");
        this.viewControl = viewControl;
        deviceControlLayer = new Layer(layers, "DEVICE_CONTROL");
        deviceControlLayer2 = new Layer(layers, "DEVICE_CONTROL2");
        // TODO figure out how to give this highest Priority
        partModifierLayer = new Layer(layers, "PART_MODIFIER");
        this.lcdDisplay = lcdDisplay;
        initPartButton(hwElements);
        assignMixLayer(hwElements);
    }
    
    @Activate
    public void doActivation() {
        activate();
    }
    
    private void assignMixLayer(final HwElements hwElements) {
        final PinnableCursorDevice cursorDevice = viewControl.getCursorDevice();
        cursorDevice.name().addValueObserver(name -> {
            if (deviceScrollingOccurred) {
                lcdDisplay.sendPopup(
                    "Device Selected", //
                    name, KeylabIcon.SFX_SMALL);
            }
        });
        
        parameterBank1 = cursorDevice.createCursorRemoteControlsPage(8);
        cursorDevice.hasPrevious().markInterested();
        
        final CursorRemoteControlsPage parameterBank2 = cursorDevice.createCursorRemoteControlsPage("sliders", 8, null);
        parameterBank2.selectedPageIndex().markInterested();
        parameterBank1.pageCount().addValueObserver(pages -> {
            if (deviceControlLayer.isActive()) {
                deviceControlLayer2.setIsActive(pages < 2);
            }
        });
        parameterBank1.pageNames().addValueObserver(pageNames -> {
            devicePageNames = pageNames;
            if (devicePageNames != null && devicePageNames.length > 0
                && parameterBank1.selectedPageIndex().get() == -1) {
                mainParameterBankIndexChanged(parameterBank2, parameterBank1.selectedPageIndex().get());
            }
        });
        parameterBank1.selectedPageIndex()
            .addValueObserver(index -> mainParameterBankIndexChanged(parameterBank2, index));
        
        mixerTrackBank = viewControl.getMixerTrackBank();
        mixerTrackBank.itemCount().markInterested();
        mixerTrackBank.scrollPosition().addValueObserver(scrollPosition ->  //
            lcdDisplay.sendPopup(
                "Tracks", String.format(
                    "%d - %d", scrollPosition + 1,
                    Math.min(scrollPosition + 8, mixerTrackBank.itemCount().get())), KeylabIcon.NONE));
        
        cursorDevice.name().markInterested();
        
        final KeylabAbsoluteControl[] sliders = hwElements.getSliders();
        final KeylabAbsoluteControl[] knobs = hwElements.getKnobs();
        
        for (int i = 0; i < 8; i++) {
            final Track track = mixerTrackBank.getItemAt(i);
            addBinding(
                new KeyLabEncoderBinding(sliders[i], track.volume(), track.name(), LcdDisplayMode.VOLUME, lcdDisplay));
            addBinding(
                new KeyLabEncoderBinding(knobs[i], track.pan(), track.name(), LcdDisplayMode.PANNING, lcdDisplay));
        }
        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        addBinding(new KeyLabEncoderBinding(
            sliders[8], cursorTrack.volume(), cursorTrack.name(), LcdDisplayMode.VOLUME,
            lcdDisplay));
        addBinding(new KeyLabEncoderBinding(
            knobs[8], cursorTrack.pan(), cursorTrack.name(), LcdDisplayMode.PANNING,
            lcdDisplay));
        
        for (int i = 0; i < 8; i++) {
            final RemoteControl parameter4Knob = parameterBank1.getParameter(i);
            deviceControlLayer.addBinding(
                new KeyLabEncoderBinding(
                    knobs[i], parameter4Knob, parameter4Knob.name(), LcdDisplayMode.DEVICE1, lcdDisplay));
            deviceControlLayer2.addBinding(
                new KeyLabEncoderBinding(
                    sliders[i], parameter4Knob, parameter4Knob.name(), LcdDisplayMode.DEVICE2, lcdDisplay));
            
            final RemoteControl parameter4Slider = parameterBank2.getParameter(i);
            deviceControlLayer.addBinding(
                new KeyLabEncoderBinding(
                    sliders[i], parameter4Slider, parameter4Slider.name(), LcdDisplayMode.DEVICE2, lcdDisplay));
        }
    }
    
    private void mainParameterBankIndexChanged(final CursorRemoteControlsPage parameterBank2, final int index) {
        if (index == -1 || devicePageNames.length == 0) {
            return;
        }
        parameterBank2.selectedPageIndex().set((index + 1) % devicePageNames.length);
        if (parameterSteppingOccurred && !deviceScrollingOccurred) {
            lcdDisplay.sendPopup(
                getParameterPageLabeling(index), //
                "Parameter Page " + (index + 1), KeylabIcon.SFX_SMALL);
            parameterSteppingOccurred = false;
        }
    }
    
    private String getParameterPageLabeling(final int index) {
        if (index >= 0 && index < devicePageNames.length) {
            final String pageName = devicePageNames[index];
            final String pageKnob = shorten(pageName);
            final String pageFader = shorten(devicePageNames[(index + 1) % devicePageNames.length]);
            final String result = pageKnob + "/" + pageFader;
            if (result.length() > 15) {
                return result.substring(0, 15);
            }
            return result;
        }
        return "";
    }
    
    private String shorten(final String value) {
        return value.replace(" ", "").replace("-", "");
    }
    
    public ValueObject<State> getCurrentState() {
        return currentState;
    }
    
    private void initPartButton(final HwElements hwElements) {
        final RgbButton partButton = hwElements.getButton(CCAssignment.PART);
        partButton.bindPressed(this, this::handlePartDown);
        partButton.bindReleased(this, () -> handlePartRelease(partButton));
        partButton.bindLight(
            this,
            () -> mixerTrackBank.scrollPosition().get() == 0 ? RgbLightState.WHITE_DIMMED : RgbLightState.WHITE);
        partButton.bindLight(
            deviceControlLayer,
            () -> parameterBank1.selectedPageIndex().get() == 0 ? RgbLightState.WHITE_DIMMED : RgbLightState.WHITE);
        final RelativeHardwareKnob encoder = hwElements.getMainEncoder();
        hwElements.bindEncoder(partModifierLayer, encoder, this::handlePartEncoder);
    }
    
    private void handlePartEncoder(final int increment) {
        if (deviceControlLayer.isActive()) {
            final PinnableCursorDevice cursorDevice = viewControl.getCursorDevice();
            if (increment > 0) {
                cursorDevice.selectNext();
            } else {
                cursorDevice.selectPrevious();
            }
            deviceScrollingOccurred = true;
        } else {
            mixerTrackBank.scrollBy(increment);
            trackBankScrollingOccurred = true;
        }
    }
    
    public void handlePartDown() {
        deviceScrollingOccurred = false;
        parameterSteppingOccurred = false;
        partModifierLayer.setIsActive(true);
    }
    
    public void handlePartRelease(final RgbButton button) {
        partModifierLayer.setIsActive(false);
        if (!deviceScrollingOccurred) {
            parameterSteppingOccurred = true;
            if (currentState.get() == State.DEVICE) {
                parameterBank1.selectNextPage(true);
            } else if (!trackBankScrollingOccurred) {
                if (mixerTrackBank.scrollPosition().get() > 0) {
                    mixerTrackBank.scrollPosition().set(0);
                } else {
                    mixerTrackBank.scrollPosition().set(8);
                }
            }
        }
        trackBankScrollingOccurred = false;
        button.forceDelayedRefresh();
    }
    
    public void toggleMode() {
        if (!deviceControlLayer.isActive()) {
            deviceControlLayer.setIsActive(true);
            deviceControlLayer2.setIsActive(parameterBank1.pageCount().get() < 2);
            currentState.set(State.DEVICE);
        } else {
            deviceControlLayer.setIsActive(false);
            deviceControlLayer2.setIsActive(false);
            currentState.set(State.MIXER);
        }
    }
}
