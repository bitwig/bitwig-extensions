package com.bitwig.extensions.controllers.nativeinstruments.komplete.definition;

import java.util.List;
import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.KontrolSMk3Extension;

public class KontrolSMk3ExtensionDefinition extends AbstractKompleteKontrolExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("8b07a8e9-886c-435b-8133-da9210306ce4");
    private static final String MODEL = "Kontrol S Mk3";
    private static final List<String> VARIANTS = List.of("49", "61", "88", "Sim");
    private static final String WIN_FORMAT_IN_OUT = "KONTROL S%s MK3";
    private static final String WIN_IN = "MIDIIN2 (KONTROL S%s MK3)";
    private static final String WIN_OUT = "MIDIOUT2 (KONTROL S%s MK3)";
    private static final String MAC_FORMAT_MAIN = "KONTROL S%s MK3 Main";
    private static final String MAC_FORMAT_DAW = "KONTROL S%s MK3 DAW";
    
    public KontrolSMk3ExtensionDefinition() {
        super(MODEL);
    }
    
    @Override
    public String getName() {
        return MODEL;
    }
    
    @Override
    public String getHardwareModel() {
        return MODEL;
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
    
    @Override
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
        final PlatformType platformType) {
        final List<String[]> inPorts = VARIANTS.stream().map(var -> getIn(platformType, var)).toList();
        final List<String[]> outPorts = VARIANTS.stream().map(var -> getOut(platformType, var)).toList();
        
        for (int i = 0; i < inPorts.size(); i++) {
            list.add(inPorts.get(i), outPorts.get(i));
        }
    }
    
    private String[] getIn(final PlatformType type, final String variant) {
        if (variant.equals("Sim")) {
            return new String[] {"Kontrol MK3 Simulator DAW", "Kontrol MK3 Simulator Main"};
        }
        return switch (type) {
            case LINUX, MAC -> new String[] {MAC_FORMAT_DAW.formatted(variant), MAC_FORMAT_MAIN.formatted(variant)};
            case WINDOWS -> new String[] {WIN_IN.formatted(variant), WIN_FORMAT_IN_OUT.formatted(variant)};
        };
    }
    
    private String[] getOut(final PlatformType type, final String variant) {
        if (variant.equals("Sim")) {
            return new String[] {"Kontrol MK3 Simulator DAW", "Kontrol MK3 Simulator Main"};
        }
        return switch (type) {
            case LINUX, MAC -> new String[] {MAC_FORMAT_DAW.formatted(variant), MAC_FORMAT_MAIN.formatted(variant)};
            case WINDOWS -> new String[] {WIN_OUT.formatted(variant), WIN_FORMAT_IN_OUT.formatted(variant)};
        };
    }
    
    @Override
    public String getHelpFilePath() {
        return "Controllers/Native Instruments/Kontrol S-Series/Kontrol S-Series Mk3.pdf";
    }
    
    @Override
    public KontrolSMk3Extension createInstance(final ControllerHost host) {
        return new KontrolSMk3Extension(this, host);
    }
}
