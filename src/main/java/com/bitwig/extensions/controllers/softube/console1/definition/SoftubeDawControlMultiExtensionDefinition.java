package com.bitwig.extensions.controllers.softube.console1.definition;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.softube.console1.Console1Extension;

public class SoftubeDawControlMultiExtensionDefinition extends Console1ExtensionDefinition {
    
    private static final UUID DRIVER_ID = UUID.fromString("98630b94-ed23-4a11-9981-05d3995b49AA");
    
    private final int portCount;
    
    private static final String WIN_PORT_CHANNEL = "Console 1 Fader Mk III";
    private static final String WIN_PORT_FADER = "Console 1 Channel Mk III";
    private static final String WIN_PORT_FLOW = "Softube Flow Studio";
    private static final List<String> DEVICE_POOL =
        List.of(WIN_PORT_CHANNEL, WIN_PORT_FADER, WIN_PORT_FLOW, "Softube Console 1 Compact");
    
    public SoftubeDawControlMultiExtensionDefinition() {
        this(2);
    }
    
    public SoftubeDawControlMultiExtensionDefinition(final int ports) {
        this.portCount = ports;
    }
    
    @Override
    public String getName() {
        return "Direct DAW Control (%d units)".formatted(portCount);
    }
    
    @Override
    public UUID getId() {
        return DRIVER_ID;
    }
    
    @Override
    public String getHardwareModel() {
        return "Direct DAW Control (%d units)".formatted(portCount);
    }
    
    @Override
    public int getNumMidiInPorts() {
        return portCount;
    }
    
    @Override
    public int getNumMidiOutPorts() {
        return portCount;
    }
    
    @Override
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
        final PlatformType platformType) {
        if (platformType == PlatformType.WINDOWS) { //
            final List<String[]> portSelection = getWinPortList();
            for (final String[] winPortList : portSelection) {
                list.add(winPortList, winPortList);
            }
        } else {
            final String[] macPortList = getMacPortList();
            list.add(macPortList, macPortList);
        }
    }
    
    protected List<String[]> getWinPortList() {
        final List<String[]> selection = new ArrayList<>();
        final int poolSize = DEVICE_POOL.size();
        final int comboSize = Math.min(portCount, poolSize);
        
        // Generate all combinations of size comboSize from DEVICE_POOL
        generateCombinations(DEVICE_POOL, comboSize, 0, new ArrayList<>(), selection, portCount);
        
        return selection;
    }
    
    private void generateCombinations(final List<String> pool, final int comboSize, final int start,
        final List<String> current, final List<String[]> result, final int totalPorts) {
        
        if (current.size() == comboSize) {
            // Build full array: chosen pool entries + overflow slots
            final String[] ports = new String[totalPorts];
            for (int i = 0; i < current.size(); i++) {
                ports[i] = current.get(i);
            }
            // Fill overflow beyond pool size
            for (int i = comboSize; i < totalPorts; i++) {
                ports[i] = "Console 1 Channel Mk 0%d".formatted(i - comboSize);
            }
            result.add(ports);
            return;
        }
        
        for (int i = start; i < pool.size(); i++) {
            current.add(pool.get(i));
            generateCombinations(pool, comboSize, i + 1, current, result, totalPorts);
            current.remove(current.size() - 1);
        }
    }
    
    private String[] getMacPortList() {
        final String[] ports = new String[portCount];
        ports[0] = "Console 1 Channel Mk III DAW FA0000000073";
        ports[1] = "Console 1 Channel Mk III DAW FA0000000073";
        for (int i = 2; i < ports.length; i++) {
            ports[i] = "Console 1 Channel Mk 0%d".formatted(i - 2);
        }
        return ports;
    }
    
    @Override
    public Console1Extension createInstance(final ControllerHost host) {
        return new Console1Extension(this, host);
    }
    
}
