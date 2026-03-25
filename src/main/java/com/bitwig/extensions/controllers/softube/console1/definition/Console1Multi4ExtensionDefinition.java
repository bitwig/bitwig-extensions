package com.bitwig.extensions.controllers.softube.console1.definition;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.softube.console1.Console1Extension;

public class Console1Multi4ExtensionDefinition extends Console1ExtensionDefinition {
    
    private static final UUID DRIVER_ID = UUID.fromString("a86f0b94-ed23-4a11-9981-05d3995b49AB");
    
    public Console1Multi4ExtensionDefinition() {
    }
    
    @Override
    public String getName() {
        return "Console 1 Mk III x 4";
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
    
    @Override
    public String getHardwareModel() {
        return "Console 1 Mk III x 4";
    }
    
    @Override
    public int getNumMidiInPorts() {
        return 4;
    }
    
    @Override
    public int getNumMidiOutPorts() {
        return 4;
    }
    
    @Override
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
        final PlatformType platformType) {
        if (platformType == PlatformType.WINDOWS) { //
            list.add(
                new String[] {
                    "Console 1 Fader Mk III", "Console 1 Channel Mk 00", "Console 1 Channel Mk 01",
                    "Console 1 Channel Mk 02"
                }, new String[] {
                    "Console 1 Fader Mk III", "Console 1 Channel Mk 00", "Console 1 Channel Mk 01",
                    "Console 1 Channel Mk 02"
                });
            list.add(
                new String[] {
                    "Console 1 Fader Mk III", "Console 1 Channel Mk 00", "Console 1 Channel Mk 01",
                    "Console 1 Channel Mk 02"
                }, new String[] {
                    "Console 1 Fader Mk III", "Console 1 Channel Mk 00", "Console 1 Channel Mk 01",
                    "Console 1 Channel Mk 02"
                });
        } else {
            list.add(
                new String[] {
                    "Console 1 Channel Mk III DAW FA0000000073", "Console 1 Channel Mk III DAW FA0000000074",
                    "Console 1 Channel Mk III DAW FA0000000075", "Console 1 Channel Mk III DAW FA0000000076"
                }, new String[] {
                    "Console 1 Channel Mk III DAW FA0000000073", "Console 1 Channel Mk III DAW FA0000000074",
                    "Console 1 Channel Mk III DAW FA0000000075", "Console 1 Channel Mk III DAW FA0000000076"
                });
        }
    }
    
    @Override
    public Console1Extension createInstance(final ControllerHost host) {
        return new Console1Extension(this, host);
    }
    
}
