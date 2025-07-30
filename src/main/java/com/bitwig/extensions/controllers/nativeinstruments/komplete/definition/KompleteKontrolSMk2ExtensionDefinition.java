package com.bitwig.extensions.controllers.nativeinstruments.komplete.definition;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.KompleteKontrolSMk2Extension;

public class KompleteKontrolSMk2ExtensionDefinition extends AbstractKompleteKontrolExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("5348f355-862e-4674-bbab-dd15f5342d99");
    private static final String MODEL = "S Mk2";
    
    public KompleteKontrolSMk2ExtensionDefinition() {
        super(MODEL);
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
    
    @Override
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
        final PlatformType platformType) {
        if (platformType == PlatformType.WINDOWS) {
            list.add(
                new String[] {"Komplete Kontrol DAW - 1", "KOMPLETE KONTROL - 1"},
                new String[] {"Komplete Kontrol DAW - 1", "KOMPLETE KONTROL - 1"});
        } else if (platformType == PlatformType.MAC) {
            list.add(
                new String[] {"Komplete Kontrol DAW - 1", "KOMPLETE KONTROL S49 MK2"},
                new String[] {"Komplete Kontrol DAW - 1", "KOMPLETE KONTROL S49 MK2"});
        } else if (platformType == PlatformType.LINUX) {
            list.add(
                new String[] {"Komplete Kontrol DAW - 1", "KOMPLETE KONTROL - 1"},
                new String[] {"Komplete Kontrol DAW - 1", "KOMPLETE KONTROL - 1"});
        }
    }
    
    @Override
    public String getHelpFilePath() {
        return "Controllers/Native Instruments/Komplete Kontrol MK2/Komplete Kontrol MK2.pdf";
    }
    
    @Override
    public KompleteKontrolSMk2Extension createInstance(final ControllerHost host) {
        return new KompleteKontrolSMk2Extension(this, host);
    }
}
