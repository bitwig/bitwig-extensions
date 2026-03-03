package com.bitwig.extensions.controllers.softube.console1;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;

public class ConsoleMidiProcessor {
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("hh:mm:ss SSS");
    
    private MidiOut midiOut = null;
    private final ControllerHost host;
    private boolean connectionInit = false;
    
    private TrackControl trackSlotControl;
    private final int portCount;
    private final List<ConnectionListener> connectionListeners = new ArrayList<>();
    
    private boolean engineActiveState = false;
    private final String name;
    
    public ConsoleMidiProcessor(final ControllerHost host, final int ports, final String name) {
        this.host = host;
        this.name = name;
        this.portCount = ports;
        for (int i = 0; i < ports; i++) {
            final int portIndex = i;
            final MidiIn midiIn = host.getMidiInPort(i);
            midiIn.setSysexCallback(data -> handleSysEx(portIndex, data));
        }
    }
    
    private void println(final String format, final Object... args) {
        host.println(" " + LocalDateTime.now().format(DF) + " " + format.formatted(args));
    }
    
    public void startHandshake() {
        for (int port = 0; port < portCount; port++) {
            println(" ######### Init Handshake %d ###### ", port);
            host.getMidiOutPort(port).sendSysex(SysexUtil.toJsonSysEx(SysexUtil.INIT_HANDSHAKE));
        }
    }
    
    public void addConnectionListener(final ConnectionListener listener) {
        this.connectionListeners.add(listener);
    }
    
    public void invokeReset() {
        // println(" Daw-> Console : Reset");
        for (int i = 0; i < portCount; i++) {
            host.getMidiOutPort(i).sendSysex(SysexUtil.toJsonSysEx(SysexUtil.RESET_CMD));
        }
    }
    
    public void setTrackSlotControl(final TrackControl trackSlotControl) {
        this.trackSlotControl = trackSlotControl;
    }
    
    public void sendJsonSysEx(final String jsonString) {
        if (midiOut == null) {
            return;
        }
        midiOut.sendSysex(SysexUtil.toJsonSysEx(jsonString));
    }
    
    private void handleSysEx(final int port, final String data) {
        final String json = SysexUtil.toJson(data);
        if (json != null) {
            handleJson(port, json);
        } else {
            println("NON JSON : \n" + data);
        }
    }
    
    private void handleJson(final int port, final String json) {
        final JsonParser parser = new JsonParser(json);
        final JsonObject obj = parser.parse();
        //host.println(" JSON " + json);
        if (obj.contains("trackId")) {
            trackSlotControl.update(obj);
        } else if (obj.contains("handshake")) {
            final boolean acknowledge = obj.getJsonObject("handshake").getBool("ack");
            handleHandshake(port, acknowledge);
        } else if (obj.contains("cmd")) {
            final String cmdValue = obj.getString("cmd");
            if ("RESET".equals(cmdValue)) {
                handleResetReceived();
            } else if ("ENABLE".equals(cmdValue)) {
                println(" ENABLE <%s>", name);
            } else if ("DISABLE".equals(cmdValue)) {
                println(" DISABLE <%s>", name);
                connectionListeners.forEach(listener -> listener.isConnected(false));
            } else {
                println(" Unknown COMMAND %s", cmdValue);
            }
        } else if (obj.contains("activeMeters")) {
            trackSlotControl.meterActivate(obj.getStringList("activeMeters"));
        } else {
            host.println("OTHER JSON : \n" + json);
        }
    }
    
    private void handleResetReceived() {
        println(" RESET received INIT=%s", connectionInit);
        if (!connectionInit) {
            trackSlotControl.setBlockMidi(false);
            startHandshake();
        } else {
            trackSlotControl.updateAllTracks();
        }
    }
    
    private void handleHandshake(final int port, final boolean acknowledged) {
        println(
            " C1 => Handshake Acknowledge=%s con_init=%s> send reset => PORT=%d  ", acknowledged, connectionInit, port);
        if (acknowledged) {
            connectToDevice(port);
        } else {
            if (connectionInit) {
                println(" Let us ignore this ");
            } else {
                connectionInit = false;
                host.getMidiOutPort(port).sendSysex(SysexUtil.toJsonSysEx(SysexUtil.RESET_CMD));
                connectionListeners.forEach(listener -> listener.isConnected(false));
            }
        }
    }
    
    private void connectToDevice(final int port) {
        // println(" ####### Connect to Device %d  ########### ", port);
        midiOut = host.getMidiOutPort(port);
        
        host.scheduleTask(this::launchTrackUpdate, 500);
        if (!connectionInit) {
            connectionInit = true;
            connectionListeners.forEach(listener -> listener.isConnected(true));
        }
        
    }
    
    private void launchTrackUpdate() {
        trackSlotControl.setBlockMidi(false);
        trackSlotControl.updateAllTracks();
    }
    
    public void handleEngineSwitch(final boolean engineActive) {
        if (engineActiveState == engineActive) {
            return;
        }
        this.engineActiveState = engineActive;
        if (!connectionInit) {
            return;
        }
        // println(" Engine Active %s init = %s", engineActive, connectionInit);
        if (connectionInit && engineActive) {
            trackSlotControl.resetAll();
            invokeReset();
            trackSlotControl.updateAllTracks();
        }
    }
}
