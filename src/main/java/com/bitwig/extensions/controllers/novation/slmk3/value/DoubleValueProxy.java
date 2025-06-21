package com.bitwig.extensions.controllers.novation.slmk3.value;

import com.bitwig.extension.callback.StringValueChangedCallback;

public interface DoubleValueProxy {
    void set(double value);
    
    double get();
    
    String getDisplayValue();
    
    void addDisplayValueListener(StringValueChangedCallback listener);
}
