package com.bitwig.extensions.controllers.softube.console1.definition;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.softube.console1.Console1Extension;

public class Console1ChannelExtensionDefinition extends Console1ExtensionDefinition {
    
    private static final UUID DRIVER_ID = UUID.fromString("98630b94-ed23-4a11-9981-05d3995b4964");
    
    public Console1ChannelExtensionDefinition() {
    }
    
    @Override
    public String getName() {
        return "Direct DAW Control (1 unit)";
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
    
    @Override
    public String getHardwareModel() {
        return "Direct DAW Control (1 unit)";
    }
    
    @Override
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
        final PlatformType platformType) {
        if (platformType == PlatformType.WINDOWS) { //
            list.add(new String[] {"Console 1 Channel Mk III"}, new String[] {"Console 1 Channel Mk III"});
            list.add(new String[] {"Console 1 Fader Mk III"}, new String[] {"Console 1 Fader Mk III"});
            list.add(new String[] {"Softube Flow Studio"}, new String[] {"Softube Flow Studio"});
            list.add(new String[] {"Softube Console 1 Compact"}, new String[] {"Softube Console 1 Compact"});
        } else {
            list.add(
                new String[] {"Console 1 Channel Mk III DAW FA0000000073"},
                new String[] {"Console 1 Channel Mk III DAW FA0000000073"});
            list.add(new String[] {"Console 1 Fader Mk III"}, new String[] {"Console 1 Fader Mk III"});
            list.add(new String[] {"Softube Flow Studio"}, new String[] {"Softube Flow Studio"});
            list.add(new String[] {"Softube Console 1 Compact"}, new String[] {"Softube Console 1 Compact"});
        }
    }
    
    @Override
    public Console1Extension createInstance(final ControllerHost host) {
        return new Console1Extension(this, host);
    }
    
}
