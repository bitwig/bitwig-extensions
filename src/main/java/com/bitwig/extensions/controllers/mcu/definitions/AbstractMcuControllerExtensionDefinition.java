package com.bitwig.extensions.controllers.mcu.definitions;

import java.util.List;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;

public abstract class AbstractMcuControllerExtensionDefinition extends ControllerExtensionDefinition {
    protected static final int MCU_API_VERSION = 18;
    protected static final String SOFTWARE_VERSION = "0.1";
    protected int nrOfExtenders;
    
    public AbstractMcuControllerExtensionDefinition(final int nrOfExtenders) {
        super();
        this.nrOfExtenders = nrOfExtenders;
    }
    
    @Override
    public String getAuthor() {
        return "Bitwig";
    }
    
    public static String getSoftwareVersion() {
        return SOFTWARE_VERSION;
    }
    
    @Override
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
        final PlatformType platformType) {
        final List<String[]> inPorts = getInPorts(platformType);
        final List<String[]> outPorts = getOutPorts(platformType);
        
        for (int i = 0; i < inPorts.size(); i++) {
            list.add(inPorts.get(i), outPorts.get(i));
        }
    }
    
    protected abstract List<String[]> getInPorts(final PlatformType platformType);
    
    protected abstract List<String[]> getOutPorts(final PlatformType platformType);
    
    @Override
    public int getRequiredAPIVersion() {
        return MCU_API_VERSION;
    }
    
    @Override
    public int getNumMidiInPorts() {
        return nrOfExtenders + 1;
    }
    
    @Override
    public int getNumMidiOutPorts() {
        return nrOfExtenders + 1;
    }
    
    public int getNrOfExtenders() {
        return nrOfExtenders;
    }
}
