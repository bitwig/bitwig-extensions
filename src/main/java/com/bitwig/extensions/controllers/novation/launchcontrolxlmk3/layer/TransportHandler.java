package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.layer;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DocumentState;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.CcConstValues;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchControlXlHwElements;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchViewControl;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchButton;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.RgbColorState;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.bitwig.extensions.framework.values.FocusMode;

@Component
public class TransportHandler {
    
    private final BooleanValueObject shiftState;
    private final CursorTrack cursorTrack;
    private FocusMode focusMode = FocusMode.LAUNCHER;
    private final Transport transport;
    private final LaunchControlXlHwElements hwElements;
    private final LaunchViewControl viewControl;
    
    public TransportHandler(final ControllerHost host, final LaunchControlXlHwElements hwElements,
        final LaunchViewControl viewControl, final Transport transport) {
        this.transport = transport;
        this.shiftState = hwElements.getShiftState();
        this.viewControl = viewControl;
        this.cursorTrack = viewControl.getCursorTrack();
        this.hwElements = hwElements;
        cursorTrack.position().markInterested();
        cursorTrack.channelIndex().markInterested();
        
        final TrackBank trackBank = viewControl.getTrackBank();
        trackBank.itemCount().addValueObserver(count -> {
        });
        final DocumentState documentState = host.getDocumentState();
        final SettableEnumValue focusMode = documentState.getEnumSetting(
            "Focus", //
            "Recording/Automation",
            new String[] {FocusMode.LAUNCHER.getDescriptor(), FocusMode.ARRANGER.getDescriptor()},
            FocusMode.ARRANGER.getDescriptor());
        focusMode.addValueObserver(mode -> this.focusMode = FocusMode.toMode(mode));
    }
    
    public Transport getTransport() {
        return transport;
    }
    
    public void bindTransport(final Layer layer) {
        final LaunchButton playButton = hwElements.getButton(CcConstValues.PLAY);
        final LaunchButton recButton = hwElements.getButton(CcConstValues.RECORD);
        transport.isPlaying().markInterested();
        transport.isClipLauncherOverdubEnabled().markInterested();
        transport.isArrangerRecordEnabled().markInterested();
        playButton.bindLight(layer, this::getPlayState);
        playButton.bindPressed(layer, this::handlePlayPressed);
        recButton.bindLight(layer, this::getRecordState);
        recButton.bindPressed(layer, this::handleRecordPressed);
    }
    
    public void bindTrackNavigation(final Layer layer) {
        final LaunchButton trackLeftButton = hwElements.getButton(CcConstValues.TRACK_LEFT);
        final LaunchButton trackRightButton = hwElements.getButton(CcConstValues.TRACK_RIGHT);
        
        trackLeftButton.bindLight(layer, () -> canNavLeft(cursorTrack) ? RgbState.WHITE : RgbState.OFF);
        trackRightButton.bindLight(layer, () -> canNavRight(cursorTrack) ? RgbState.WHITE : RgbState.OFF);
        
        trackRightButton.bindRepeatHold(layer, this::navRight);
        trackLeftButton.bindRepeatHold(layer, this::navLeft);
    }
    
    private void handlePlayPressed() {
        if (shiftState.get()) {
            transport.continuePlayback();
        } else {
            transport.play();
        }
    }
    
    private void handleRecordPressed() {
        if (focusMode == FocusMode.LAUNCHER) {
            transport.isClipLauncherOverdubEnabled().toggle();
        } else {
            transport.isArrangerRecordEnabled().toggle();
        }
    }
    
    private RgbColorState getPlayState() {
        return transport.isPlaying().get() ? RgbColorState.GREEN_FULL : RgbColorState.GREEN_DIM;
    }
    
    private RgbColorState getRecordState() {
        if (focusMode == FocusMode.LAUNCHER) {
            return transport.isClipLauncherOverdubEnabled().get()
                ? RgbColorState.RED_ORANGE_FULL
                : RgbColorState.RED_ORANGE_DIM;
        }
        return transport.isArrangerRecordEnabled().get() ? RgbColorState.RED_FULL : RgbColorState.RED_DIM;
    }
    
    public boolean canNavLeft(final CursorTrack cursorTrack) {
        return cursorTrack.hasPrevious().get();
    }
    
    public boolean canNavRight(final CursorTrack cursorTrack) {
        if (shiftState.get()) {
            return viewControl.canScrollBy(8);
        }
        return cursorTrack.hasNext().get();
    }
    
    public void navRight() {
        if (shiftState.get()) {
            viewControl.navigateCursorBy(8);
        } else {
            viewControl.navigateCursorBy(1);
        }
    }
    
    public void navLeft() {
        if (shiftState.get()) {
            viewControl.navigateCursorBy(-8);
        } else {
            viewControl.navigateCursorBy(-1);
        }
    }
    
}
