package com.bitwig.extensions.controllers.expressivee;

import java.util.List;
import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class OsmoseControllerExtensionDefinition extends ControllerExtensionDefinition {
    
    private static final UUID DRIVER_ID = UUID.fromString("ffa07aa9-94cb-4c2c-8fc2-becabbddb53e");
    private final static List<String> PORT_NAME_VARIATIONS =
        List.of("Port ", "Anschluss ", "Puerto ", "Portti ", "Poort ", "ポート", "포트");
    
    public OsmoseControllerExtensionDefinition() {
    }
    
    @Override
    public String getName() {
        return "Osmose";
    }
    
    @Override
    public String getAuthor() {
        return "Bitwig";
    }
    
    @Override
    public String getVersion() {
        return "1.0";
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
    
    @Override
    public String getHardwareVendor() {
        return "Expressive E";
    }
    
    @Override
    public String getHardwareModel() {
        return "Osmose";
    }
    
    @Override
    public int getRequiredAPIVersion() {
        return 20;
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
        return "Controllers/Expressive-e/Osmose.pdf";
    }
    
    @Override
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
        final PlatformType platformType) {
        switch (platformType) {
            case WINDOWS -> {
                list.add(new String[] {"Osmose", "MIDIIN2 (Osmose)"}, new String[] {"Osmose", "MIDIOUT2 (Osmose)"});
            }
            case MAC, LINUX -> {
                PORT_NAME_VARIATIONS.forEach(var -> list.add(
                    new String[] {"Osmose %s1".formatted(var), "Osmose %s2".formatted(var)},
                    new String[] {"Osmose %s1".formatted(var), "Osmose %s2".formatted(var)}));
            }
        }
        list.add(new String[] {"Osmose play", "Osmose haken"}, new String[] {"Osmose play", "Osmose haken"});
    }
    
    @Override
    public OsmoseControllerExtension createInstance(final ControllerHost host) {
        return new OsmoseControllerExtension(this, host);
    }
    
}
