package com.bitwig.extensions.controllers.mackie.seqencer;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extensions.controllers.mackie.value.BooleanValueObject;

import java.util.ArrayList;
import java.util.List;

public class StepViewPosition {

   public final int gridLength;
   private double gridResolution;
   private double loopLength = 0.0;
   private int pagePosition = 0;
   private int pages = 0;
   private int steps;

   private final Clip clip;
   private final BooleanValueObject canScrollLeft = new BooleanValueObject();
   private final BooleanValueObject canScrollRight = new BooleanValueObject();
   private final List<PagesChangedCallback> pagesChangedCallbacks = new ArrayList<>();

   public interface PagesChangedCallback {
      void notify(int pagePosition, int pages);
   }

   public StepViewPosition(final Clip clip, final int gridLength) {
      super();
      this.clip = clip;
      gridResolution = 0.25;
      this.gridLength = gridLength;
      this.clip.setStepSize(gridResolution);
      clip.getLoopLength().addValueObserver(this::handleLoopLengthChanged);
      clip.scrollToStep(pagePosition * gridLength);
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
      clip.setStepSize(gridResolution);
      pagePosition = (int) (pagePosition * quote);
      steps = (int) (loopLength / gridResolution);
      pages = Math.max(0, steps - 1) / gridLength + 1;
      clip.scrollToStep(pagePosition * gridLength);

      updateStates();
   }

   private void updateStates() {
      if (pagePosition < pages) {
         clip.scrollToStep(pagePosition * gridLength);
      }
      canScrollLeft.set(pagePosition > 0);
      canScrollRight.set(pagePosition < pages - 1);
      pagesChangedCallbacks.forEach(cb -> cb.notify(pagePosition, pages));
   }

   public BooleanValueObject canScrollLeft() {
      return canScrollLeft;
   }

   public BooleanValueObject canScrollRight() {
      return canScrollRight;
   }

   public void setPage(final int index) {
      pagePosition = index;
      clip.scrollToStep(pagePosition * gridLength);
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

   public void setLoopLength(final double loopLength) {
      this.loopLength = loopLength;
   }

   public double getLoopLength() {
      return loopLength;
   }

   public void scrollLeft() {
      if (pagePosition > 0) {
         pagePosition--;
         clip.scrollToStep(pagePosition * gridLength);
         updateStates();
      }
   }

   public void scrollRight() {
      if (pagePosition < pages - 1) {
         pagePosition++;
         clip.scrollToStep(pagePosition * gridLength);
         updateStates();
      }
   }

}
