package com.bitwig.extensions.controllers.mcu.definitions.icon;

import java.util.UUID;

import com.bitwig.extensions.controllers.mcu.config.ControllerConfig;
import com.bitwig.extensions.controllers.mcu.config.EncoderBehavior;
import com.bitwig.extensions.controllers.mcu.definitions.ManufacturerType;
import com.bitwig.extensions.controllers.mcu.definitions.SubType;

public class IconV1mExtensionDefinition extends AbstractIconExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("faa99215-1162-4159-a513-970342d6dd9d");
    private static final String DEVICE_NAME = "iCON V1-M";
    protected static final String BASE_DEVICE_PORT = "iCON V1-M %s";
    protected static final String[] VERSIONS = {"V1.16", "V1.17", "V1.18"};
    
    public IconV1mExtensionDefinition() {
        this(0);
    }
    
    public IconV1mExtensionDefinition(final int nrOfExtenders) {
        super(nrOfExtenders);
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
    public String getHardwareModel() {
        return "V1-M";
    }
    
    @Override
    protected ControllerConfig createControllerConfig() {
        return new ControllerConfig(ManufacturerType.ICON, SubType.V1M) //
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
            .setForceUpdateOnStartup(5000) //
            .setHasMasterVu(true);
    }
    
    
    @Override
    public String getHelpFilePath() {
        return "Controllers/iCon/V1-M.pdf";
    }
    
    
    @Override
    public String getName() {
        if (nrOfExtenders == 0) {
            return IconV1mExtensionDefinition.DEVICE_NAME;
        }
        return String.format("%s +%d EXTENDER", IconV1mExtensionDefinition.DEVICE_NAME, nrOfExtenders);
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
}
