package com.bitwig.extensions.controllers.softube.console1.definition;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.softube.console1.Console1Extension;

public class Console1Multi3ExtensionDefinition extends Console1ExtensionDefinition {
    
    private static final UUID DRIVER_ID = UUID.fromString("98630b94-ed23-4a11-9981-05d3995b49AB");
    
    public Console1Multi3ExtensionDefinition() {
    }
    
    @Override
    public String getName() {
        return "Direct DAW Control (3 units)";
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
    
    @Override
    public String getHardwareModel() {
        return "Direct DAW Control (3 units)";
    }
    
    @Override
    public int getNumMidiInPorts() {
        return 3;
    }
    
    @Override
    public int getNumMidiOutPorts() {
        return 3;
    }
    
    @Override
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
        final PlatformType platformType) {
        if (platformType == PlatformType.WINDOWS) { //
            list.add(
                new String[] {"Console 1 Fader Mk III", "Console 1 Channel Mk III", "Console 1 Channel Mk III x"},
                new String[] {"Console 1 Fader Mk III", "Console 1 Channel Mk III", "Console 1 Channel Mk III x"});
            list.add(
                new String[] {"Console 1 Fader Mk III", "Console 1 Channel Mk III", "Console 1 Channel Mk III x"},
                new String[] {"Console 1 Fader Mk III", "Console 1 Channel Mk III", "Console 1 Channel Mk III x"});
        } else {
            list.add(
                new String[] {
                    "Console 1 Channel Mk III DAW FA0000000073", "Console 1 Channel Mk III DAW FA0000000074",
                    "Console 1 Channel Mk III DAW FA0000000075"
                }, new String[] {
                    "Console 1 Channel Mk III DAW FA0000000073", "Console 1 Channel Mk III DAW FA0000000074",
                    "Console 1 Channel Mk III DAW FA0000000075"
                });
        }
    }
    
    @Override
    public Console1Extension createInstance(final ControllerHost host) {
        return new Console1Extension(this, host);
    }
    
}
