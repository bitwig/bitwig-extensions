package com.bitwig.extensions.controllers.novation.launchkey_mk4.sequencer;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.values.ValueSet;
import com.bitwig.extensions.framework.values.BasicStringValue;
import com.bitwig.extensions.framework.values.BooleanValueObject;

public class StepViewPosition {
    private double gridResolution;
    private final List<Clip> clips;
    
    public final int gridLength;
    private double loopLength = 0.0;
    private int pagePosition = 0;
    private int pages = 0;
    private int loopBound = 16;
    private int steps;
    
    private final BooleanValueObject canScrollLeft = new BooleanValueObject();
    private final BooleanValueObject canScrollRight = new BooleanValueObject();
    private final List<PagesChangedCallback> pagesChangedCallbacks = new ArrayList<>();
    private final BasicStringValue pagePositionDisplay = new BasicStringValue();
    
    public interface PagesChangedCallback {
        void notify(int pagePosition, int pages);
    }
    
    public StepViewPosition(final List<Clip> clips, final ValueSet gridResolutionValue, final int gridLength) {
        this.clips = clips;
        final Clip mainClip = clips.get(0);
        gridResolution = gridResolutionValue.getValue();
        gridResolutionValue.addValueObserver(s -> setGridResolution(gridResolutionValue.getValue()));
        
        this.gridLength = gridLength;
        clips.forEach(cl -> cl.setStepSize(gridResolution));
        mainClip.exists().addValueObserver(exist -> updatePagePositionDisplay());
        mainClip.getLoopLength().addValueObserver(this::handleLoopLengthChanged);
        mainClip.scrollToStep(pagePosition * gridLength);
    }
    
    public double lengthWithLastStep(final int index) {
        return gridResolution * (pagePosition * gridLength + index + 1);
    }
    
    public void addPagesChangedCallback(final PagesChangedCallback callback) {
        pagesChangedCallbacks.add(callback);
    }
    
    public void handleLoopLengthChanged(final double newLength) {
        loopLength = newLength;
        steps = (int) (loopLength / gridResolution);
        pages = Math.max(0, steps - 1) / gridLength + 1;
        updateStates();
        pagesChangedCallbacks.forEach(cb -> cb.notify(pagePosition, pages));
    }
    
    public int getCurrentPage() {
        return pagePosition;
    }
    
    public int getAvailableSteps() {
        return Math.max(0, steps - pagePosition * gridLength);
    }
    
    public int getPages() {
        return pages;
    }
    
    public void setGridResolution(final double resolution) {
        final double quote = gridResolution / resolution;
        gridResolution = resolution;
        
        clips.forEach(cl -> cl.setStepSize(gridResolution));
        pagePosition = (int) (pagePosition * quote);
        steps = (int) (loopLength / gridResolution);
        pages = Math.max(0, steps - 1) / gridLength + 1;
        clips.forEach(cl -> cl.scrollToStep(pagePosition * gridLength));
        updateStates();
        pagesChangedCallbacks.forEach(cb -> cb.notify(pagePosition, pages));
    }
    
    public BasicStringValue getPagePositionDisplay() {
        return pagePositionDisplay;
    }
    
    private void updatePagePositionDisplay() {
        pagePositionDisplay.set("%02d/%02d".formatted(pagePosition + 1, pages));
    }
    
    private void updateStates() {
        if (pagePosition < pages) {
            clips.forEach(cl -> cl.scrollToStep(pagePosition * gridLength));
        }
        
        this.loopBound = (int) Math.round(loopLength / gridResolution);
        updatePagePositionDisplay();
        canScrollLeft.set(pagePosition > 0);
        canScrollRight.set(pagePosition < pages - 1);
    }
    
    public BooleanValueObject canScrollLeft() {
        return canScrollLeft;
    }
    
    public BooleanValueObject canScrollRight() {
        return canScrollRight;
    }
    
    public void setPage(final int index) {
        pagePosition = index;
        clips.forEach(cl -> cl.scrollToStep(pagePosition * gridLength));
        updateStates();
    }
    
    public int getStepOffset() {
        return pagePosition * gridLength;
    }
    
    public double getPosition() {
        return pagePosition * gridResolution;
    }
    
    public double getGridResolution() {
        return gridResolution;
    }
    
    public void scrollLeft() {
        if (pagePosition > 0) {
            pagePosition--;
            clips.forEach(cl -> cl.scrollToStep(pagePosition * gridLength));
            updateStates();
            pagesChangedCallbacks.forEach(cb -> cb.notify(pagePosition, pages));
        }
    }
    
    public void scrollRight() {
        if (pagePosition < pages - 1) {
            pagePosition++;
            clips.forEach(cl -> cl.scrollToStep(pagePosition * gridLength));
            updateStates();
            pagesChangedCallbacks.forEach(cb -> cb.notify(pagePosition, pages));
        }
    }
    
    public boolean stepIndexInLoop(final int index) {
        return (pagePosition * gridLength + index) < loopBound;
    }
    
    public void setClipLengthByIndex(final int stepIndex) {
        final double newPos = pagePosition * 16 * gridResolution + (stepIndex + 1) * gridResolution;
        clips.get(0).getLoopLength().set(newPos);
    }
}
