package com.bitwig.extensions.controllers.arturia.minilab3;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.api.ControllerHost;

public class MiniLab37ExtensionDefinition extends MiniLabExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("00d0f8ae-0f13-482f-b96b-8fb016025fcd");
    
    private static final String PORT_NAME = "Minilab37";
    
    public MiniLab37ExtensionDefinition() {
        super();
    }
    
    @Override
    public String getName() {
        return "MiniLab 37";
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
    
    @Override
    public String getHardwareModel() {
        return "MiniLab 37";
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
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
        final PlatformType platformType) {
        if (platformType == PlatformType.WINDOWS) {
            for (int i = 1; i < 5; i++) {
                appendWin11Prefix(list, i);
            }
        } else if (platformType == PlatformType.MAC) {
            for (int i = 1; i < 5; i++) {
                appendWin11Prefix(list, i);
            }
        } else if (platformType == PlatformType.LINUX) {
            appendLinuxPrefix(list, 0);
        }
    }
    
    
    private void appendWin11Prefix(final AutoDetectionMidiPortNamesList list, final int index) {
        final String prefix = index > 1 ? "%d- ".formatted(index) : "";
        list.add(
            new String[] {"%s%s DAW".formatted(prefix, PORT_NAME), "%s%s MIDI".formatted(prefix, PORT_NAME)},
            new String[] {"%s%s DAW".formatted(prefix, PORT_NAME), "%s%s MIDI".formatted(prefix, PORT_NAME)});
    }
    
    private void appendLinuxPrefix(final AutoDetectionMidiPortNamesList list, final int index) {
        final String prefix = "%s ".formatted(PORT_NAME);
        list.add(
            new String[] {"%s%s DAW".formatted(prefix, PORT_NAME), "%s%s MIDI".formatted(prefix, PORT_NAME)},
            new String[] {"%s%s DAW".formatted(prefix, PORT_NAME), "%s%s MIDI".formatted(prefix, PORT_NAME)});
    }
    
    @Override
    public MiniLab3Extension createInstance(final ControllerHost host) {
        return new MiniLab3Extension(this, host, MinilabModel.MINILAB_37);
    }
}
