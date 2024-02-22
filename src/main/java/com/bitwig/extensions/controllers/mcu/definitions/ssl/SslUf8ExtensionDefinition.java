package com.bitwig.extensions.controllers.mcu.definitions.ssl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.mcu.McuExtension;
import com.bitwig.extensions.controllers.mcu.config.ControllerConfig;
import com.bitwig.extensions.controllers.mcu.config.EncoderBehavior;
import com.bitwig.extensions.controllers.mcu.config.McuAssignments;
import com.bitwig.extensions.controllers.mcu.config.McuFunction;
import com.bitwig.extensions.controllers.mcu.definitions.AbstractMcuControllerExtensionDefinition;
import com.bitwig.extensions.controllers.mcu.definitions.ManufacturerType;
import com.bitwig.extensions.controllers.mcu.definitions.SubType;

public class SslUf8ExtensionDefinition extends AbstractMcuControllerExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("ebd7dcbe-7093-4f80-8a8a-a77308ab20a1");
    private final int layerIndex;
    protected static final String DEVICE_NAME = "SSL UF8/UF1";
    protected static final String SOFTWARE_VERSION = "1.0";
    protected static final String BASE_DEVICE_PORT = "SSL V-MIDI Port %d";
    protected static final String BASE_DEVICE_PORT_MAC_IN = "SSL V-MIDI Port %d Source";
    protected static final String BASE_DEVICE_PORT_MAC_OUT = "SSL V-MIDI Port %d Destination";
    
    public SslUf8ExtensionDefinition() {
        this(0, 0);
    }
    
    public SslUf8ExtensionDefinition(final int nrOfExtenders, final int layerIndex) {
        super(nrOfExtenders);
        this.layerIndex = layerIndex;
    }
    
    @Override
    protected List<String[]> getInPorts(final PlatformType platformType) {
        final String[] inPortNames = new String[nrOfExtenders + 1];
        final int portOffset = layerIndex * 4;
        final String inPortNameFormat =
            platformType == PlatformType.WINDOWS ? BASE_DEVICE_PORT : BASE_DEVICE_PORT_MAC_IN;
        inPortNames[0] = inPortNameFormat.formatted(1 + portOffset);
        for (int i = 1; i < nrOfExtenders + 1; i++) {
            inPortNames[i] = inPortNameFormat.formatted(i + 1 + portOffset);
        }
        final List<String[]> portList = new ArrayList<>();
        portList.add(inPortNames);
        return portList;
    }
    
    @Override
    protected List<String[]> getOutPorts(final PlatformType platformType) {
        final String[] outPortNames = new String[nrOfExtenders + 1];
        final int portOffset = layerIndex * 4;
        final String outPortNameFormat =
            platformType == PlatformType.WINDOWS ? BASE_DEVICE_PORT : BASE_DEVICE_PORT_MAC_OUT;
        outPortNames[0] = outPortNameFormat.formatted(1 + portOffset);
        for (int i = 1; i < nrOfExtenders + 1; i++) {
            outPortNames[i] = outPortNameFormat.formatted(i + 1 + portOffset);
        }
        final List<String[]> portList = new ArrayList<>();
        portList.add(outPortNames);
        return portList;
    }
    
    @Override
    public String getHardwareVendor() {
        return "Solid State Logic";
    }
    
    @Override
    public String getHardwareModel() {
        if (layerIndex == 0) {
            return "UF8/UF1";
        } else {
            return "UF8/UF1 Layer %d".formatted(layerIndex + 1);
        }
    }
    
    public McuExtension createInstance(final ControllerHost host) {
        return new McuExtension(this, host, createSslConfig());
    }
    
    protected ControllerConfig createSslConfig() {
        final ControllerConfig controllerConfig = new ControllerConfig(ManufacturerType.SSL, SubType.UF8) //
            .setDisplaySegmented(true)//
            .setJogWheelCoding(EncoderBehavior.ACCEL) //
            //         .setSingleMainUnit(false) //
            .setHasMasterFader(0x8) //
            .setHasTimeCodeLed(true) //
            .setHasDedicateVu(true);
        controllerConfig.setAssignment(McuFunction.PUNCH_IN, McuAssignments.DROP);
        controllerConfig.setAssignment(McuFunction.PUNCH_OUT, McuAssignments.REPLACE);
        controllerConfig.setAssignment(McuFunction.OVERDUB, McuAssignments.TRIM);
        controllerConfig.setAssignment(McuFunction.TEMPO, McuAssignments.GV_AUDIO_LF3);
        controllerConfig.setAssignment(McuFunction.UNDO, McuAssignments.UNDO);
        controllerConfig.setAssignment(McuFunction.SEND_SELECT_1, McuAssignments.F1);
        controllerConfig.setAssignment(McuFunction.SEND_SELECT_2, McuAssignments.F2);
        controllerConfig.setAssignment(McuFunction.SEND_SELECT_3, McuAssignments.F3);
        controllerConfig.setAssignment(McuFunction.SEND_SELECT_4, McuAssignments.F4);
        controllerConfig.setAssignment(McuFunction.SEND_SELECT_5, McuAssignments.F5);
        controllerConfig.setAssignment(McuFunction.SEND_SELECT_6, McuAssignments.F6);
        controllerConfig.setAssignment(McuFunction.SEND_SELECT_7, McuAssignments.F7);
        controllerConfig.setAssignment(McuFunction.SEND_SELECT_8, McuAssignments.F8);
        controllerConfig.setAssignment(McuFunction.TRACK_MODE, McuAssignments.V_TRACK);
        controllerConfig.setAssignment(McuFunction.MODE_DEVICE, McuAssignments.V_PLUGIN);
        controllerConfig.setAssignment(McuFunction.MODE_TRACK_REMOTE, McuAssignments.V_INSTRUMENT);
        controllerConfig.setAssignment(McuFunction.MODE_PROJECT_REMOTE, McuAssignments.SAVE);
        controllerConfig.setAssignment(McuFunction.CLIP_LAUNCHER_MODE_2, McuAssignments.GROUP);
        controllerConfig.setAssignment(McuFunction.SSL_PLUGINS_MENU, McuAssignments.GV_BUSSES_LF6);
        controllerConfig.setAssignment(McuFunction.GROOVE_MENU, McuAssignments.GV_AUX_LF5);
        controllerConfig.setAssignment(McuFunction.ZOOM_MENU, McuAssignments.GV_INSTRUMENT_LF4);
        controllerConfig.setAssignment(
            McuFunction.RESTORE_AUTOMATION, McuAssignments.SOLO); //McuAssignments.AUTO_READ_OFF
        controllerConfig.setAssignment(McuFunction.PAGE_LEFT, McuAssignments.GV_OUTPUTS_LF7);
        controllerConfig.setAssignment(McuFunction.PAGE_RIGHT, McuAssignments.GV_USER_LF8);
        controllerConfig.setAssignment(McuFunction.ZOOM_IN, McuAssignments.CANCEL);
        controllerConfig.setAssignment(McuFunction.ZOOM_OUT, McuAssignments.NUDGE);
        
        return controllerConfig;
    }
    
    @Override
    public String getName() {
        if (layerIndex == 0) {
            return getBaseName();
        }
        return "%s Layer %d".formatted(getBaseName(), layerIndex + 1);
    }
    
    private String getBaseName() {
        if (nrOfExtenders == 0) {
            return SslUf8ExtensionDefinition.DEVICE_NAME;
        }
        return String.format("%s +%d EXTENDER", SslUf8ExtensionDefinition.DEVICE_NAME, nrOfExtenders);
    }
    
    @Override
    public String getVersion() {
        return SOFTWARE_VERSION;
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
    
    @Override
    public String getHelpFilePath() {
        return "Controllers/SSL/SSL UF8-UF1.pdf";
    }
    
    
}
