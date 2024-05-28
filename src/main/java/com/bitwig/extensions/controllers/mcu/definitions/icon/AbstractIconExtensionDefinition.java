package com.bitwig.extensions.controllers.mcu.definitions.icon;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.mcu.McuExtension;
import com.bitwig.extensions.controllers.mcu.config.ControllerConfig;
import com.bitwig.extensions.controllers.mcu.config.CustomAssignment;
import com.bitwig.extensions.controllers.mcu.config.McuAssignments;
import com.bitwig.extensions.controllers.mcu.config.McuFunction;
import com.bitwig.extensions.controllers.mcu.definitions.AbstractMcuControllerExtensionDefinition;

public abstract class AbstractIconExtensionDefinition extends AbstractMcuControllerExtensionDefinition {
    private static final String[] PORT_VARIANTS = new String[] {"Anschluss", "Port"};
    protected static final String SOFTWARE_VERSION = "1.0";
    
    public AbstractIconExtensionDefinition() {
        this(0);
    }
    
    public AbstractIconExtensionDefinition(final int nrOfExtenders) {
        super(nrOfExtenders);
    }
    
    private String getInPortName(final PlatformType platformType, final String baseDevicePort,
        final String portNameVariant, final int port) {
        return switch (platformType) {
            case WINDOWS -> port == 1 ? baseDevicePort : "MIDIIN%d (%s%s)".formatted(port, baseDevicePort, getFiller());
            case LINUX -> "%s MIDI %d".formatted(baseDevicePort, port);
            case MAC -> "%s %s %d".formatted(baseDevicePort, portNameVariant, port);
        };
    }
    
    private String getOutPortName(final PlatformType platformType, final String baseDevicePort,
        final String portNameVariant, final int port) {
        return switch (platformType) {
            case WINDOWS ->
                port == 1 ? baseDevicePort : "MIDIOUT%d (%s%s)".formatted(port, baseDevicePort, getFiller());
            case LINUX -> "%s MIDI %d".formatted(baseDevicePort, port);
            case MAC -> "%s %s %d".formatted(baseDevicePort, portNameVariant, port);
        };
    }
    
    protected List<String[]> getInPorts(final PlatformType platformType, final String baseModel,
        final String[] versions) {
        final List<String[]> portList = new ArrayList<>();
        final String[] portVariants = getPortVariants(platformType);
        for (final String portVariant : portVariants) {
            for (final String version : versions) {
                final String base = baseModel.formatted(version);
                portList.add(getInPorts(platformType, base, portVariant));
            }
        }
        return portList;
    }
    
    protected List<String[]> getOutPorts(final PlatformType platformType, final String baseModel,
        final String[] versions) {
        final List<String[]> portList = new ArrayList<>();
        
        final String[] portVariants = getPortVariants(platformType);
        for (final String portVariant : portVariants) {
            for (final String version : versions) {
                final String base = baseModel.formatted(version);
                portList.add(getOutPorts(platformType, base, portVariant));
            }
        }
        return portList;
    }
    
    protected String[] getInPorts(final PlatformType platformType, final String baseDevicePort,
        final String portNameVariant) {
        final String[] inPortNames = new String[nrOfExtenders + 1];
        for (int i = 0; i < nrOfExtenders + 1; i++) {
            inPortNames[i] = getInPortName(platformType, baseDevicePort, portNameVariant, i + 1);
        }
        return inPortNames;
    }
    
    protected String[] getOutPorts(final PlatformType platformType, final String baseDevicePort,
        final String portNameVariant) {
        final String[] outPortNames = new String[nrOfExtenders + 1];
        for (int i = 0; i < nrOfExtenders + 1; i++) {
            outPortNames[i] = getOutPortName(platformType, baseDevicePort, portNameVariant, i + 1);
        }
        return outPortNames;
    }
    
    @Override
    protected List<String[]> getInPorts(final PlatformType platformType) {
        return getInPorts(platformType, getBasePortName(), getSupportedVersions());
    }
    
    @Override
    protected List<String[]> getOutPorts(final PlatformType platformType) {
        return getOutPorts(platformType, getBasePortName(), getSupportedVersions());
    }
    
    protected String getFiller() {
        return "";
    }
    
    protected abstract String getBasePortName();
    
    protected abstract String[] getSupportedVersions();
    
    protected String[] getPortVariants(final PlatformType platformType) {
        return platformType == PlatformType.MAC ? PORT_VARIANTS : new String[] {""};
    }
    
    protected abstract ControllerConfig createControllerConfig();
    
    public McuExtension createInstance(final ControllerHost host) {
        final ControllerConfig controllerConfig = createControllerConfig();
        controllerConfig.setAssignment(McuFunction.CUE_MARKER, McuAssignments.SAVE);
        controllerConfig.setAssignment(McuFunction.TEMPO, McuAssignments.GV_AUDIO_LF3);
        controllerConfig.setAssignment(McuFunction.GROOVE_MENU, McuAssignments.GV_AUX_LF5);
        controllerConfig.setAssignment(McuFunction.ZOOM_MENU, McuAssignments.GV_INSTRUMENT_LF4);
        controllerConfig.setAssignment(McuFunction.AUTOMATION_LAUNCHER, McuAssignments.F2);
        controllerConfig.setAssignment(McuFunction.TRACK_MODE, McuAssignments.V_TRACK);
        controllerConfig.setAssignment(McuFunction.MODE_DEVICE, McuAssignments.V_PLUGIN);
        controllerConfig.setAssignment(McuFunction.MODE_PROJECT_REMOTE, McuAssignments.NUDGE);
        controllerConfig.setAssignment(McuFunction.MODE_TRACK_REMOTE, McuAssignments.V_INSTRUMENT);
        controllerConfig.setAssignment(McuFunction.UNDO, McuAssignments.UNDO);
        controllerConfig.setAssignment(McuFunction.CLIP_LAUNCHER_MODE_4, McuAssignments.GROUP);
        controllerConfig.setAssignment(
            McuFunction.RESTORE_AUTOMATION, McuAssignments.SOLO); //McuAssignments.AUTO_READ_OFF
        controllerConfig.setAssignment(McuFunction.ARRANGER, McuAssignments.F1);
        controllerConfig.setAssignment(McuFunction.DUPLICATE, new CustomAssignment(McuFunction.DUPLICATE, 54, 1));
        controllerConfig.setAssignment(McuFunction.CLEAR, new CustomAssignment(McuFunction.CLEAR, 55, 1));
        //initSimulationLayout(controllerConfig.getSimulationLayout());
        return new McuExtension(this, host, controllerConfig);
    }
    
    @Override
    public String getSupportFolderPath() {
        return "Controllers/iCon/mappings";
    }
    
    @Override
    public String getVersion() {
        return SOFTWARE_VERSION;
    }
    
    @Override
    public String getHardwareVendor() {
        return "iCON";
    }
    
}
