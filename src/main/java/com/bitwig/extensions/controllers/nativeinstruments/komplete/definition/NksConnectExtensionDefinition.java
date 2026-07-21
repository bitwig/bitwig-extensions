package com.bitwig.extensions.controllers.nativeinstruments.komplete.definition;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.KontrolSMk3Extension;

public class NksConnectExtensionDefinition extends AbstractKompleteKontrolExtensionDefinition {

    private static final UUID DRIVER_ID = UUID.fromString("8b07a8e9-886c-f35b-e133-da9210306ce4");
    private static final String MODEL = "NKS Connect";

    public NksConnectExtensionDefinition() {
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
    public int getNumMidiInPorts() {
        return 1;
    }

    @Override
    public int getNumMidiOutPorts() {
        return 1;
    }

    @Override
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
        final PlatformType platformType) {
        list.add(new String[] {"NKS Connect"}, new String[] {"NKS Connect"});
    }

    @Override
    public String getHelpFilePath() {
        return "Controllers/Native Instruments/NKS Connect/NKS Connect.pdf";
    }

    @Override
    public KontrolSMk3Extension createInstance(final ControllerHost host) {
        return new KontrolSMk3Extension(this, host, true);
    }
}
