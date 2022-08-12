package com.bitwig.extensions.controllers.m_audio;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public abstract class OxygenProDefinition extends ControllerExtensionDefinition {

    abstract String getModel();

    @Override
    public ControllerExtension createInstance(ControllerHost host) {
        // TODO Auto-generated method stub
        return new OxygenPro(this, host, getModel());
    }

    @Override
    public String getHardwareModel() {
        // TODO Auto-generated method stub
        return "Oxygen Pro " + getModel();
    }

    @Override
    public String getHardwareVendor() {
        // TODO Auto-generated method stub
        return "M-Audio";
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

    @Override
    public void listAutoDetectionMidiPortNames(AutoDetectionMidiPortNamesList list, PlatformType platformType) {
        // TODO Auto-generated method stub
        switch (platformType)
      {
         case MAC:
            list.add(new String[]{"Oxygen Pro "+ getModel() + " USB MIDI", "Oxygen Pro "+ getModel() +" Mackie/HUI"}, new String[]{"Oxygen Pro "+ getModel() +" Editor", "Oxygen Pro "+ getModel() +" Mackie/HUI"});
            break;
         case WINDOWS:
            // TODO: Edit
            list.add(new String[]{"Oxygen Pro "+ getModel() +" USB MIDI", "Oxygen Pro "+ getModel() +" Mackie/HUI"}, new String[]{"Oxygen Pro "+ getModel() +" Editor", "Oxygen Pro "+ getModel() +" Mackie/HUI"});
            break;
         case LINUX:
            // TODO: Edit
            list.add(new String[]{"Oxygen Pro "+ getModel() +" USB MIDI", "Oxygen Pro "+ getModel() +" Mackie/HUI"}, new String[]{"Oxygen Pro "+ getModel() +" Editor", "Oxygen Pro "+ getModel() +" Mackie/HUI"});
            break;
      }
        
    }

    @Override
    public String getAuthor() {
        // TODO Auto-generated method stub
        return "Bitwig";
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return "M-Audio Oxygen Pro " + getModel();
    }

    @Override
    public int getRequiredAPIVersion() {
        // TODO Auto-generated method stub
        return 16;
    }

    @Override
    public String getVersion() {
        // TODO Auto-generated method stub
        return "1.0";
    }

    @java.lang.Override
    public java.lang.String getHelpFilePath() {
       return "Controllers/M-Audio/M-Audio Oxygen Pro 25.pdf";
    }
}
