package com.bitwig.extensions.controllers.nativeinstruments.commons;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extensions.framework.values.BooleanValueObject;

public class StepViewPosition {
   private final double[] rateTable = {0.0625, 0.125, 0.25, 0.5, 1.0, 2.0, 4.0};

   private final String[] rateDisplayValues = {"1/64", "1/32", "1/16", "1/8", "1/4", "1/2", "1/1"};

   private int resolutionIndex = 2;
   private double gridResolution;

   private double loopLength = 0.0;

   private int pagePosition = 0;

   private int pages = 0;

   private final BooleanValueObject canScrollLeft = new BooleanValueObject();
   private final BooleanValueObject canScrollRight = new BooleanValueObject();

   private final Clip clip;

   public StepViewPosition(final Clip clip) {
      super();
      this.clip = clip;
      gridResolution = rateTable[resolutionIndex];
      this.clip.setStepSize(gridResolution);
      clip.getLoopLength().addValueObserver(this::handleLoopLenghtChanged);
      clip.scrollToStep(pagePosition * 16);
   }

   public void handleLoopLenghtChanged(final double newLength) {
      this.loopLength = newLength;

      final int newPageSize = (int) Math.ceil(this.loopLength * gridResolution);
      this.pages = newPageSize;
      updateStates();
   }

   public void modifyGrid(final int amount) {
      final int newIndex = Math.min(Math.max(resolutionIndex + amount, 0), rateTable.length - 1);
      if (newIndex != resolutionIndex) {
         resolutionIndex = newIndex;
         gridResolution = rateTable[resolutionIndex];
         this.clip.setStepSize(gridResolution);

         final int newPageSize = (int) Math.ceil(this.loopLength / gridResolution / 16);
         this.pages = newPageSize;

         if (amount < 0) {
            this.pagePosition *= 2;
         } else {
            this.pagePosition /= 2;
         }

         updateStates();
      }
   }

   public String getGridValue() {
      return rateDisplayValues[resolutionIndex];
   }

   private void updateStates() {
      if (pagePosition >= pages) {
         pagePosition = pages > 0 ? pages - 1 : 0;
         clip.scrollToStep(pagePosition * 16);
      }
      canScrollLeft.set(pagePosition > 0);
      canScrollRight.set(pagePosition < pages - 1);
   }

   public int getStepOffset() {
      return pagePosition * 16;
   }

   public double getPosition() {
      return pagePosition * gridResolution;
   }

   public double getGridResolution() {
      return gridResolution;
   }

   public BooleanValue canScrollLeft() {
      return canScrollLeft;
   }

   public BooleanValue canScrollRight() {
      return canScrollRight;
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
         clip.scrollToStep(pagePosition * 16);
         updateStates();
      }
   }

   public void scrollRight() {
      if (pagePosition < pages) {
         pagePosition++;
         clip.scrollToStep(pagePosition * 16);
         updateStates();
      }
   }
}
