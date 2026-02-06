package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display;

import java.util.HashMap;
import java.util.Map;

import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchControlMidiProcessor;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchControlMk3Extension;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.definition.AbstractLaunchControlExtensionDefinition;
import com.bitwig.extensions.framework.di.Component;

@Component
public class DisplayControl {
    private static final byte[] TEXT_CONFIG_COMMAND =
        {(byte) 0xF0, 0x00, 0x20, 0x29, 0x02, 0x15, 0x04, 0x00, 0x00, (byte) 0xF7};
    private final String textCommandHeader;
    private final LaunchControlMidiProcessor midiProcessor;
    private final DisplaySegment fixedDisplay;
    private final DisplaySegment temporaryDisplay;
    private FixDisplayState fixedState = FixDisplayState.TRACK;
    private final Map<Integer, Integer> targetConfigs = new HashMap<>();
    private QueuedTextMessage waitingMessage = null;
    
    private record QueuedTextMessage(int targetId, int config, String line1, String line2) {
    
    }
    
    public DisplayControl(final LaunchControlMidiProcessor processor,
        final AbstractLaunchControlExtensionDefinition definition) {
        TEXT_CONFIG_COMMAND[5] = (byte) (definition.isXlVersion() ? 0x15 : 0x16);
        this.midiProcessor = processor;
        this.fixedDisplay = new DisplaySegment(0x35, this);
        this.temporaryDisplay = new DisplaySegment(0x36, this);
        textCommandHeader = processor.getSysexHeader() + "06 ";
        midiProcessor.addStartListener(this::initTemps);
        midiProcessor.addTimedListener(this::updateQuedMessages);
    }
    
    private void updateQuedMessages() {
        if (waitingMessage != null) {
            LaunchControlMk3Extension.println(
                " SHOW MESSAGE %02X %02X L1=%s L2=%s", waitingMessage.targetId, waitingMessage.config,
                waitingMessage.line1, waitingMessage.line2);
            configureDisplay(waitingMessage.targetId, waitingMessage.config);
            setText(waitingMessage.targetId, 0, waitingMessage.line1);
            setText(waitingMessage.targetId, 1, waitingMessage.line2);
            showDisplay(waitingMessage.targetId);
            waitingMessage = null;
        }
    }
    
    public void configureDisplay(final int targetId, final int config) {
        targetConfigs.put(targetId, config);
        TEXT_CONFIG_COMMAND[7] = (byte) targetId;
        TEXT_CONFIG_COMMAND[8] = (byte) config;
        midiProcessor.sendSysExBytes(TEXT_CONFIG_COMMAND);
    }
    
    public void displayParamNames(final String... paramNames) {
        this.fixedState = FixDisplayState.PARAM;
        this.fixedDisplay.showParamInfo(paramNames);
    }
    
    public void revertToFixed() {
        this.temporaryDisplay.set2Lines(this.fixedDisplay);
        this.temporaryDisplay.update2Lines();
    }
    
    public void fixDisplayUpdate(final int lineIndex, final String text, final long lastBlockTime) {
        this.fixedDisplay.setLine(lineIndex, text);
        final long diff = System.currentTimeMillis() - lastBlockTime;
        if (diff > 1000) {
            temporaryDisplay.setLine(lineIndex, text);
            temporaryDisplay.update2Lines();
        }
        if (this.fixedState == FixDisplayState.TRACK) {
            this.fixedDisplay.update2Lines();
        }
    }
    
    public void show2LineTemporary(final String line1, final String line2) {
        temporaryDisplay.setLine(0, line1);
        temporaryDisplay.setLine(1, line2);
        temporaryDisplay.update2Lines();
    }
    
    public void cancelTemporary() {
        hideDisplay(0x36);
    }
    
    public void updateStatic() {
        showDisplay(0x35);
    }
    
    public DisplaySegment getTemporaryDisplay() {
        return temporaryDisplay;
    }
    
    public DisplaySegment getFixedDisplay() {
        return fixedDisplay;
    }
    
    public void showDisplay(final int targetId) {
        TEXT_CONFIG_COMMAND[7] = (byte) targetId;
        TEXT_CONFIG_COMMAND[8] = 62;
        midiProcessor.sendSysExBytes(TEXT_CONFIG_COMMAND);
    }
    
    public void hideDisplay(final int targetId) {
        TEXT_CONFIG_COMMAND[7] = (byte) targetId;
        TEXT_CONFIG_COMMAND[8] = 0;
        midiProcessor.sendSysExBytes(TEXT_CONFIG_COMMAND);
    }
    
    public void setText(final int target, final int field, final String text) {
        final StringBuilder msg = new StringBuilder(textCommandHeader);
        msg.append("%02X ".formatted(target));
        msg.append("%02X ".formatted(field));
        final String validText = StringUtil.toAsciiDisplay(text, 16);
        for (int i = 0; i < validText.length(); i++) {
            msg.append("%02X ".formatted((int) validText.charAt(i)));
        }
        msg.append("F7");
        midiProcessor.sendSysExString(msg.toString());
    }
    
    public void initTemps() {
        configureDisplay(0x21, 0x61);
        configureDisplay(0x20, 0x61);
        for (int i = 0; i < 24; i++) {
            configureDisplay(0x05 + i, 0x62);
        }
        configureDisplay(0x22, 0x01);
        configureDisplay(0x23, 0x61);
        configureDisplay(0x24, 0x01);
        configureDisplay(0x25, 0x01);
        configureDisplay(0x26, 0x01);
        configureDisplay(0x27, 0x01);
    }
    
    public void queue2LineMessage(final int targetId, final int config, final String line1, final String line2) {
        waitingMessage = new QueuedTextMessage(targetId, config, line1, line2);
    }
}
