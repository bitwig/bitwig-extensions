package com.bitwig.extensions.controllers.esi;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class XjamDefinition extends ControllerExtensionDefinition {

    private static final UUID DRIVER_ID = UUID.fromString("e139038a-fdfc-11ec-b939-0242ac120002");

    @Override
    public String getHardwareVendor() {
        return "ESI";
    }

    @Override
    public String getHardwareModel() {
        return "Xjam";
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
    public ControllerExtension createInstance(ControllerHost host) {
        return new Xjam(this, host);
    }

    @Override
    public String getName() {
        return "Xjam";
    }

    @Override
    public String getAuthor() {
        return "Bitwig";
    }

    @Override
    public String getVersion() {
        return "0.2";
    }

    @Override
    public UUID getId() {
        return DRIVER_ID;
    }

    @Override
    public int getRequiredAPIVersion() {
        return 16;
    }

    @Override
    public void listAutoDetectionMidiPortNames(AutoDetectionMidiPortNamesList list, PlatformType platformType) {
        list.add(new String[] { "Xjam" }, new String[] { "Xjam" });

    }

    @Override
    public String getHelpFilePath() {
        return "Controllers/ESI/ESI Xjam.pdf";
    }

}
