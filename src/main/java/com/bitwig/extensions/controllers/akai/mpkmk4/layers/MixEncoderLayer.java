package com.bitwig.extensions.controllers.akai.mpkmk4.layers;

import java.util.List;

import com.bitwig.extension.controller.api.SendBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkHwElements;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkMidiProcessor;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkViewControl;
import com.bitwig.extensions.controllers.akai.mpkmk4.bindings.ParameterContextBinding;
import com.bitwig.extensions.controllers.akai.mpkmk4.bindings.ParameterDisplayBinding;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.Encoder;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.LineDisplay;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.MpkColorLookup;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.ParameterValues;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.StringUtil;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.TemporaryLineValue;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class MixEncoderLayer extends EncoderLayer {
    
    private final TrackBank trackBank;
    private final Layer mixingLayer;
    private final Layer sendsLayer;
    private final ParameterValues sendParameterValues;
    private int sendScrollPos;
    private final SendBank sendBank;
    private final LineDisplay display;
    private LayerMode mode = LayerMode.MIXER;
    
    private enum LayerMode {
        MIXER,
        SENDS;
    }
    
    public MixEncoderLayer(final Layers layers, final MpkHwElements hwElements, final MpkMidiProcessor midiProcessor,
        final MpkViewControl viewControl) {
        super("GRID_MIXER", layers, midiProcessor);
        trackBank = viewControl.getTrackBank();
        final List<Encoder> encoders = hwElements.getEncoders();
        mixingLayer = new Layer(layers, "MIXING_LAYER");
        sendsLayer = new Layer(layers, "GRID_SENDS");
        this.sendParameterValues = new ParameterValues(midiProcessor);
        sendBank = trackBank.getItemAt(0).sendBank();
        sendBank.scrollPosition().addValueObserver(this::handleScrollPos);
        display = hwElements.getMainLineDisplay();
        
        for (int i = 0; i < 4; i++) {
            final Track track = trackBank.getItemAt(i);
            final Encoder encoderPan = encoders.get(i);
            encoderPan.bindValue(mixingLayer, track.pan());
            mixingLayer.addBinding(new ParameterDisplayBinding(track.pan(), encoderPan, parameterValues, i));
            final Encoder encoderVolume = encoders.get(i + 4);
            encoderVolume.bindValue(mixingLayer, track.volume());
            mixingLayer.addBinding(new ParameterDisplayBinding(track.volume(), encoderVolume, parameterValues, i + 4));
            
            final TemporaryLineValue lineDisplay = new TemporaryLineValue(midiProcessor);
            mixingLayer.addBinding(new ParameterContextBinding(encoderVolume, lineDisplay));
            mixingLayer.addBinding(new ParameterContextBinding(encoderPan, lineDisplay));
            
            track.color().addValueObserver((r, g, b) -> lineDisplay.setColorIndex(MpkColorLookup.rgbToIndex(r, g, b)));
            track.name().addValueObserver(name -> lineDisplay.setValue(name));
            
            final SendBank sendsBank = track.sendBank();
            sendsBank.canScrollForwards().markInterested();
            sendsBank.canScrollBackwards().markInterested();
            
            encoderPan.bindValue(sendsLayer, sendsBank.getItemAt(0));
            sendsLayer.addBinding(
                new ParameterDisplayBinding(sendsBank.getItemAt(0), encoderPan, sendParameterValues, i));
            
            encoderVolume.bindValue(sendsLayer, sendsBank.getItemAt(1));
            sendsLayer.addBinding(
                new ParameterDisplayBinding(sendsBank.getItemAt(1), encoderVolume, sendParameterValues, i + 4));
            
            sendsLayer.addBinding(new ParameterContextBinding(encoderVolume, lineDisplay));
            sendsLayer.addBinding(new ParameterContextBinding(encoderPan, lineDisplay));
        }
    }
    
    private void handleScrollPos(final int scrollPos) {
        this.sendScrollPos = scrollPos;
        if (isSendsActive()) {
            display.temporaryInfo(1, "Mixer", getSendScrollInfo());
        }
    }
    
    public String getSendScrollInfo() {
        final String name1 = sendBank.getItemAt(0).name().get();
        final String name2 = sendBank.getItemAt(1).name().get();
        return "%s/%s".formatted(StringUtil.shorten(name1, 6), StringUtil.shorten(name2, 6));
    }
    
    public void toggleSends() {
        this.mode = mode == LayerMode.MIXER ? LayerMode.SENDS : LayerMode.MIXER;
        sendsLayer.setIsActive(mode == LayerMode.SENDS);
        mixingLayer.setIsActive(mode == LayerMode.MIXER);
        
        updateDisplay();
        if (mode == LayerMode.SENDS) {
            display.temporaryInfo(1, "Mixer Sends", getSendScrollInfo());
        } else {
            display.temporaryInfo(1, "Mixer", "Pan/Volume");
        }
    }
    
    public boolean isSendsActive() {
        return mode == LayerMode.SENDS;
    }
    
    @Override
    public void navigateLeft() {
        if (mode == LayerMode.SENDS) {
            if (sendScrollPos == 0) {
                toggleSends();
            } else {
                for (int i = 0; i < trackBank.getSizeOfBank(); i++) {
                    trackBank.getItemAt(i).sendBank().scrollBackwards();
                }
            }
        }
    }
    
    @Override
    public void navigateRight() {
        if (mode == LayerMode.SENDS) {
            for (int i = 0; i < trackBank.getSizeOfBank(); i++) {
                trackBank.getItemAt(i).sendBank().scrollForwards();
            }
        } else {
            toggleSends();
        }
    }
    
    @Override
    public boolean canScrollRight() {
        if (mode == LayerMode.SENDS) {
            return sendBank.canScrollForwards().get();
        }
        return true;
    }
    
    @Override
    public boolean canScrollLeft() {
        return sendsLayer.isActive();
    }
    
    @Override
    protected void onActivate() {
        updateDisplay();
        if (mode == LayerMode.MIXER) {
            mixingLayer.setIsActive(true);
        } else {
            sendsLayer.setIsActive(true);
        }
    }
    
    public void updateDisplay() {
        display.setText(1, "Mixer");
        display.setText(
            2, sendsLayer.isActive() ? "Sends %d/%d".formatted(sendScrollPos + 1, sendScrollPos + 2) : "Pan/Volume");
    }
    
    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        sendsLayer.setIsActive(false);
        mixingLayer.setIsActive(false);
    }
    
}
