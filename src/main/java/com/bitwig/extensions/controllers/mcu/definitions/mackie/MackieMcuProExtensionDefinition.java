package com.bitwig.extensions.controllers.mcu.definitions.mackie;

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

public class MackieMcuProExtensionDefinition extends AbstractMcuControllerExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("fa145533-5f45-4e19-81ad-1de77ffa2dab");
    private static final String DEVICE_NAME = "Mackie Control";
    protected static final String SOFTWARE_VERSION = "0.2";
    
    public MackieMcuProExtensionDefinition() {
        this(0);
    }
    
    public MackieMcuProExtensionDefinition(final int nrOfExtenders) {
        super(nrOfExtenders);
    }
    
    @Override
    protected List<String[]> getInPorts(final PlatformType platformType) {
        final String[] inPortNames = new String[nrOfExtenders + 1];
        inPortNames[0] = "MCU Pro USB v3.1";
        for (int i = 1; i < nrOfExtenders + 1; i++) {
            inPortNames[i] = String.format("MIDIIN%d (MCU Pro USB v3.1)", i);
        }
        final List<String[]> portList = new ArrayList<>();
        portList.add(inPortNames);
        return portList;
    }
    
    @Override
    protected List<String[]> getOutPorts(final PlatformType platformType) {
        final String[] outPortNames = new String[nrOfExtenders + 1];
        outPortNames[0] = "MCU Pro USB v3.1";
        for (int i = 1; i < nrOfExtenders + 1; i++) {
            outPortNames[i] = String.format("MIDIOUT%d (MCU Pro USB v3.1)", i);
        }
        final List<String[]> portList = new ArrayList<>();
        portList.add(outPortNames);
        return portList;
    }
    
    @Override
    public String getHardwareVendor() {
        return "Mackie";
    }
    
    @Override
    public String getHardwareModel() {
        return "Mackie Control";
    }
    
    public McuExtension createInstance(final ControllerHost host) {
        final ControllerConfig controllerConfig = new ControllerConfig(ManufacturerType.MACKIE, SubType.UNSPECIFIED) //
            .setHasDedicateVu(false)//
            .setJogWheelCoding(EncoderBehavior.ACCEL) //
            .setHasMasterFader(0x8) //
            .setHasTimeCodeLed(true) //
            .setHasMasterVu(false);
        //initSimulationLayout(controllerConfig.getSimulationLayout());
        controllerConfig.setAssignment(McuFunction.CLIP_LAUNCHER_MODE_4, McuAssignments.GROUP);
        controllerConfig.setAssignment(McuFunction.MODE_DEVICE, McuAssignments.V_PLUGIN);
        controllerConfig.setAssignment(McuFunction.MODE_TRACK_REMOTE, McuAssignments.V_INSTRUMENT);
        controllerConfig.setAssignment(McuFunction.MODE_PROJECT_REMOTE, McuAssignments.GV_MIDI_LF1);
        return new McuExtension(this, host, controllerConfig);
    }
    
    @Override
    public String getName() {
        if (nrOfExtenders == 0) {
            return MackieMcuProExtensionDefinition.DEVICE_NAME;
        }
        return String.format("%s +%d EXTENDER", MackieMcuProExtensionDefinition.DEVICE_NAME, nrOfExtenders);
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
