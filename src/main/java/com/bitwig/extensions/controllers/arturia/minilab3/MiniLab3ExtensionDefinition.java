package com.bitwig.extensions.controllers.arturia.minilab3;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.api.ControllerHost;

public class MiniLab3ExtensionDefinition extends MiniLabExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("6d0d76fc-b9b0-11ec-8422-0242ac120002");
    
    private static final String PORT_NAME_MIDI = "Minilab3 MIDI";
    private static final String PORT_NAME = "Minilab3";
    private static final String PORT_NAME_LINUX = "Minilab3 Minilab3 MIDI";
    
    public MiniLab3ExtensionDefinition() {
        super();
    }
    
    @Override
    public String getName() {
        return "MiniLab 3";
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
    
    @Override
    public String getHardwareModel() {
        return "MiniLab 3";
    }
    
    @Override
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
        final PlatformType platformType) {
        if (platformType == PlatformType.WINDOWS) {
            list.add(new String[] {PORT_NAME}, new String[] {PORT_NAME});
            for (int i = 1; i < 5; i++) {
                appendWinPrefix(list, i);
            }
        } else if (platformType == PlatformType.MAC) {
            list.add(new String[] {PORT_NAME_MIDI}, new String[] {PORT_NAME_MIDI});
        } else if (platformType == PlatformType.LINUX) {
            list.add(new String[] {PORT_NAME_LINUX}, new String[] {PORT_NAME_LINUX});
        }
    }
    
    private void appendWinPrefix(final AutoDetectionMidiPortNamesList list, final int index) {
        final String prefix = index > 1 ? "%d- ".formatted(index) : "";
        list.add(
            new String[] {"%s%s MIDI".formatted(prefix, PORT_NAME)},
            new String[] {"%s%s MIDI".formatted(prefix, PORT_NAME)});
    }
    
    @Override
    public MiniLab3Extension createInstance(final ControllerHost host) {
        return new MiniLab3Extension(this, host, MinilabModel.MINILAB_3);
    }
}
