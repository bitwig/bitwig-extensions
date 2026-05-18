package com.bitwig.extensions.controllers.softube.console1.definition;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.softube.console1.Console1Extension;

public class SoftubeDawControlMultiExtensionDefinition extends Console1ExtensionDefinition {
    
    private static final UUID DRIVER_ID = UUID.fromString("98630b94-ed23-4a11-9981-05d3995b49AA");
    
    private final int portCount;
    
    public SoftubeDawControlMultiExtensionDefinition() {
        this(2);
    }
    
    public SoftubeDawControlMultiExtensionDefinition(int ports) {
        this.portCount = ports;
    }
    
    @Override
    public String getName() {
        return "Direct DAW Control (%d units)".formatted(portCount);
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
    
    @Override
    public String getHardwareModel() {
        return "Direct DAW Control (%d units)".formatted(portCount);
    }
    
    @Override
    public int getNumMidiInPorts() {
        return portCount;
    }
    
    @Override
    public int getNumMidiOutPorts() {
        return portCount;
    }
    
    @Override
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
        final PlatformType platformType) {
        if (platformType == PlatformType.WINDOWS) { //
            final String[] winPortList = getWinPortList();
            list.add(winPortList, winPortList);
        } else {
            final String[] macPortList = getMacPortList();
            list.add(macPortList, macPortList);
        }
    }
    
    private String[] getWinPortList() {
        String[] ports = new String[portCount];
        ports[0] = "Console 1 Fader Mk III";
        ports[1] = "Console 1 Channel Mk III";
        for (int i = 2; i < ports.length; i++) {
            ports[i] = "Console 1 Channel Mk 0%d".formatted(i - 2);
        }
        return ports;
    }
    
    private String[] getMacPortList() {
        String[] ports = new String[portCount];
        ports[0] = "Console 1 Channel Mk III DAW FA0000000073";
        ports[1] = "Console 1 Channel Mk III DAW FA0000000073";
        for (int i = 2; i < ports.length; i++) {
            ports[i] = "Console 1 Channel Mk 0%d".formatted(i - 2);
        }
        return ports;
    }
    
    @Override
    public Console1Extension createInstance(final ControllerHost host) {
        return new Console1Extension(this, host);
    }
    
}
