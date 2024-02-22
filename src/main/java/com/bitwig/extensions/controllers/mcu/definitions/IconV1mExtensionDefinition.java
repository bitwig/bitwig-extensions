package com.bitwig.extensions.controllers.mcu.definitions;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.mcu.McuExtension;
import com.bitwig.extensions.controllers.mcu.config.ControllerConfig;
import com.bitwig.extensions.controllers.mcu.config.EncoderBehavior;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class IconV1mExtensionDefinition extends AbstractMcuControllerExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("faa99215-1162-4159-a511-860342d6dd9d");
    private static final String DEVICE_NAME = "Icon V1-M";
    protected static final String SOFTWARE_VERSION = "0.1";
    protected static final String BASE_DEVICE_PORT = "iCON V1-M V1.12 ";

    public IconV1mExtensionDefinition() {
        this(0);
    }

    public IconV1mExtensionDefinition(final int nrOfExtenders) {
        super(nrOfExtenders);
    }

    @Override
    protected List<String[]> getInPorts(final PlatformType platformType) {
        final String[] inPortNames = new String[nrOfExtenders + 1];
        inPortNames[0] = BASE_DEVICE_PORT;
        for (int i = 1; i < nrOfExtenders + 1; i++) {
            inPortNames[i] = String.format("MIDIIN%d (%s)", i, BASE_DEVICE_PORT);
        }
        final List<String[]> portList = new ArrayList<>();
        portList.add(inPortNames);
        return portList;
    }

    @Override
    protected List<String[]> getOutPorts(final PlatformType platformType) {
        final String[] outPortNames = new String[nrOfExtenders + 1];
        outPortNames[0] = BASE_DEVICE_PORT;
        for (int i = 1; i < nrOfExtenders + 1; i++) {
            outPortNames[i] = String.format("MIDIOUT%d (%s)", i, BASE_DEVICE_PORT);
        }
        final List<String[]> portList = new ArrayList<>();
        portList.add(outPortNames);
        return portList;
    }

    @Override
    public String getHardwareVendor() {
        return "iCon Pro Audio";
    }

    @Override
    public String getHardwareModel() {
        return "V1-M";
    }

    public McuExtension createInstance(final ControllerHost host) {
        final ControllerConfig controllerConfig = new ControllerConfig(ManufacturerType.ICON, SubType.V1M) //
                .setHasDedicateVu(true)//
                .setHasLowerDisplay(true) //
                .setHasMasterFader(0x8) //
                .setHasIconTrackColoring(true) //
                .setHas2ClickResolution(true) //
                .setHasTimeCodeLed(true) //
                .setDisplaySegmented(true) //
                .setTopDisplayRowsFlipped(true) //
                .setNavigationWithJogWheel(false) // currently
                .setJogWheelCoding(EncoderBehavior.STEP) //
                .setHasMasterVu(true);
        //initSimulationLayout(controllerConfig.getSimulationLayout());
        return new McuExtension(this, host, controllerConfig);
    }

    @Override
    public String getName() {
        if (nrOfExtenders == 0) {
            return IconV1mExtensionDefinition.DEVICE_NAME;
        }
        return String.format("%s +%d EXTENDER", IconV1mExtensionDefinition.DEVICE_NAME, nrOfExtenders);
    }

    @Override
    public String getVersion() {
        return SOFTWARE_VERSION;
    }

    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
}
