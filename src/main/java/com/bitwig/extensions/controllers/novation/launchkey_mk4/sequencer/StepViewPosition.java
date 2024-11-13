package com.bitwig.extensions.controllers.novation.launchkey_mk4.sequencer;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.values.IntValue;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.values.ValueSet;
import com.bitwig.extensions.framework.values.BooleanValueObject;

public class StepViewPosition {
    private double gridResolution;
    private final List<Clip> clips;
    
    public final int gridLength;
    private double loopLength = 0.0;
    private final IntValue pagePosition = new IntValue();
    private int pages = 0;
    private int loopBound = 16;
    private int steps;
    
    private final BooleanValueObject canScrollLeft = new BooleanValueObject();
    private final BooleanValueObject canScrollRight = new BooleanValueObject();
    private final List<PagesChangedCallback> pagesChangedCallbacks = new ArrayList<>();
    
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
        mainClip.getLoopLength().addValueObserver(this::handleLoopLengthChanged);
        mainClip.scrollToStep(pagePosition.get() * gridLength);
    }
    
    public void addPagesChangedCallback(final PagesChangedCallback callback) {
        pagesChangedCallbacks.add(callback);
    }
    
    public void handleLoopLengthChanged(final double newLength) {
        loopLength = newLength;
        steps = (int) (loopLength / gridResolution);
        pages = Math.max(0, steps - 1) / gridLength + 1;
        updateStates();
        pagesChangedCallbacks.forEach(cb -> cb.notify(pagePosition.get(), pages));
    }
    
    public IntValue getPagePosition() {
        return pagePosition;
    }
    
    public double getLoopLength() {
        return loopLength;
    }
    
    public int getPages() {
        return pages;
    }
    
    public void setGridResolution(final double resolution) {
        final double quote = gridResolution / resolution;
        gridResolution = resolution;
        
        clips.forEach(cl -> cl.setStepSize(gridResolution));
        pagePosition.set((int) (pagePosition.get() * quote));
        steps = (int) (loopLength / gridResolution);
        pages = Math.max(0, steps - 1) / gridLength + 1;
        clips.forEach(cl -> cl.scrollToStep(pagePosition.get() * gridLength));
        updateStates();
        pagesChangedCallbacks.forEach(cb -> cb.notify(pagePosition.get(), pages));
    }
    
    private void updateStates() {
        if (pagePosition.get() < pages) {
            clips.forEach(cl -> cl.scrollToStep(pagePosition.get() * gridLength));
        }
        
        this.loopBound = (int) Math.round(loopLength / gridResolution);
        canScrollLeft.set(pagePosition.get() > 0);
        canScrollRight.set(pagePosition.get() < pages - 1);
    }
    
    public BooleanValueObject canScrollLeft() {
        return canScrollLeft;
    }
    
    public BooleanValueObject canScrollRight() {
        return canScrollRight;
    }
    
    public void setPage(final int index) {
        pagePosition.set(index);
        clips.forEach(cl -> cl.scrollToStep(pagePosition.get() * gridLength));
        updateStates();
    }
    
    public int getStepOffset() {
        return pagePosition.get() * gridLength;
    }
    
    public double getPosition() {
        return pagePosition.get() * gridResolution;
    }
    
    public double getGridResolution() {
        return gridResolution;
    }
    
    public void scrollLeft() {
        if (pagePosition.get() > 0) {
            pagePosition.inc(-1);
            clips.forEach(cl -> cl.scrollToStep(pagePosition.get() * gridLength));
            updateStates();
            pagesChangedCallbacks.forEach(cb -> cb.notify(pagePosition.get(), pages));
        }
    }
    
    public void scrollRight() {
        pagePosition.inc(1);
        clips.forEach(cl -> cl.scrollToStep(pagePosition.get() * gridLength));
        updateStates();
        pagesChangedCallbacks.forEach(cb -> cb.notify(pagePosition.get(), pages));
    }
    
    public void moveLeft() {
        clips.forEach(cl -> cl.scrollToStep((pagePosition.get() - 1) * gridLength));
    }
    
    public void moveRight() {
        clips.forEach(cl -> cl.scrollToStep((pagePosition.get() + 1) * gridLength));
    }
    
    public boolean stepIndexInLoop(final int index) {
        return (pagePosition.get() * gridLength + index) < loopBound;
    }
    
    public void expandClipToPagePosition(final int index) {
        final double pageLoop = pagePosition.get() * 16 * gridResolution;
        final double expansion = Math.ceil((index + 1.0) * gridResolution / 4.0) * 4.0;
        clips.get(0).getLoopLength().set(pageLoop + expansion);
    }
    
    public void expandClipIfNecessary() {
        if (pagePosition.get() + 1 >= pages) {
            clips.get(0).getLoopLength().set((pagePosition.get() + 2) * 16 * gridResolution);
        }
    }
}
