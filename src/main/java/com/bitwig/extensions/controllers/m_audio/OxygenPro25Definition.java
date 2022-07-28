package com.bitwig.extensions.controllers.m_audio;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class OxygenPro25Definition extends ControllerExtensionDefinition {

    private static final UUID DRIVER_ID = UUID.fromString("7ef7baa1-3abe-4553-9f5f-50d021ebda9e");

    @Override
    public String getHardwareVendor() {
        // TODO Auto-generated method stub
        return "M-Audio";
    }

    @Override
    public String getHardwareModel() {
        // TODO Auto-generated method stub
        return "Oxygen Pro 25";
    }

    @Override
    public int getNumMidiInPorts() {
        // TODO Auto-generated method stub
        return 2;
    }

    @Override
    public int getNumMidiOutPorts() {
        // TODO Auto-generated method stub
        return 2;
    }
    @java.lang.Override
    public java.lang.String getHelpFilePath()
    {
       return "Controllers/M-Audio/M-Audio Oxygen Pro 25.pdf";
    }
    @Override
    public void listAutoDetectionMidiPortNames(AutoDetectionMidiPortNamesList list, PlatformType platformType) {
        // TODO Auto-generated method stub
        switch (platformType)
      {
         case MAC:
            list.add(new String[]{"Oxygen Pro 25 USB MIDI", "Oxygen Pro 25 Mackie/HUI"}, new String[]{"Oxygen Pro 25 Editor", "Oxygen Pro 25 Mackie/HUI"});
            break;
         case WINDOWS:
            // TODO: Edit
            list.add(new String[]{"Oxygen Pro 25 USB MIDI", "Oxygen Pro 25 Mackie/HUI"}, new String[]{"Oxygen Pro 25 Editor", "Oxygen Pro 25 Mackie/HUI"});
            break;
         case LINUX:
            // TODO: Edit
            list.add(new String[]{"Oxygen Pro 25 USB MIDI", "Oxygen Pro 25 Mackie/HUI"}, new String[]{"Oxygen Pro 25 Editor", "Oxygen Pro 25 Mackie/HUI"});
            break;
      }
        
    }

    @Override
    public ControllerExtension createInstance(ControllerHost host) {
        // TODO Auto-generated method stub
        return new OxygenPro25(this, host);
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return "M-Audio Oxygen Pro 25";
    }

    @Override
    public String getAuthor() {
        // TODO Auto-generated method stub
        return "Bitwig";
    }

    @Override
    public String getVersion() {
        // TODO Auto-generated method stub
        return "1.0";
    }

    @Override
    public UUID getId() {
        // TODO Auto-generated method stub
        return DRIVER_ID;
    }

    @Override
    public int getRequiredAPIVersion() {
        // TODO Auto-generated method stub
        return 16;
    }


}
