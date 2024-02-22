package com.bitwig.extensions.controllers.mcu.display;

import com.bitwig.extensions.framework.values.BooleanValueObject;

import java.util.List;

public interface ControllerDisplay {

    void showText(DisplayPart part, int row, int cell, String text);

    void showText(DisplayPart part, int row, List<String> text);

    void showText(DisplayPart part, int row, String text);

    void refresh();

    boolean hasLower();

    BooleanValueObject getSlidersTouched();
    
    void sendVuUpdate(final int index, final int value);

    void sendMasterVuUpdateL(final int value);

    void sendMasterVuUpdateR(final int value);

    void blockUpdate(DisplayPart part, int row);

    void enableUpdate(DisplayPart part, int row);
}
