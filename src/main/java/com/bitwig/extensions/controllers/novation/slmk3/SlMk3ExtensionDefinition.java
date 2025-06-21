package com.bitwig.extensions.controllers.novation.slmk3;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class SlMk3ExtensionDefinition extends ControllerExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("d59ad58c-101d-4769-910d-2bf781b1f330");
    private static final String DEVICE_PORT = "Novation SL MkIII";
    
    public SlMk3ExtensionDefinition() {
    }
    
    @Override
    public String getName() {
        return "SL MkIII";
    }
    
    @Override
    public String getAuthor() {
        return "Bitwig";
    }
    
    @Override
    public String getVersion() {
        return "0.9";
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
    
    @Override
    public String getHardwareVendor() {
        return "Novation";
    }
    
    @Override
    public String getHardwareModel() {
        return "SL MkIII";
    }
    
    @Override
    public int getRequiredAPIVersion() {
        return 18;
    }
    
    @Override
    public int getNumMidiInPorts() {
        return 2;
    }
    
    @Override
    public int getNumMidiOutPorts() {
        return 2;
    }
    
    @Override
    public String getHelpFilePath() {
        return "Controllers/Novation/Novation SL MkIII.pdf";
    }
    
    @Override
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
        final PlatformType platformType) {
        if (platformType == PlatformType.WINDOWS) {
            list.add(
                new String[] {"MIDIIN2 (%s)".formatted(DEVICE_PORT), DEVICE_PORT},
                new String[] {"MIDIOUT2 (%s)".formatted(DEVICE_PORT), DEVICE_PORT});
        } else if (platformType == PlatformType.MAC) {
            list.add(new String[] {
                "%s SL MkIII InControl".formatted(DEVICE_PORT), "%s SL MkIII MIDI".formatted(DEVICE_PORT)
            }, new String[] {
                "%s SL MkIII InControl".formatted(DEVICE_PORT), "%S SL MkIII MIDI".formatted(DEVICE_PORT)
            });
        } else {
            list.add(new String[] {
                "%s SL MkIII InCo".formatted(DEVICE_PORT), "%s SL MkIII MIDI".formatted(DEVICE_PORT)
            }, new String[] {
                "%s SL MkIII InCo".formatted(DEVICE_PORT), "%S SL MkIII MIDI".formatted(DEVICE_PORT)
            });
        }
    }
    
    @Override
    public SlMk3Extension createInstance(final ControllerHost host) {
        return new SlMk3Extension(this, host);
    }
}
