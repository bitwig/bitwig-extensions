package com.bitwig.extensions.controllers.akai.mpkmk4;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class MpkMiniPlusControllerExtensionDefinition extends ControllerExtensionDefinition {
    
    private final static UUID ID = UUID.fromString("3ad31d42-81d1-a603-8719-d0462633d943");
    
    @Override
    public String getHardwareVendor() {
        return "Akai";
    }
    
    @Override
    public String getHardwareModel() {
        return "MPK Mini Plus";
    }
    
    @Override
    public int getNumMidiInPorts() {
        return 2;
    }
    
    @Override
    public int getNumMidiOutPorts() {
        return 2;
    }
    
    @Override
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
        final PlatformType platformType) {
        if (platformType == PlatformType.WINDOWS) {
            list.add(
                new String[] {"MIDIIN2 (MPK mini Plus II)", "MPK mini Plus II"},
                new String[] {"MIDIOUT2 (MPK mini Plus II)", "MPK mini Plus II"});
        } else if (platformType == PlatformType.MAC) {
            list.add(
                new String[] {"MPK mini Plus II DAW Port", "MPK mini Plus II MIDI Port"},
                new String[] {"MPK mini Plus II DAW Port", "MPK mini Plus II MIDI Port"});
        } else if (platformType == PlatformType.LINUX) {
            list.add(
                new String[] {"MPK mini Plus II MPK mini Plus II DAW Por", "MPK mini IV MPK mini IV MIDI Po"},
                new String[] {"MPK mini Plus II DAW Port", "MPK mini Plus II MIDI Port"});
        }
    }
    
    @Override
    public ControllerExtension createInstance(final ControllerHost host) {
        return new MpkMk4ControllerExtension(this, host, MpkMk4ControllerExtension.Variant.MINI_PLUS);
    }
    
    @Override
    public String getHelpFilePath() {
        return "Controllers/Akai/MPK Mini Mk4.pdf";
    }
    
    @Override
    public String getName() {
        return "MPK Mini IV";
    }
    
    @Override
    public String getAuthor() {
        return "Bitwig";
    }
    
    @Override
    public String getVersion() {
        return "1.0";
    }
    
    @Override
    public UUID getId() {
        return ID;
    }
    
    @Override
    public int getRequiredAPIVersion() {
        return 24;
    }
    
    
}
