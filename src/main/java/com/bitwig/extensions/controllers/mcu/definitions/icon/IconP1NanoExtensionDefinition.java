package com.bitwig.extensions.controllers.mcu.definitions.icon;

import java.util.UUID;

import com.bitwig.extensions.controllers.mcu.config.ControllerConfig;
import com.bitwig.extensions.controllers.mcu.config.EncoderBehavior;
import com.bitwig.extensions.controllers.mcu.definitions.ManufacturerType;
import com.bitwig.extensions.controllers.mcu.definitions.SubType;

public class IconP1NanoExtensionDefinition extends AbstractIconExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("5cde7e5c-9357-42f9-a984-e24ab3b04a1a");
    private static final String DEVICE_NAME = "iCON P1-Nano";
    protected static final String BASE_DEVICE_PORT = "iCON P1-Nano %s"; //
    protected static final String[] VERSIONS = {"V1.21", "V1.22"};
    
    public IconP1NanoExtensionDefinition() {
        this(0);
    }
    
    public IconP1NanoExtensionDefinition(final int nrOfExtenders) {
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
        return "P1-Nano";
    }
    
    
    @Override
    public String getHelpFilePath() {
        return "Controllers/iCon/P1-Nano.pdf";
    }
    
    @Override
    protected ControllerConfig createControllerConfig() {
        return new ControllerConfig(ManufacturerType.ICON, SubType.V1M) //
            .setHasDedicateVu(true)//
            .setHasLowerDisplay(true) //
            .setHasIconTrackColoring(true) //
            .setHas2ClickResolution(true) //
            .setHasMasterFader(0x8)//
            .setHasTimeCodeLed(true) //
            .setJogWheelCoding(EncoderBehavior.ACCEL) //
            .setDecelerateJogWheel(true) //
            .setDisplaySegmented(true) //
            .setHasMasterVu(true);
    }
    
    @Override
    public String getName() {
        if (nrOfExtenders == 0) {
            return IconP1NanoExtensionDefinition.DEVICE_NAME;
        }
        return String.format("%s +%d EXTENDER", IconP1NanoExtensionDefinition.DEVICE_NAME, nrOfExtenders);
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
