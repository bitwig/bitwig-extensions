package com.bitwig.extensions.controllers.novation.launchkey_mk4.definition;

import java.util.List;
import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.LaunchkeyMk4Extension;

public class LaunchkeyMiniMk4ExtensionDefinition extends AbstractLaunchkeyMk4ExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("ef68e909-d5f1-4557-8526-2ce6978fabcc");
    private static final List<Integer> VARIANTS = List.of(25, 37);
    
    @Override
    public String getName() {
        return "Launchkey Mini Mk4";
    }
    
    @Override
    public String getHardwareModel() {
        return "Launchkey Mini Mk4";
    }
    
    @Override
    public String getHelpFilePath() {
        return "Controllers/Novation/LaunchKey Mini Mk4.pdf";
    }
    
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
    
    @Override
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
        final PlatformType platformType) {
        if (platformType == PlatformType.WINDOWS) {
            for (final int variant : VARIANTS) {
                for (int i = 0; i < 4; i++) {
                    list.add(
                        new String[] {
                            "MIDIIN2 (Launchkey Mini MK4 %d MIDI%s".formatted(variant, (i & 0x1) != 0 ? ")" : ""),
                            "Launchkey Mini MK4 %d MIDI".formatted(variant)
                        }, new String[] {
                            "MIDIOUT2 (Launchkey Mini MK4 %d MIDI%s".formatted(variant, (i & 0x2) != 0 ? ")" : ""),
                            "Launchkey Mini MK4 %d MIDI".formatted(variant)
                        });
                }
            }
        } else if (platformType == PlatformType.MAC) {
            for (final int variant : VARIANTS) {
                list.add(
                    new String[] {
                        "Launchkey Mini MK4 %d DAW Out".formatted(variant),
                        "Launchkey Mini MK4 %d MIDI Out".formatted(variant)
                    }, new String[] {
                        "Launchkey Mini MK4 %d DAW In".formatted(variant),
                        "Launchkey Mini MK4 %d MIDI In".formatted(variant)
                    });
            }
        } else if (platformType == PlatformType.LINUX) {
            for (final int variant : VARIANTS) {
                list.add(
                    new String[] {
                        "Launchkey Mini MK4 %d Launchkey".formatted(variant),
                        "Launchkey Mini MK4 %d Launchkey #2".formatted(variant)
                    }, new String[] {
                        "Launchkey Mini MK4 %d Launchkey".formatted(variant),
                        "Launchkey Mini MK4 %d Launchkey #2".formatted(variant)
                    });
            }
        }
    }
    
    @Override
    public LaunchkeyMk4Extension createInstance(final ControllerHost host) {
        return new LaunchkeyMk4Extension(this, host, true);
    }
    
}
