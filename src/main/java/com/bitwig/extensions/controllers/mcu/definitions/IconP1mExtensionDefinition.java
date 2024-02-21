package com.bitwig.extensions.controllers.mcu.definitions;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.mcu.McuExtension;
import com.bitwig.extensions.controllers.mcu.config.ControllerConfig;
import com.bitwig.extensions.controllers.mcu.config.EncoderBehavior;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class IconP1mExtensionDefinition extends AbstractMcuControllerExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("8b027591-dc6a-4dbd-9c23-4b8e05b755db");
    private static final String DEVICE_NAME = "Icon P1-M";
    protected static final String SOFTWARE_VERSION = "0.1";
    protected static final String BASE_DEVICE_PORT = "iCON P1-M V1.05 "; //iCON P1-Nano V1.19

    public IconP1mExtensionDefinition() {
        this(0);
    }

    public IconP1mExtensionDefinition(final int nrOfExtenders) {
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
        return "P1-M";
    }

    public McuExtension createInstance(final ControllerHost host) {
        final ControllerConfig controllerConfig = new ControllerConfig(ManufacturerType.ICON, SubType.V1M) //
                .setHasDedicateVu(true)//
                .setHasLowerDisplay(true)//
                .setHasIconTrackColoring(true) //
                .setHas2ClickResolution(true) //
                .setHasMasterFader(0x8) //
                .setDisplaySegmented(true) //
                .setJogWheelCoding(EncoderBehavior.ACCEL) //
                .setHasTimeCodeLed(true) //
                .setHasMasterVu(true);
        //initSimulationLayout(controllerConfig.getSimulationLayout());
        return new McuExtension(this, host, controllerConfig);
    }

    @Override
    public String getName() {
        if (nrOfExtenders == 0) {
            return IconP1mExtensionDefinition.DEVICE_NAME;
        }
        return String.format("%s +%d EXTENDER", IconP1mExtensionDefinition.DEVICE_NAME, nrOfExtenders);
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
