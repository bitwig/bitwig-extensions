package com.bitwig.extensions.controllers.mcu.definitions.icon;

import java.util.UUID;

import com.bitwig.extensions.controllers.mcu.config.ControllerConfig;
import com.bitwig.extensions.controllers.mcu.config.EncoderBehavior;
import com.bitwig.extensions.controllers.mcu.definitions.ManufacturerType;
import com.bitwig.extensions.controllers.mcu.definitions.SubType;

public class IconP1mExtensionDefinition extends AbstractIconExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("8b027591-dc6a-4dbd-9c23-4b8e05b755db");
    private static final String DEVICE_NAME = "iCON P1-M";
    protected static final String BASE_DEVICE_PORT = "iCON P1-M %s"; //iCON P1-Nano V1.19
    protected static final String[] VERSIONS = {"V1.06", "V1.07"};
    
    public IconP1mExtensionDefinition() {
        this(0);
    }
    
    public IconP1mExtensionDefinition(final int nrOfExtenders) {
        super(nrOfExtenders);
    }
    
    @Override
    public String getHardwareModel() {
        return "P1-M";
    }
    
    @Override
    protected String[] getSupportedVersions() {
        return VERSIONS;
    }
    
    @Override
    protected String getBasePortName() {
        return BASE_DEVICE_PORT;
    }
    
    @Override
    public String getHelpFilePath() {
        return "Controllers/iCon/P1-M.pdf";
    }
    
    @Override
    protected ControllerConfig createControllerConfig() {
        return new ControllerConfig(ManufacturerType.ICON, SubType.P1M) //
            .setHasDedicateVu(true)//
            .setHasLowerDisplay(true) //
            .setHasMasterFader(0x8) //
            .setHasIconTrackColoring(true) //
            .setHas2ClickResolution(true) //
            .setHasTimeCodeLed(true) //
            .setDisplaySegmented(true) //
            .setTopDisplayRowsFlipped(true) //
            .setNavigationWithJogWheel(true) //
            .setJogWheelCoding(EncoderBehavior.ACCEL) //
            .setDecelerateJogWheel(true) //
            .setHasMasterVu(true);
    }
    
    @Override
    public String getName() {
        if (nrOfExtenders == 0) {
            return IconP1mExtensionDefinition.DEVICE_NAME;
        }
        return String.format("%s +%d EXTENDER", IconP1mExtensionDefinition.DEVICE_NAME, nrOfExtenders);
    }
    
    @Override
    protected String getFiller() {
        return " ";
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
}
