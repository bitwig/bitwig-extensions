package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers;

import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.HwElements;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.ViewControl;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.buttons.RgbButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;

import java.util.List;

@Component
public class TrackLayer extends Layer {

   public TrackLayer(Layers layers, HwElements hwElements, ViewControl viewControl, ModifierLayer modifierLayer) {
      super(layers, "GROUP_LAYER");
      TrackBank trackBank = viewControl.getMixerTrackBank();
      List<RgbButton> gridButtons = hwElements.getPadButtons();
      for (int i = 0; i < trackBank.getSizeOfBank(); i++) {
         Track track = trackBank.getItemAt(i);
      }
   }
}
