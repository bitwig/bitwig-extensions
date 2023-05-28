package com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.display;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.CCAssignment;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.KeylabIcon;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.RgbButton;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.SliderEncoderControl;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.color.RgbLightState;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.components.HwElements;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.components.SysExHandler;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.components.ViewControl;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;
import com.bitwig.extensions.framework.di.PostConstruct;
import com.bitwig.extensions.framework.values.BasicStringValue;
import com.bitwig.extensions.framework.values.ValueObject;

@Component
public class MainScreenSection {
   private static final int BUTTON_WAIT_UTIL_REPEAT = 500;
   private static final int HOLD_REPEAT_FREQUENCY = 200;

   @Inject
   private LcdDisplay display;
   @Inject
   private ViewControl viewControl;
   @Inject
   private SliderEncoderControl sliderControl;

   private final RgbButton context1;
   private final RgbButton context2;
   private final RgbButton context3;
   private final RgbButton context4;

   private final Layer navigationLayer;
   private ContextPageConfiguration contextPage;
   private final BasicStringValue focusElementContent = new BasicStringValue("");
   private SysExHandler.PadMode padMode = SysExHandler.PadMode.PAD_CLIPS;
   private BasicStringValue padLocationInfo = new BasicStringValue("");

   public MainScreenSection(final HwElements hwElements, final Layers layers) {
      context1 = hwElements.getButton(CCAssignment.CONTEXT1);
      context2 = hwElements.getButton(CCAssignment.CONTEXT2);
      context3 = hwElements.getButton(CCAssignment.CONTEXT3);
      context4 = hwElements.getButton(CCAssignment.CONTEXT4);
      navigationLayer = new Layer(layers, "SCREEN NAVIGATION");
   }

   ///KeylabIcon selector = KeylabIcon.NONE;

   @PostConstruct
   void init() {
      initNavigationScreen();
      final ValueObject<SliderEncoderControl.State> sliderMode = sliderControl.getCurrentState();
      sliderMode.addValueObserver((oldValue, newValue) -> {
         contextPage.setHeaderText(newValue == SliderEncoderControl.State.MIXER ? "Mixer" : "Device");
         contextPage.getContextParts()[1].setFrame(
            newValue == SliderEncoderControl.State.MIXER ? ContextPart.FrameType.FRAME_SMALL : ContextPart.FrameType.BAR);
         updateFooter();
         updateHeader();
      });
   }

   private void initNavigationScreen() {
      final CursorTrack cursorTrack = viewControl.getCursorTrack();

      contextPage = new ContextPageConfiguration("Navigation",//
         new ContextPart("", KeylabIcon.SEARCH, ContextPart.FrameType.BAR), //
         new ContextPart("", KeylabIcon.MIXER, ContextPart.FrameType.FRAME_SMALL), //
         new ContextPart("", KeylabIcon.BRACKET_LEFT, ContextPart.FrameType.NONE), //
         new ContextPart("", KeylabIcon.BRACKET_RIGHT, ContextPart.FrameType.NONE));
      contextPage.setHeaderText("Mixer");
      contextPage.setHeaderIcon(KeylabIcon.NONE);

      context2.bindPressed(navigationLayer, () -> sliderControl.toggleMode());
      context2.bindLight(navigationLayer, () -> sliderControl.getCurrentState()
         .get() == SliderEncoderControl.State.MIXER ? RgbLightState.WHITE : RgbLightState.WHITE_DIMMED);

      context3.bindRepeatHold(navigationLayer, cursorTrack::selectPrevious, BUTTON_WAIT_UTIL_REPEAT,
         HOLD_REPEAT_FREQUENCY);

      context3.bindLight(navigationLayer, () -> canScroll(cursorTrack.hasPrevious()));

      context4.bindRepeatHold(navigationLayer, cursorTrack::selectNext, BUTTON_WAIT_UTIL_REPEAT, HOLD_REPEAT_FREQUENCY);
      context4.bindLight(navigationLayer, () -> canScroll(cursorTrack.hasNext()));

      viewControl.getSceneTrackItem().name().addValueObserver(name -> {
         if (padMode == SysExHandler.PadMode.PAD_CLIPS) {
            focusElementContent.set(name);
         }
      });

      navigationLayer.addBinding(
         new MainViewDisplayBinding(contextPage, display, cursorTrack.name(), focusElementContent));
   }

   public void setPadInfo(BasicStringValue padLocationInfo) {
      this.padLocationInfo = padLocationInfo;
      padLocationInfo.addValueObserver(info -> {
         if (padMode == SysExHandler.PadMode.PAD_DRUMS) {
            focusElementContent.set(info);
         }
      });
   }

   public void notifyPadMode(SysExHandler.PadMode padMode) {
      this.padMode = padMode;
      if (padMode == SysExHandler.PadMode.PAD_CLIPS) {
         focusElementContent.set(viewControl.getSceneTrackItem().name().get());
      } else {
         focusElementContent.set(padLocationInfo.get());
      }
   }

   public RgbButton getContextButton(int which) {
      switch (which) {
         case 0:
            return context1;
         case 1:
            return context2;
         case 2:
            return context3;
         case 3:
            return context4;
      }
      return context1;
   }

   public Layer getLayer() {
      return navigationLayer;
   }

   public ContextPageConfiguration getContextPage() {
      return contextPage;
   }

   private RgbLightState canScroll(final BooleanValue canScrollValue) {
      return canScrollValue.get() ? RgbLightState.WHITE : RgbLightState.WHITE_DIMMED;
   }

   public void updateFooter() {
      display.updateFooter(contextPage);
   }

   private void updateHeader() {
      display.updateHeader(contextPage);
   }

   public void updatePage() {
      display.sendNavigationPage(contextPage, false);
   }

   @Activate
   public void activate() {
      navigationLayer.setIsActive(true);
      display.sendNavigationPage(contextPage, false);
   }

}
