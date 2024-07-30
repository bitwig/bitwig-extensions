package com.bitwig.extensions.controllers.mcu.devices;

import com.bitwig.extension.controller.api.Parameter;

public interface DeviceManager {
    
    //void initiateBrowsing(final BrowserConfiguration browser, Type type);
    
    //void addBrowsing(final BrowserConfiguration browser, boolean after);
    
    //void setInfoLayer(DisplayLayer infoLayer);
    
    //void enableInfo(InfoSource type);
    
    void disableInfo();
    
    //InfoSource getInfoSource();
    
    Parameter getParameter(int index);
    
    //ParameterPage getParameterPage(int index);
    
    void navigateDeviceParameters(final int direction);
    
    //void handleResetInvoked(final int index, final ModifierValueObject modifier);
    
    DeviceTypeFollower getDeviceFollower();
    
    boolean isSpecificDevicePresent();
    
    int getPageCount();
    
}
