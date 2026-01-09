package com.bitwig.extensions.controllers.akai.mpkmk4.layers;

import java.util.List;

import com.bitwig.extension.controller.api.SendBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkHwElements;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkMidiProcessor;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkViewControl;
import com.bitwig.extensions.controllers.akai.mpkmk4.bindings.ParameterDisplayBinding;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.Encoder;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.LineDisplay;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.ParameterValues;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class MixEncoderLayer extends EncoderLayer {
    
    private final TrackBank trackBank;
    private final Layer sendsLayer;
    private final ParameterValues sendParameterValues;
    private int sendScrollPos;
    private final SendBank sendBank;
    private final LineDisplay display;
    
    public MixEncoderLayer(final Layers layers, final MpkHwElements hwElements, final MpkMidiProcessor midiProcessor,
        final MpkViewControl viewControl) {
        super("GRID_MIXER", layers, midiProcessor);
        trackBank = viewControl.getTrackBank();
        final List<Encoder> encoders = hwElements.getEncoders();
        sendsLayer = new Layer(layers, "GRID_SENDS");
        this.sendParameterValues = new ParameterValues(midiProcessor);
        sendBank = trackBank.getItemAt(0).sendBank();
        sendBank.scrollPosition().addValueObserver(this::handleScrollPos);
        display = hwElements.getMainLineDisplay();
        
        for (int i = 0; i < 4; i++) {
            final Track track = trackBank.getItemAt(i);
            final Encoder encoderPan = encoders.get(i);
            encoderPan.bindValue(this, track.pan());
            this.addBinding(new ParameterDisplayBinding(track.pan(), encoderPan, parameterValues, i));
            final Encoder encoderVolume = encoders.get(i + 4);
            encoderVolume.bindValue(this, track.volume());
            this.addBinding(new ParameterDisplayBinding(track.volume(), encoderVolume, parameterValues, i + 4));
            
            final SendBank sendsBank = track.sendBank();
            sendsBank.canScrollForwards().markInterested();
            sendsBank.canScrollBackwards().markInterested();
            
            encoderPan.bindValue(sendsLayer, sendsBank.getItemAt(0));
            sendsLayer.addBinding(
                new ParameterDisplayBinding(sendsBank.getItemAt(0), encoderPan, sendParameterValues, i));
            encoderVolume.bindValue(sendsLayer, sendsBank.getItemAt(1));
            sendsLayer.addBinding(
                new ParameterDisplayBinding(sendsBank.getItemAt(1), encoderVolume, sendParameterValues, i));
        }
    }
    
    private void handleScrollPos(final int scrollPos) {
        this.sendScrollPos = scrollPos;
        if (isSendsActive()) {
            display.temporaryInfo(1, "Mixer", getSendScrollInfo());
        }
    }
    
    public String getSendScrollInfo() {
        //return "Sends %d-%d".formatted(sendScrollPos + 1, sendScrollPos + 2);
        final String name1 = sendBank.getItemAt(0).name().get();
        final String name2 = sendBank.getItemAt(1).name().get();
        return "%s/%s".formatted(name1, name2);
    }
    
    public void toggleSends() {
        sendsLayer.toggleIsActive();
        if (sendsLayer.isActive()) {
            display.temporaryInfo(1, "Mixer Sends", getSendScrollInfo());
        } else {
            display.temporaryInfo(1, "Mixer", "Pan/Volume");
        }
    }
    
    public boolean isSendsActive() {
        return sendsLayer.isActive();
    }
    
    @Override
    public void navigateLeft() {
        if (sendsLayer.isActive()) {
            for (int i = 0; i < trackBank.getSizeOfBank(); i++) {
                trackBank.getItemAt(i).sendBank().scrollBackwards();
            }
        }
    }
    
    @Override
    public void navigateRight() {
        if (sendsLayer.isActive()) {
            for (int i = 0; i < trackBank.getSizeOfBank(); i++) {
                trackBank.getItemAt(i).sendBank().scrollForwards();
            }
        }
    }
    
    @Override
    public boolean canScrollRight() {
        if (sendsLayer.isActive()) {
            return sendBank.canScrollForwards().get();
        }
        
        return false;
    }
    
    @Override
    public boolean canScrollLeft() {
        if (sendsLayer.isActive()) {
            return sendBank.canScrollBackwards().get();
        }
        return false;
    }
    
    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        sendsLayer.setIsActive(false);
    }
}
