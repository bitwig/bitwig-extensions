package com.bitwig.extensions.controllers.allenheath.xonek3;

import java.util.UUID;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DeviceMatcher;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.SpecificBitwigDevice;
import com.bitwig.extension.controller.api.Track;

public class TrackSpecControl {
    private static final UUID DJ_EQ_DEVICE_ID = UUID.fromString("3cc1b71a-e22a-42cf-89f0-316475368fb3");
    private static final UUID LIMITER_ID = UUID.fromString("8da7251e-2578-4bcc-b3c4-8f4ec2e115d0");
    
    private final Track track;
    private final Device djEqDevice;
    private final Device limiterDevice;
    private boolean djEqExists;
    private boolean djEqActive;
    private final Parameter lowGain;
    private final Parameter midGain;
    private final Parameter hiGain;
    private final Parameter lowKill;
    private final Parameter midKill;
    private final Parameter hiKill;
    private final CursorDevice cursorDevice;
    private final Parameter lowFreq;
    private final Parameter hiFreq;
    private final CursorRemoteControlsPage trackRemotes;
    private boolean trackExists = false;
    
    public TrackSpecControl(final int index, final Track track, final ControllerHost host) {
        this.track = track;
        cursorDevice = track.createCursorDevice();
        track.exists().addValueObserver(exists -> this.trackExists = exists);
        
        trackRemotes = track.createCursorRemoteControlsPage(8);
        djEqDevice = createSpecDevice(host.createBitwigDeviceMatcher(DJ_EQ_DEVICE_ID));
        limiterDevice = createSpecDevice(host.createBitwigDeviceMatcher(LIMITER_ID));
        djEqDevice.exists().addValueObserver(exists -> this.djEqExists = exists);
        djEqDevice.isEnabled().addValueObserver(active -> this.djEqActive = active);
        
        // CONTENTS/LOW_FREQ, CONTENTS/HIGH_FREQ,
        final SpecificBitwigDevice djSpecificDevice = djEqDevice.createSpecificBitwigDevice(DJ_EQ_DEVICE_ID);
        lowGain = djSpecificDevice.createParameter("LOW_GAIN");
        lowKill = djSpecificDevice.createParameter("KILL_LOWS");
        midGain = djSpecificDevice.createParameter("MID_GAIN");
        midKill = djSpecificDevice.createParameter("KILL_MIDS");
        hiGain = djSpecificDevice.createParameter("HIGH_GAIN");
        hiKill = djSpecificDevice.createParameter("KILL_HIGHS");
        lowFreq = djSpecificDevice.createParameter("LOW_FREQ");
        hiFreq = djSpecificDevice.createParameter("HIGH_FREQ");
        lowKill.value().markInterested();
        midKill.value().markInterested();
        hiKill.value().markInterested();
        //        djEqDevice.addDirectParameterIdObserver(parmId -> {
        //            XoneK3ControllerExtension.println(" >> %s", Arrays.toString(parmId));
        //        });
    }
    
    
    public CursorRemoteControlsPage getTrackRemotes() {
        return trackRemotes;
    }
    
    private Device createSpecDevice(final DeviceMatcher matcher) {
        final DeviceBank deviceBank = this.track.createDeviceBank(1);
        deviceBank.setDeviceMatcher(matcher);
        return deviceBank.getDevice(0);
    }
    
    public boolean isTrackExists() {
        return trackExists;
    }
    
    public boolean isDjEqActive() {
        return djEqActive;
    }
    
    public void toggleDjEqActive() {
        djEqDevice.isEnabled().toggle();
    }
    
    public Parameter getLowGain() {
        return lowGain;
    }
    
    public Parameter getMidGain() {
        return midGain;
    }
    
    public Parameter getHiGain() {
        return hiGain;
    }
    
    public Parameter getHiKill() {
        return hiKill;
    }
    
    public Parameter getMidKill() {
        return midKill;
    }
    
    public Parameter getLowKill() {
        return lowKill;
    }
    
    public boolean eqExists() {
        return djEqExists;
    }
    
    public Parameter getLowFreq() {
        return lowFreq;
    }
    
    public Parameter getHiFreq() {
        return hiFreq;
    }
    
    public void insertEq() {
        cursorDevice.deviceChain().endOfDeviceChainInsertionPoint().insertBitwigDevice(DJ_EQ_DEVICE_ID);
    }
}
