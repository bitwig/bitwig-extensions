package com.bitwig.extensions.controllers.mcu.config;

import java.util.HashMap;
import java.util.Map;

import com.bitwig.extensions.controllers.mcu.definitions.ManufacturerType;
import com.bitwig.extensions.controllers.mcu.definitions.SubType;

public class ControllerConfig {
    private boolean hasLowerDisplay;
    private final ManufacturerType manufacturerType;
    private final SubType subType;
    private boolean hasDedicateVu;
    private boolean hasMasterVu;
    private boolean useClearDuplicateModifiers = false;
    private boolean functionSectionLayered = false;
    private boolean has2ClickResolution;
    private boolean topDisplayRowsFlipped = false;
    private boolean navigationWithJogWheel = false;
    private int masterFaderChannel = -1;
    private boolean hasIconTrackColoring = false;
    private boolean displaySegmented = false;
    private boolean singleMainUnit = true;
    private int nrOfExtenders;
    private final Map<McuFunction, ButtonAssignment> assignmentMap = new HashMap<>();
    private boolean hasTimeCodeLed;
    private EncoderBehavior jogWheelBehavior = EncoderBehavior.STEP;
    
    public ControllerConfig(final ManufacturerType manufacturerType, final SubType subType) {
        this.manufacturerType = manufacturerType;
        this.subType = subType;
        createDefaultAssignmentMap();
    }
    
    public void createDefaultAssignmentMap() {
        assignmentMap.put(McuFunction.PLAY, McuAssignments.PLAY);
        assignmentMap.put(McuFunction.STOP, McuAssignments.STOP);
        assignmentMap.put(McuFunction.RECORD, McuAssignments.RECORD);
        assignmentMap.put(McuFunction.LOOP, McuAssignments.CYCLE);
        assignmentMap.put(McuFunction.METRO, McuAssignments.CLICK);
        assignmentMap.put(McuFunction.PUNCH_IN, McuAssignments.F6);
        assignmentMap.put(McuFunction.PUNCH_OUT, McuAssignments.F7);
        assignmentMap.put(McuFunction.FAST_FORWARD, McuAssignments.FFWD);
        assignmentMap.put(McuFunction.FAST_REVERSE, McuAssignments.REWIND);
        assignmentMap.put(McuFunction.SHIFT, McuAssignments.SHIFT);
        assignmentMap.put(McuFunction.OPTION, McuAssignments.OPTION);
        assignmentMap.put(McuFunction.ALT, McuAssignments.ALT);
        assignmentMap.put(McuFunction.CONTROL, McuAssignments.CONTROL);
        assignmentMap.put(McuFunction.GLOBAL_VIEW, McuAssignments.GLOBAL_VIEW);
        assignmentMap.put(McuFunction.OVERDUB, McuAssignments.REPLACE);
        assignmentMap.put(McuFunction.DISPLAY_SMPTE, McuAssignments.DISPLAY_SMPTE);
        
        assignmentMap.put(McuFunction.FLIP, McuAssignments.FLIP);
        assignmentMap.put(McuFunction.NAME_VALUE, McuAssignments.DISPLAY_NAME);
        
        assignmentMap.put(McuFunction.AUTO_READ, McuAssignments.AUTO_READ_OFF);
        assignmentMap.put(McuFunction.AUTO_WRITE, McuAssignments.AUTO_WRITE);
        assignmentMap.put(McuFunction.AUTO_TOUCH, McuAssignments.TOUCH);
        assignmentMap.put(McuFunction.AUTO_LATCH, McuAssignments.LATCH);
        assignmentMap.put(McuFunction.CUE_MARKER, McuAssignments.MARKER);
        
        assignmentMap.put(McuFunction.MODE_ALL_SENDS, McuAssignments.V_TRACK);
        assignmentMap.put(McuFunction.MODE_PAN, McuAssignments.V_PAN);
        assignmentMap.put(McuFunction.MODE_EQ, McuAssignments.V_EQ);
        assignmentMap.put(McuFunction.MODE_PLUGIN, McuAssignments.V_PLUGIN);
        assignmentMap.put(McuFunction.MODE_INSTRUMENT, McuAssignments.V_INSTRUMENT);
        assignmentMap.put(McuFunction.MODE_NOTE_FX, McuAssignments.F1);
        assignmentMap.put(McuFunction.MODE_TRACK_REMOTE, McuAssignments.F2);
        assignmentMap.put(McuFunction.MODE_PROJECT_REMOTE, McuAssignments.F3);
        assignmentMap.put(McuFunction.MODE_SEND, McuAssignments.V_SEND);
        assignmentMap.put(McuFunction.BANK_LEFT, McuAssignments.BANK_LEFT);
        assignmentMap.put(McuFunction.BANK_RIGHT, McuAssignments.BANK_RIGHT);
        assignmentMap.put(McuFunction.CHANNEL_LEFT, McuAssignments.TRACK_LEFT);
        assignmentMap.put(McuFunction.CHANNEL_RIGHT, McuAssignments.TRACK_RIGHT);
        assignmentMap.put(McuFunction.NAV_DOWN, McuAssignments.CURSOR_DOWN);
        assignmentMap.put(McuFunction.NAV_UP, McuAssignments.CURSOR_UP);
        assignmentMap.put(McuFunction.NAV_LEFT, McuAssignments.CURSOR_LEFT);
        assignmentMap.put(McuFunction.NAV_RIGHT, McuAssignments.CURSOR_RIGHT);
        assignmentMap.put(McuFunction.ZOOM, McuAssignments.ZOOM);
    }
    
    
    public boolean usesUnifiedDeviceControl() {
        return assignmentMap.containsKey(McuFunction.MODE_DEVICE);
    }
    
    public boolean hasDirectSelect() {
        return assignmentMap.containsKey(McuFunction.SEND_SELECT_1);
    }
    
    public ControllerConfig setAssignment(final McuFunction function, final ButtonAssignment assignment) {
        assignmentMap.entrySet().stream().filter(entry -> entry.getValue().equals(assignment)).findFirst()
            .ifPresent(entry -> assignmentMap.remove(entry.getKey()));
        assignmentMap.put(function, assignment);
        return this;
    }
    
    public ControllerConfig removeAssignment(final McuFunction function) {
        assignmentMap.remove(function);
        return this;
    }
    
    public boolean isHas2ClickResolution() {
        return has2ClickResolution;
    }
    
    public ControllerConfig setHas2ClickResolution(final boolean value) {
        this.has2ClickResolution = value;
        return this;
    }
    
    public ControllerConfig setHasMasterVu(final boolean hasMasterVu) {
        this.hasMasterVu = hasMasterVu;
        return this;
    }
    
    public ControllerConfig setJogWheelCoding(final EncoderBehavior behavior) {
        this.jogWheelBehavior = behavior;
        return this;
    }
    
    public ControllerConfig setHasTimeCodeLed(final boolean hasTimeCodeLed) {
        this.hasTimeCodeLed = hasTimeCodeLed;
        return this;
    }
    
    public ControllerConfig setHasIconTrackColoring(final boolean hasIconTrackColoring) {
        this.hasIconTrackColoring = hasIconTrackColoring;
        return this;
    }
    
    public ControllerConfig setNavigationWithJogWheel(final boolean navigationWithJogWheel) {
        this.navigationWithJogWheel = navigationWithJogWheel;
        return this;
    }
    
    public boolean isSingleMainUnit() {
        return singleMainUnit;
    }
    
    public ControllerConfig setSingleMainUnit(final boolean singleMainUnit) {
        this.singleMainUnit = singleMainUnit;
        return this;
    }
    
    public boolean hasNavigationWithJogWheel() {
        return navigationWithJogWheel;
    }
    
    public boolean hasIconTrackColoring() {
        return hasIconTrackColoring;
    }
    
    public boolean isDisplaySegmented() {
        return displaySegmented;
    }
    
    public ControllerConfig setDisplaySegmented(final boolean displaySegmented) {
        this.displaySegmented = displaySegmented;
        return this;
    }
    
    public ControllerConfig setHasMasterFader(final int masterFaderChannel) {
        this.masterFaderChannel = masterFaderChannel;
        return this;
    }
    
    public ControllerConfig setHasLowerDisplay(final boolean hasLowerDisplay) {
        this.hasLowerDisplay = hasLowerDisplay;
        return this;
    }
    
    public int getNrOfExtenders() {
        return nrOfExtenders;
    }
    
    public ControllerConfig setNrOfExtenders(final int nrOfExtenders) {
        this.nrOfExtenders = nrOfExtenders;
        return this;
    }
    
    public EncoderBehavior getJogWheelBehavior() {
        return jogWheelBehavior;
    }
    
    public boolean isFunctionSectionLayered() {
        return functionSectionLayered;
    }
    
    public ControllerConfig setFunctionSectionLayered(final boolean functionSectionLayer) {
        functionSectionLayered = functionSectionLayer;
        return this;
    }
    
    public boolean isUseClearDuplicateModifiers() {
        return useClearDuplicateModifiers;
    }
    
    public ControllerConfig setUseClearDuplicateModifiers(final boolean useClearDuplicateModifiers) {
        this.useClearDuplicateModifiers = useClearDuplicateModifiers;
        return this;
    }
    
    public boolean isTopDisplayRowsFlipped() {
        return topDisplayRowsFlipped;
    }
    
    public ControllerConfig setTopDisplayRowsFlipped(final boolean topDisplayRowsFlipped) {
        this.topDisplayRowsFlipped = topDisplayRowsFlipped;
        return this;
    }
    
    public boolean isHasDedicateVu() {
        return hasDedicateVu;
    }
    
    public ControllerConfig setHasDedicateVu(final boolean hasDedicateVu) {
        this.hasDedicateVu = hasDedicateVu;
        return this;
    }
    
    public boolean hasLowerDisplay() {
        return hasLowerDisplay;
    }
    
    public ManufacturerType getManufacturerType() {
        return manufacturerType;
    }
    
    public SubType getSubType() {
        return subType;
    }
    
    public boolean hasMasterVu() {
        return hasMasterVu;
    }
    
    public ButtonAssignment getAssignment(final McuFunction function) {
        return assignmentMap.get(function);
    }
    
    public int getMasterFaderChannel() {
        return masterFaderChannel;
    }
    
    public boolean hasTimecodeLed() {
        return this.hasTimeCodeLed;
    }
    
    public boolean hasAssignment(final McuFunction function) {
        return assignmentMap.containsKey(function);
    }
}
