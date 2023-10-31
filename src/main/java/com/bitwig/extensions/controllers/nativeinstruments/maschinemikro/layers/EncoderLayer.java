package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.CcAssignment;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.HwElements;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.ViewControl;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.values.BooleanValueObject;

import java.util.Optional;

public class EncoderLayer extends Layer {
   private final BooleanValueObject shiftHeld;
   private final Transport transport;
   private final CursorTrack cursorTrack;
   private EncoderMode encoderMode = EncoderMode.NONE;
   private final Groove groove;

   public EncoderLayer(Layers layers, HwElements hwElements, ViewControl viewControl, ModifierLayer modifierLayer,
                       Transport transport, ControllerHost host) {
      super(layers, "ENCODER_LAYER");
      this.shiftHeld = modifierLayer.getShiftHeld();
      this.transport = transport;
      this.groove = host.createGroove();
      this.groove.getEnabled().markInterested();
      cursorTrack = viewControl.getCursorTrack();
      hwElements.bindEncoder(this, hwElements.getMainEncoder(), dir -> handleEncoder(cursorTrack, dir));
      hwElements.getButton(CcAssignment.ENCODER_TOUCH).bindIsPressed(this, this::handleEncoderTouch);
   }

   private void handleEncoderTouch(boolean touched) {
      getCurrentParameter().ifPresent(parameter -> {
         parameter.touch(touched);
      });
   }

   private Optional<Parameter> getCurrentParameter() {
      if (encoderMode == EncoderMode.VOLUME) {
         return Optional.of(cursorTrack.volume());
      }
      return Optional.empty();
   }

   private void handleEncoder(CursorTrack cursorTrack, int diff) {
      switch (encoderMode) {
         case VOLUME -> {
            if (shiftHeld.get()) {
               cursorTrack.volume().value().inc(diff, 128);
            } else {
               cursorTrack.volume().value().inc(diff * 4, 128);
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
      getCurrentParameter().ifPresent(parameter -> parameter.touch(false));
      this.encoderMode = encoderMode;
      if (encoderMode == EncoderMode.NONE) {
         setIsActive(false);
      } else {
         setIsActive(true);
      }
   }
}
