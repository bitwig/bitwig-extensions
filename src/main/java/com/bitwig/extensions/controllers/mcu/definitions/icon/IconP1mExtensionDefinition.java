package com.bitwig.extensions.controllers.mcu.definitions.icon;

import java.util.UUID;

import com.bitwig.extensions.controllers.mcu.config.ControllerConfig;
import com.bitwig.extensions.controllers.mcu.config.EncoderBehavior;
import com.bitwig.extensions.controllers.mcu.definitions.ManufacturerType;
import com.bitwig.extensions.controllers.mcu.definitions.SubType;
import com.bitwig.extensions.controllers.mcu.display.TimeCodeLed;

public class IconP1mExtensionDefinition extends AbstractIconExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("e57683e3-3ff5-4f2b-bee3-6cade432a685");
    private static final String DEVICE_NAME = "iCON P1-M";
    protected static final String BASE_DEVICE_PORT = "iCON P1-M %s"; //iCON P1-Nano V1.19
    protected static final String[] VERSIONS = {"V1.06", "V1.07", "V1.08", "V1.09"};
    
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
        final ControllerConfig config = new ControllerConfig(ManufacturerType.ICON, SubType.P1M) //
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
        config.setDisplayType(TimeCodeLed.DisplayType.ICON);
        return config;
    }
    
    @Override
    public String getName() {
        if (nrOfExtenders == 0) {
            return IconP1mExtensionDefinition.DEVICE_NAME + getFiller();
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
