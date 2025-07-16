package com.bitwig.extensions.controllers.nativeinstruments.komplete.device;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DirectParameterValueDisplayObserver;
import com.bitwig.extension.controller.api.IntegerValue;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.ControlElements;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.KompleteKontrolExtension;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.midi.MidiProcessor;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.values.BasicIntegerValue;
import com.bitwig.extensions.framework.values.BooleanValueObject;

public class DirectParameterControl extends AbstractParameterControl {
    public static final int MAX_PARAMS = 128;
    private final MidiProcessor midiProcessor;
    private final Device device;
    private final DirectParameterValueDisplayObserver parameterObserver;
    private final List<DirectSlot> allSlots = new ArrayList<>();
    private final List<DirectControlSlot> pageSlots = new ArrayList<>();
    private final HashMap<String, DirectSlot> mapping = new HashMap<>();
    private final HashMap<String, DirectControlSlot> activeMapping = new HashMap<>();
    private final HashMap<String, Integer> pageIndexMap = new HashMap<>();
    
    private int pageIndex = 0;
    private final BasicIntegerValue pageCount = new BasicIntegerValue();
    private final BooleanValueObject directActive = new BooleanValueObject();
    private int remotePages;
    private boolean isPlugin;
    private String deviceName;
    private boolean fineTune;
    
    public DirectParameterControl(final Layer layer, final CursorDevice device, final ControlElements controlElements,
        final MidiProcessor midiProcessor, final IntegerValue remotePageCount) {
        super(layer);
        this.midiProcessor = midiProcessor;
        this.device = device;
        for (int i = 0; i < MAX_PARAMS; i++) {
            allSlots.add(new DirectSlot(i));
        }
        for (int i = 0; i < 8; i++) {
            final int index = i;
            final DirectControlSlot pageSlot = new DirectControlSlot(index);
            pageSlots.add(pageSlot);
            final RelativeHardwareKnob knob = controlElements.getDeviceKnobs().get(i);
            layer.bind(knob, midiProcessor.createIncDoubleAction(inc -> handleKnobChange(index, inc)));
            layer.addBinding(new KnobDirectBinding(i, knob, pageSlot, midiProcessor));
        }
        applySlotsToIndex();
        this.device.addDirectParameterIdObserver(this::handleParameterIds);
        parameterObserver = this.device.addDirectParameterValueDisplayObserver(14, this::handleParameterValueChanged);
        this.device.addDirectParameterNormalizedValueObserver(this::handleValueChanged);
        this.device.addDirectParameterNameObserver(15, this::handleParameterNames);
        this.device.name().addValueObserver(this::handleDeviceName);
        remotePageCount.addValueObserver(this::handleRemotePageCount);
        pageCount.addValueObserver(this::handlePageCount);
        this.device.isPlugin().addValueObserver(this::handleIsPlugin);
    }
    
    private void applySlotsToIndex() {
        activeMapping.clear();
        if (pageCount.get() == 0) {
            return;
        }
        for (int i = 0; i < 8; i++) {
            final DirectControlSlot directSlot = pageSlots.get(i);
            final DirectSlot mainSlot = allSlots.get(pageIndex * 8 + i);
            directSlot.apply(mainSlot);
            if (directSlot.getParamId() != null) {
                activeMapping.put(directSlot.getParamId(), directSlot);
                activeMapping.put(directSlot.getInParamId(), directSlot);
            }
        }
    }
    
    @Override
    public void setActive(final boolean active) {
        super.setActive(active);
        updatePageCount();
    }
    
    private void handleIsPlugin(final boolean isPlugin) {
        this.isPlugin = isPlugin;
        updateDirectState();
    }
    
    private void handleRemotePageCount(final int remotePages) {
        this.remotePages = remotePages;
        updateDirectState();
    }
    
    public BooleanValueObject getDirectActive() {
        return directActive;
    }
    
    private void updateDirectState() {
        this.directActive.set(this.remotePages == 0 && this.isPlugin);
    }
    
    private void handleDeviceName(final String deviceName) {
        if (this.deviceName != null && !this.deviceName.isBlank()) {
            pageIndexMap.put(this.deviceName, pageIndex);
        }
        this.deviceName = deviceName;
        if (this.deviceName != null && !this.deviceName.isBlank()) {
            final Integer storedIndex = pageIndexMap.get(deviceName);
            if (storedIndex != null && storedIndex != pageIndex) {
                this.pageIndex = storedIndex;
                applySlotsToIndex();
                updatePageCount();
            } else if (storedIndex == null) {
                this.pageIndex = 0;
                applySlotsToIndex();
                updatePageCount();
            }
        }
    }
    
    private void handleKnobChange(final int index, final double inc) {
        if (!isActive()) {
            return;
        }
        final DirectControlSlot directSlot = pageSlots.get(index);
        if (directSlot.getParamId() != null) {
            if (!directSlot.isStepped()) {
                this.device.incDirectParameterValueNormalized(directSlot.getParamId(), inc, fineTune ? 40.0 : 4.0);
                directSlot.notifyIncrement(inc);
            } else {
                final int incInt = (int) (inc / 0.0078) / 4;
                directSlot.resetIncrements();
                final int preValue = directSlot.getValue();
                final int newValue = Math.max(0, Math.min(127, directSlot.getValue() + incInt));
                if (preValue != newValue) {
                    directSlot.setValue(newValue);
                    this.device.setDirectParameterValueNormalized(directSlot.getParamId(), newValue, MAX_PARAMS);
                }
            }
        }
    }
    
    
    private void handleKnobChange_(final int index, final double inc) {
        if (!isActive()) {
            return;
        }
        final DirectControlSlot directSlot = pageSlots.get(index);
        if (directSlot.getParamId() != null) {
            KompleteKontrolExtension.println(" CHANGE > " + inc);
            
        }
    }
    
    private void handleValueChanged(final String paramId, final double v) {
        final DirectSlot slot = mapping.get(paramId);
        if (slot != null) {
            final int value = (int) Math.round(v * 127);
            slot.setValue(value);
        }
        final DirectControlSlot directSlot = activeMapping.get(paramId);
        if (directSlot != null && !directSlot.isStepped()) {
            final int value = (int) Math.round(v * 127);
            directSlot.setValue(value);
        }
    }
    
    private void handleParameterValueChanged(final String paramId, final String value) {
        final DirectSlot slot = mapping.get(paramId);
        if (slot != null) {
            slot.getParamValue().set(value);
        }
        final DirectControlSlot directSlot = activeMapping.get(paramId);
        if (directSlot != null) {
            directSlot.setValueString(value);
        }
    }
    
    private void handleParameterIds(final String[] ids) {
        final List<String> observed = new ArrayList<>();
        mapping.clear();
        int assigned = 0;
        for (int i = 0; i < allSlots.size(); i++) {
            final DirectSlot slot = allSlots.get(i);
            if (i < ids.length) {
                assigned++;
                slot.setParamId(ids[i]);
                observed.add(slot.getInParamId());
                observed.add(slot.getParamId());
                mapping.put(slot.getParamId(), slot);
                mapping.put(slot.getInParamId(), slot);
            } else {
                slot.setParamId(null);
            }
        }
        pageCount.set((assigned + 7) / 8);
        final String[] idsToObserve = observed.stream().toArray(String[]::new);
        midiProcessor.delay(() -> parameterObserver.setObservedParameterIds(idsToObserve), 50);
        //parameterObserver.setObservedParameterIds(idsToObserve);
        applySlotsToIndex();
    }
    
    private void handleParameterNames(final String paramId, final String paramName) {
        final DirectSlot slot = mapping.get(paramId);
        if (slot != null) {
            slot.getParamName().set(paramName);
        }
        final DirectSlot directSlot = activeMapping.get(paramId);
        if (directSlot != null) {
            directSlot.getParamName().set(paramName);
        }
    }
    
    private void handlePageCount(final int pageCount) {
        if (pageCount == -1) {
            return;
        }
        updatePageCount();
    }
    
    private void updatePageCount() {
        if (isActive()) {
            midiProcessor.sendPageCount(this.pageCount.get(), pageIndex);
            midiProcessor.sendSection(0, "Page %d".formatted(pageIndex + 1));
        }
    }
    
    @Override
    public void navigateLeft() {
        if (canScrollLeft()) {
            this.pageIndex -= 1;
            applySlotsToIndex();
            updatePageCount();
        }
    }
    
    @Override
    public void navigateRight() {
        if (canScrollRight()) {
            this.pageIndex += 1;
            applySlotsToIndex();
            updatePageCount();
        }
    }
    
    @Override
    public void setFineTune(final boolean fineTune) {
        this.fineTune = fineTune;
    }
    
    @Override
    public boolean canScrollRight() {
        return pageIndex + 1 < pageCount.get();
    }
    
    @Override
    public boolean canScrollLeft() {
        return pageIndex > 0;
    }
}
