package com.bitwig.extensions.controllers.novation.launchpadpromk3.sliderlayers;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.novation.commonsmk3.MidiProcessor;
import com.bitwig.extensions.controllers.novation.commonsmk3.PanelLayout;
import com.bitwig.extensions.controllers.novation.commonsmk3.SliderBinding;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.SysExHandler;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.layers.ControlMode;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.layers.TrackState;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Inject;

public abstract class TrackSliderLayer extends SliderLayer {

   protected TrackState trackState;
   protected PanelLayout panelLayout = PanelLayout.VERTICAL;

   public TrackSliderLayer(final String name, final ControlMode mode, final HardwareSurface controlSurface,
                           final Layers layers, final MidiProcessor midiProcessor, final SysExHandler sysExHandler) {
      super(name, mode, controlSurface, layers, midiProcessor, sysExHandler);
   }

   @Inject
   public void setTrackState(final TrackState trackState) {
      this.trackState = trackState;
      trackState.addExistsListener((trackIndex, exists) -> updateFaderState());
      trackState.addColorStateListener((trackIndex, color) -> updateFaderState());
   }

   protected abstract void bind(TrackBank trackBank);

   protected void updateFaderState() {
      if (isActive()) {
         refreshTrackColors();
         modeHandler.setFaderBank(mode == ControlMode.PAN || panelLayout == PanelLayout.HORIZONTAL ? 1 : 0, mode,
            tracksExistsColors);
         valueBindings.forEach(SliderBinding::update);
      }
   }

   @Override
   protected void refreshTrackColors() {
      final boolean[] exists = trackState.getExists();

      for (int i = 0; i < 8; i++) {
         if (exists[i]) {
            tracksExistsColors[i] = mode.getColor();
         } else {
            tracksExistsColors[i] = 0;
         }
      }
   }

}
