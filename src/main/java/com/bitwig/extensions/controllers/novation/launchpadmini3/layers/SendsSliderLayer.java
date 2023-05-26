package com.bitwig.extensions.controllers.novation.launchpadmini3.layers;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.Send;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.novation.commonsmk3.MidiProcessor;
import com.bitwig.extensions.controllers.novation.commonsmk3.PanelLayout;
import com.bitwig.extensions.controllers.novation.commonsmk3.SliderBinding;
import com.bitwig.extensions.controllers.novation.launchpadmini3.LaunchPadPreferences;
import com.bitwig.extensions.controllers.novation.launchpadmini3.ViewCursorControl;
import com.bitwig.extensions.framework.Layers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SendsSliderLayer extends TrackSliderLayer {

   private TrackBank trackBank;
   private int sendItems = 0;
   private final List<Consumer<ControlMode>> controlModeRemovedListeners = new ArrayList<>();

   public SendsSliderLayer(final ViewCursorControl viewCursorControl, final HardwareSurface controlSurface,
                           final Layers layers, final MidiProcessor midiProcessor,
                           final LaunchPadPreferences preferences) {
      super("SENDS", controlSurface, layers, midiProcessor, 30, 13);
      bind(viewCursorControl.getTrackBank());
      preferences.getPanelLayout().addValueObserver(((oldValue, newValue) -> {
         layout = newValue;
         updateFaderState();
      }));
   }

   @Override
   public boolean canBeEntered(final ControlMode mode) {
      if (mode == ControlMode.SENDS_A && sendItems > 0) {
         return true;
      }
      return mode == ControlMode.SENDS_B && sendItems > 1;
   }

   public void addControlModeRemoveListener(final Consumer<ControlMode> listeners) {
      controlModeRemovedListeners.add(listeners);
   }

   @Override
   protected void bind(final TrackBank trackBank) {
      this.trackBank = trackBank;
      for (int i = 0; i < 8; i++) {
         final Track track = trackBank.getItemAt(i);
         if (i == 0) {
            track.sendBank().itemCount().addValueObserver(this::updateSendItemsAvailable);
         }
         final Send sends = track.sendBank().getItemAt(0);
         final SliderBinding binding = new SliderBinding(baseCcNr, sends, sliders[i], i, midiProcessor);
         addBinding(binding);
         valueBindings.add(binding);
      }
   }

   private void updateSendItemsAvailable(final int items) {
      if (items == 1 && sendItems > 1) {
         controlModeRemovedListeners.forEach(l -> l.accept(ControlMode.SENDS_B));
      } else if (items == 0 && sendItems > 0) {
         controlModeRemovedListeners.forEach(l -> l.accept(ControlMode.SENDS_A));
      }
      sendItems = items;
   }

   public void setControl(final ControlMode mode) {
      for (int i = 0; i < 8; i++) {
         final Track track = trackBank.getItemAt(i);
         track.sendBank().scrollPosition().set(mode == ControlMode.SENDS_A ? 0 : 1);
      }
   }


   @Override
   protected void updateFaderState() {
      if (isActive()) {
         refreshTrackColors();
         midiProcessor.setFaderBank(layout == PanelLayout.VERTICAL ? 0 : 1, tracksExistsColors, true, baseCcNr);
         valueBindings.forEach(SliderBinding::update);
      }
   }


   @Override
   protected void refreshTrackColors() {
      final boolean[] exists = trackState.getExists();
      for (int i = 0; i < 8; i++) {
         tracksExistsColors[i] = exists[i] ? baseColor : 0;
      }
   }

   @Override
   protected void onDeactivate() {
      super.onDeactivate();
      updateFaderState();
   }

   @Override
   protected void onActivate() {
      super.onActivate();
      refreshTrackColors();
      updateFaderState();
   }
}


