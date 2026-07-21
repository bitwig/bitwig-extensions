package com.bitwig.extensions.controllers.nativeinstruments.komplete;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceMatcher;
import com.bitwig.extension.controller.api.SpecificPluginDevice;

public enum NksDevice {
    KOMPLETE(KompleteKontrolExtension.KOMPLETE_KONTROL_VST3_ID, 0),
    KOMPLETE_VST2(KompleteKontrolExtension.KOMPLETE_KONTROL_DEVICE_ID, 0),
    KONTAKT7(KompleteKontrolExtension.KONTAKT_7_VST3_ID, 2048),
    KONTAKT8(KompleteKontrolExtension.KONTAKT_8_VST3_ID, 2048),
    MASCHINE(KompleteKontrolExtension.MASCHINE_3_VST3_ID, 128);
    final String id;
    final int paramOffset;
    final int vst2id;
    
    NksDevice(final String id, final int paramOffset) {
        this(id, paramOffset, -1);
    }
    
    NksDevice(final int id, final int paramOffset) {
        this(null, paramOffset, id);
    }
    
    NksDevice(final String id, final int paramOffset, final int vst2Id) {
        this.id = id;
        this.paramOffset = paramOffset;
        this.vst2id = vst2Id;
    }
    
    public String getId() {
        return id;
    }
    
    public int getParamOffset() {
        return paramOffset;
    }
    
    public DeviceMatcher createMatcher(final ControllerHost host) {
        if (this.id != null) {
            return host.createVST3DeviceMatcher(this.id);
        }
        return host.createVST2DeviceMatcher(this.vst2id);
    }
    
    public SpecificPluginDevice createSpecDevice(final Device device) {
        if (this.id != null) {
            return device.createSpecificVst3Device(this.id);
        }
        return device.createSpecificVst2Device(this.vst2id);
    }
}
