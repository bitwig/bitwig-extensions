package com.bitwig.extensions.controllers.nativeinstruments.komplete.device;

import com.bitwig.extension.callback.IntegerValueChangedCallback;
import com.bitwig.extensions.framework.values.BasicIntegerValue;
import com.bitwig.extensions.framework.values.BasicStringValue;

public class DirectSlot {
    private final static String ID_PREFIX = "CONTENTS/";
    protected final BasicStringValue paramName = new BasicStringValue();
    protected final BasicStringValue paramValue = new BasicStringValue();
    protected final BasicIntegerValue value = new BasicIntegerValue();
    protected String paramId;
    private final int index;
    
    public DirectSlot(final int index) {
        this.index = index;
    }
    
    public void addValueObserver(final IntegerValueChangedCallback callback) {
        value.addValueObserver(callback);
    }
    
    public BasicStringValue getParamName() {
        return paramName;
    }
    
    public BasicStringValue getParamValue() {
        return paramValue;
    }
    
    public void setValueString(final String value) {
        this.paramValue.set(value);
    }
    
    public void setValue(final int value) {
        this.value.set(value);
    }
    
    public int getValue() {
        return value.get();
    }
    
    public void setParamId(final String paramId) {
        this.paramId = paramId;
    }
    
    public String getParamId() {
        return paramId;
    }
    
    public String getBaseParam() {
        final int last = paramId.lastIndexOf("/");
        return paramId.substring(last + 1);
    }
    
    public String getInParamId() {
        if (paramId != null && paramId.startsWith(ID_PREFIX)) {
            return "%sROOT_GENERIC_MODULE/%s".formatted(ID_PREFIX, paramId.substring(ID_PREFIX.length()));
        }
        return paramId;
    }
    
    public boolean exists() {
        return paramId != null;
    }
    
    public int getIndex() {
        return index;
    }
    
}
