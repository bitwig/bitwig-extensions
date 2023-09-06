package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers;

import com.bitwig.extensions.controllers.nativeinstruments.commons.ColorBrightness;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.HwElements;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.RgbColor;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.buttons.RgbButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;

import java.util.List;

@Component
public class GridLayer extends Layer {

   @Inject
   private StepEditor stepEditor;

   public GridLayer(Layers layers, HwElements hwElements) {
      super(layers, "GRID_LAYER");

      List<RgbButton> gridButtons = hwElements.getPadButtons();
      for (int i = 0; i < 8; i++) {
         final int index = i;
         RgbButton button = gridButtons.get(i);
         button.bindLight(this, () -> RgbColor.OFF);
         button.bindPressed(this, () -> {

         });
      }
      for (int i = 8; i < 16; i++) {
         int row = 1 - (i - 8) / 4;
         final int index = row * 4 + (i - 8) % 4;
         RgbButton button = gridButtons.get(i);
         button.bindLight(this, () -> resolutionColor(index));
         button.bindPressed(this, () -> selectGridResolution(index));
      }
   }

   private void selectGridResolution(int index) {
      stepEditor.setResolutionIndex(index);
   }

   private RgbColor resolutionColor(int index) {
      RgbColor color = index % 2 == 0 ? RgbColor.PINK : RgbColor.WHITE;
      if (stepEditor.getResolutionIndex() == index) {
         return color.brightness(ColorBrightness.BRIGHT);
      }
      return color.brightness(ColorBrightness.DIMMED);
   }
}
