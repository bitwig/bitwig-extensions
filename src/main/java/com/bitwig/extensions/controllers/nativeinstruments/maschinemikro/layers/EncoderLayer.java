package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Groove;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.HwElements;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.ViewControl;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.BooleanValueObject;

@Component(priority = 0)
public class EncoderLayer extends Layer {
   private final BooleanValueObject shiftHeld;
   private final Transport transport;
   private EncoderMode encoderMode = EncoderMode.NONE;
   private final Groove groove;

   public EncoderLayer(Layers layers, HwElements hwElements, ViewControl viewControl, ModifierLayer modifierLayer,
                       Transport transport, ControllerHost host) {
      super(layers, "ENCODER_LAYER");
      this.shiftHeld = modifierLayer.getShiftHeld();
      this.transport = transport;
      this.groove = host.createGroove();
      this.groove.getEnabled().markInterested();
      CursorTrack cursorTrack = viewControl.getCursorTrack();
      hwElements.bindEncoder(this, hwElements.getMainEncoder(), dir -> handleEncoder(cursorTrack, dir));
   }

   private void handleEncoder(CursorTrack masterTrack, int diff) {
      switch (encoderMode) {
         case VOLUME -> {
            if (shiftHeld.get()) {
               masterTrack.volume().value().inc(diff, 128);
            } else {
               masterTrack.volume().value().inc(diff * 4, 128);
            }
         }
         case TEMPO -> {
            transport.tempo().value().incRaw(shiftHeld.get() ? diff * 0.1 : diff);
         }
         case SWING -> {
            if (shiftHeld.get()) {
               groove.getAccentAmount().value().inc(diff, 100);
            } else {
               groove.getShuffleAmount().value().inc(diff, 100);
            }
         }
         default -> {
            // do nothing
         }
      }
   }

   public void setMode(EncoderMode encoderMode) {
      this.encoderMode = encoderMode;
      if (encoderMode == EncoderMode.NONE) {
         setIsActive(false);
      } else {
         setIsActive(true);
      }
   }
}
