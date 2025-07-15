package com.bitwig.extensions.controllers.expressivee;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class OsmosisControllerExtensionDefinition extends ControllerExtensionDefinition {

    private static final UUID DRIVER_ID = UUID.fromString("ffa07aa9-94cb-4c2c-8fc2-becabbddb53e");

    public OsmosisControllerExtensionDefinition() {
    }

    @Override
    public String getName() {
        return "Osmosis";
    }

    @Override
    public String getAuthor() {
        return "Bitwig";
    }

    @Override
    public String getVersion() {
        return "0.1";
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
        return "Osmosis";
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
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
        final PlatformType platformType) {
        list.add(new String[] {"Osmose", "MIDIIN2 (Osmose)"}, new String[] {"Osmose", "MIDIOUT2 (Osmose)"});
        list.add(new String[] {"Osmose play", "Osmose haken"}, new String[] {"Osmose play", "Osmose haken"});
        list.add(new String[] {"Osmose Port 1", "Osmose Port 2"}, new String[] {"Osmose Port 1", "Osmose Port 2"});
        list.add(new String[] {"Osmose Anschluss 1", "Osmose Anschluss 2"},
            new String[] {"Osmose Anschluss 1", "Osmose Anschluss 2"}
        );
        list.add(
            new String[] {"Osmose Puerto 1", "Osmose Puerto 2"}, new String[] {"Osmose Puerto 1", "Osmose Puerto 2"});
        list.add(
            new String[] {"Osmose Portti 1", "Osmose Portti 2"}, new String[] {"Osmose Portti 1", "Osmose Portti 2"});
        list.add(new String[] {"Osmose Poort 1", "Osmose Poort 2"}, new String[] {"Osmose Poort 1", "Osmose Poort 2"});
        list.add(new String[] {"Osmose Poort 1", "Osmose Poort 2"}, new String[] {"Osmose Poort 1", "Osmose Poort 2"});
        list.add(new String[] {"Osmose ポート1", "Osmose ポート2"}, new String[] {"Osmose ポート1", "Osmose ポート2"});
        list.add(new String[] {"Osmose 포트1", "Osmose 포트2"}, new String[] {"Osmose 포트1", "Osmose 포트2"});
    }

    @Override
    public OsmosisControllerExtension createInstance(final ControllerHost host) {
        return new OsmosisControllerExtension(this, host);
    }

}
