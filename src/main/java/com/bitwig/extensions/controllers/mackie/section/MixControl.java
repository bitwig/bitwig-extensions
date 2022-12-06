package com.bitwig.extensions.controllers.mackie.section;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.mackie.*;
import com.bitwig.extensions.controllers.mackie.configurations.*;
import com.bitwig.extensions.controllers.mackie.configurations.BrowserConfiguration.Type;
import com.bitwig.extensions.controllers.mackie.devices.CursorDeviceControl;
import com.bitwig.extensions.controllers.mackie.devices.DeviceManager;
import com.bitwig.extensions.controllers.mackie.devices.DeviceTypeBank;
import com.bitwig.extensions.controllers.mackie.devices.SpecialDevices;
import com.bitwig.extensions.controllers.mackie.display.DisplayLayer;
import com.bitwig.extensions.controllers.mackie.display.LcdDisplay;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.controllers.mackie.display.VuMode;
import com.bitwig.extensions.controllers.mackie.layer.*;
import com.bitwig.extensions.controllers.mackie.seqencer.NoteSequenceLayer;
import com.bitwig.extensions.controllers.mackie.value.BooleanValueObject;
import com.bitwig.extensions.controllers.mackie.value.ModifierValueObject;
import com.bitwig.extensions.controllers.mackie.value.TrackColor;
import com.bitwig.extensions.framework.Layer;

public class MixControl implements LayerStateHandler {
   private final MixerSectionHardware hwControls;
   final MackieMcuProExtension driver;

   final MixerLayerGroup mainGroup;
   final MixerLayerGroup globalGroup;
   final DrumMixerLayerGroup drumGroup;

   protected final LayerState layerState;
   private final MultiStateHardwareLight backgroundColoring;
   private NoteSequenceLayer noteSequenceLayer;

   protected LayerConfiguration currentConfiguration;

   protected final LayerConfiguration panConfiguration = new MixerLayerConfiguration("PAN", this, ParamElement.PAN);
   private final LayerConfiguration sendConfiguration = new MixerLayerConfiguration("SEND", this,
      ParamElement.SENDMIXER);
   private final TrackLayerConfiguration sendTrackConfiguration;
   private final TrackLayerConfiguration sendDrumTrackConfiguration;
   private final TrackLayerConfiguration cursorDeviceConfiguration;
   private final TrackLayerConfiguration eqTrackConfiguration;
   private final GlovalViewLayerConfiguration globalViewLayerConfiguration;

   private final BooleanValueObject fadersTouched = new BooleanValueObject();
   private int touchCount = 0;
   private final boolean hasBottomDisplay;

   private final SectionType type;
   private final ClipLaunchButtonLayer launchButtonLayer;

   private final BooleanValueObject isMenuHoldActive = new BooleanValueObject();
   private final DisplayLayer infoLayer;
   private DeviceTypeBank deviceTypeBank;
   private DisplayLayer activeDisplayLayer;

   protected VPotMode activeVPotMode = VPotMode.PAN;
   private DrumNoteHandler drumNoteHandler;
   private final NotePlayingButtonLayer scaleButtonLayer;

   public MixControl(final MackieMcuProExtension driver, final MidiIn midiIn, final MidiOut midiOut,
                     final int sectionIndex, final SectionType type, final boolean hasTrackColoring) {
      this.driver = driver;
      this.type = type;

      backgroundColoring = driver.getSurface()
         .createMultiStateHardwareLight(String.format("BACKGROUND_COLOR_" + "%d_%s", sectionIndex, type));

      if (hasTrackColoring) {
         backgroundColoring.state().onUpdateHardware(state -> {
            if (state instanceof TrackColor) {
               ((TrackColor) state).send(midiOut);
            }
         });
      }

      hasBottomDisplay = driver.getControllerConfig().hasLowerDisplay();
      hwControls = new MixerSectionHardware(driver, midiIn, midiOut, sectionIndex, type);
      for (int i = 0; i < 8; i++) {
         hwControls.assignFaderTouchAction(i, this::handleTouch);
      }
      infoLayer = new DisplayLayer("HINT_DISP_LAYER", sectionIndex, getDriver().getLayers(), hwControls);

      final TrackSelectionHandler trackSelectionHandler = driver.createTrackSelectionHandler();
      mainGroup = new MixerLayerGroup("MN", this, trackSelectionHandler);
      globalGroup = new MixerLayerGroup("GL", this, trackSelectionHandler);
      drumGroup = new DrumMixerLayerGroup("DR", this, trackSelectionHandler);

      sendConfiguration.setNavigateHorizontalHandler(direction -> {
         if (driver.getMixerMode().get() == MixerMode.DRUM) {
            drumGroup.navigateHorizontally(direction);
         } else {
            mainGroup.navigateHorizontally(direction);
            globalGroup.navigateHorizontally(direction);
         }
      });

      if (type == SectionType.MAIN) {
         noteSequenceLayer = new NoteSequenceLayer("NOTE_SEQUENCER", this);
         mainGroup.setSequenceLayer(noteSequenceLayer);
         globalGroup.setSequenceLayer(noteSequenceLayer);
      }

      sendTrackConfiguration = new TrackLayerConfiguration("SN_TR", this);
      sendDrumTrackConfiguration = new TrackLayerConfiguration("SEND_DRUM_TRACK", this);
      cursorDeviceConfiguration = new TrackLayerConfiguration("INSTRUMENT", this);
      eqTrackConfiguration = new TrackLayerConfiguration("EQ_DEVICE", this);
      globalViewLayerConfiguration = new GlovalViewLayerConfiguration("GLOBAL_VIEW", this);
      launchButtonLayer = new ClipLaunchButtonLayer("CLIP_LAUNCH", this, driver.createSlotHandler());
      scaleButtonLayer = new NotePlayingButtonLayer(this);

      currentConfiguration = panConfiguration;
      panConfiguration.setActive(true);

      layerState = new LayerState(this);
      activeDisplayLayer = infoLayer;

      driver.getFlipped().addValueObserver(flipped -> layerState.updateState(this));
      driver.getMixerMode().addValueObserver(this::changeMixerMode);
      driver.getButtonView().addValueObserver(this::handleButtonViewChanged);
      driver.getTrackChannelMode().addValueObserver(trackMode -> doModeChange(driver.getVpotMode().getMode(), true));

      if (getHwControls().hasBottomDisplay()) {
         fadersTouched.addValueObserver(this::handleFadersTouchedBottom);
      } else {
         fadersTouched.addValueObserver(this::handleFadersTouched);
      }
      if (type == SectionType.MAIN) {
         setUpModifierHandling(driver.getModifier());
      }
   }

   public void init() {
      layerState.init();
   }

   public MultiStateHardwareLight getBackgroundColoring() {
      return backgroundColoring;
   }

   private void handleButtonViewChanged(final ButtonViewState oldState, final ButtonViewState newState) {
      if (newState == ButtonViewState.GLOBAL_VIEW) {
         switchActiveConfiguration(globalViewLayerConfiguration);
         layerState.updateState(this);
      } else {
         doModeChange(activeVPotMode, true);
      }
   }

   private void setUpModifierHandling(final ModifierValueObject modifier) {
      modifier.addValueObserver(modvalue -> {
         // TODO this will have to change to accommodate different modes
         if (currentConfiguration.applyModifier(modvalue)) {
            layerState.updateState(this);
         }
//         else if (modifier.get() > 0 && launchButtonLayer.isActive()) {
//            infoLayer.setMainText("Clip mods:  Shft+Opt=delete  Shft+Alt=double content",
//               "Opt=duplicate alt=stop track", false);
//            infoLayer.enableFullTextMode(true);
//            infoLayer.setIsActive(true);
//            layerState.updateState(this);
//         } else if (infoLayer.isActive()) {
//            infoLayer.setIsActive(false);
//            layerState.updateState(this);
//      }
      });
   }

   private void handleFadersTouchedBottom(final boolean touched) {
      // TODO this will have to be figured out
   }

   private void handleFadersTouched(final boolean touched) {
      if (touched) {
         layerState.updateDisplayState(getActiveDisplayLayer());
      } else {
         driver.scheduleAction("TOUCH", 1500, () -> layerState.updateDisplayState(getActiveDisplayLayer()));
      }
   }

   public MixerLayerGroup getActiveMixGroup() {
      final MixerMode group = driver.getMixerMode().get();
      switch (group) {
         case GLOBAL:
            return globalGroup;
         case DRUM:
            return drumGroup;
         case MAIN:
         default:
            return mainGroup;
      }
   }

   @Override
   public LayerConfiguration getCurrentConfig() {
      return currentConfiguration;
   }

   @Override
   public Layer getButtonLayer() {
      switch (driver.getButtonView().get()) {
         case MIXER:
         case STEP_SEQUENCER:
         case GLOBAL_VIEW:
            return currentConfiguration.getButtonLayer();
         case GROUP_LAUNCH:
            return launchButtonLayer;
         case NOTE_PLAY:
            return scaleButtonLayer;
         default:
            break;
      }
      return currentConfiguration.getButtonLayer();
   }

   @Override
   public DisplayLayer getActiveDisplayLayer() {
      if (infoLayer.isActive()) {
         activeDisplayLayer = infoLayer;
      } else {
         final boolean flipped = getActiveMixGroup().isFlipped();
         final boolean touched = fadersTouched.get();
         final int whichLayer = !flipped && touched || flipped && !touched ? 1 : 0;

         activeDisplayLayer = currentConfiguration.getDisplayLayer(whichLayer);
      }
      return activeDisplayLayer;
   }

   @Override
   public DisplayLayer getBottomDisplayLayer() {
      return currentConfiguration.getBottomDisplayLayer(getActiveMixGroup().isFlipped() ? 1 : 0);
   }

   @Override
   public boolean hasBottomDisplay() {
      return hasBottomDisplay;
   }

   public BooleanValueObject getIsMenuHoldActive() {
      return isMenuHoldActive;
   }

   public MixerSectionHardware getHwControls() {
      return hwControls;
   }

   public ModifierValueObject getModifier() {
      return driver.getModifier();
   }

   public void resetFaders() {
      hwControls.resetFaders();
   }

   public LcdDisplay getDisplay() {
      return hwControls.getMainDisplay();
   }

   public void clearAll() {
      hwControls.getMainDisplay().clearAll();
   }

   public void exitMessage() {
      hwControls.getMainDisplay().exitMessage();
   }

   public void applyVuMode(final VuMode mode) {
      hwControls.getMainDisplay().setVuMode(mode);
   }

   public MackieMcuProExtension getDriver() {
      return driver;
   }

   public void navigateLeftRight(final int direction, final boolean isPressed) {
      // TODO this is too complicated we need redesign this
      if (launchButtonLayer.isActive()) {
         launchButtonLayer.navigateHorizontal(direction, isPressed);
      } else if (scaleButtonLayer.isActive()) {
         scaleButtonLayer.navigateHorizontal(direction, isPressed);
      } else if (getActiveMixGroup().hasCursorNavigation()) {
         if (isPressed) {
            getActiveMixGroup().navigateHorizontally(direction);
         }
      } else {
         if (isPressed) {
            currentConfiguration.navigateHorizontal(direction);
            if (currentConfiguration.enableInfo(InfoSource.NAV_HORIZONTAL)) {
               layerState.updateState(this);
            }
         } else {
            if (currentConfiguration.disableInfo()) {
               layerState.updateState(this);
            }
         }
      }
   }

   public void navigateUpDown(final int direction, final boolean isPressed) {
      if (launchButtonLayer.isActive()) {
         launchButtonLayer.navigateVertical(direction, isPressed);
      } else if (scaleButtonLayer.isActive()) {
         driver.getNotePlayingSetup().modifyOctave(direction); // Problematic with note playing in multiple
      } else if (getActiveMixGroup().hasCursorNavigation()) {
         if (isPressed) {
            getActiveMixGroup().navigateVertically(direction);
         }
      } else {
         if (isPressed) {
            currentConfiguration.navigateVertical(direction);
            if (currentConfiguration.enableInfo(InfoSource.NAV_VERTICAL)) {
               layerState.updateState(this);
            }
         } else {
            if (currentConfiguration.disableInfo()) {
               layerState.updateState(this);
            }
         }
      }
   }

   public void notifyModeAdvance(final VPotMode mode, final boolean pressed) {
      if (!pressed) {
         currentConfiguration.disableInfo();
         isMenuHoldActive.set(false);
      } else {
         isMenuHoldActive.set(true);

         final DeviceManager deviceTracker = currentConfiguration.getDeviceManager();

         switch (activeVPotMode) {
            case EQ:
               if (deviceTracker != null && !deviceTracker.isSpecificDevicePresent()) {
                  final InsertionPoint ip = driver.getCursorTrack().endOfDeviceChainInsertionPoint();
                  ip.insertBitwigDevice(SpecialDevices.EQ_PLUS.getUuid());
               }
               break;
            case PLUGIN:
            case INSTRUMENT:
            case MIDI_EFFECT:
               if (deviceTracker != null && !deviceTracker.isSpecificDevicePresent()) {
                  deviceTracker.initiateBrowsing(driver.getBrowserConfiguration(), Type.DEVICE);
               }
               break;
            default:
         }
      }
      layerState.updateState(this);
   }

   public void notifyModeChange(final VPotMode mode, final boolean down) {
      if (down) {
         doModeChange(mode, true);
      } else {
         currentConfiguration.disableInfo();
         layerState.updateState(this);
      }
   }

   private void changeMixerMode(final MixerMode oldMode, final MixerMode newMode) {
      DebugUtil.println(" CHANGE MODE %s", newMode);
      determineSendTrackConfig(activeVPotMode);
      if (oldMode == MixerMode.DRUM) {
         driver.getCursorTrack().selectChannel(driver.getCursorTrack()); // re-assert selection to main channel
         driver.getCursorDeviceControl().focusOnPrimary();
         if (activeVPotMode.isDeviceMode()) {
            driver.getVpotMode().setMode(VPotMode.PAN);
         } else {
            layerState.updateState(this);
         }
      } else {
         layerState.updateState(this, true);
      }
      switch (newMode) {
         case MAIN:
            drumGroup.setActive(false);
            globalGroup.setActive(false);
            mainGroup.setActive(true);
            break;
         case GLOBAL:
            drumGroup.setActive(false);
            mainGroup.setActive(false);
            globalGroup.setActive(true);
            break;
         case DRUM:
            globalGroup.setActive(false);
            mainGroup.setActive(false);
            drumGroup.setActive(true);
            break;

      }
   }

   void doModeChange(final VPotMode mode, final boolean focus) {
      switch (mode) {
         case EQ:
            switchActiveConfiguration(eqTrackConfiguration);
            currentConfiguration.setCurrentFollower(deviceTypeBank.getFollower(mode));
            break;
         case PAN:
            switchActiveConfiguration(panConfiguration);
            break;
         case MIDI_EFFECT:
         case INSTRUMENT:
         case PLUGIN:
            switchActiveConfiguration(cursorDeviceConfiguration);
            currentConfiguration.setCurrentFollower(deviceTypeBank.getFollower(mode));
            break;
         case SEND:
            determineSendTrackConfig(VPotMode.SEND);
            break;
         default:
            break;
      }
      activeVPotMode = mode;
      if (currentConfiguration.getDeviceManager() != null && focus) {
         focusDevice(currentConfiguration.getDeviceManager());
      } else {
         ensureDevicePointer(currentConfiguration.getDeviceManager());
      }
      getDriver().getBrowserConfiguration().endUserBrowsing();
      layerState.updateState(this);
   }

   protected void determineSendTrackConfig(final VPotMode mode) {
      if (mode != VPotMode.SEND) {
         return;
      }
      if (type != SectionType.MAIN) {
         switchActiveConfiguration(sendConfiguration);
      } else if (driver.getTrackChannelMode().get()) {
         if (driver.getMixerMode().get() == MixerMode.DRUM) {
            switchActiveConfiguration(sendDrumTrackConfiguration);
         } else {
            switchActiveConfiguration(sendTrackConfiguration);
         }
      } else {
         switchActiveConfiguration(sendConfiguration);
      }
   }

   public void setConfiguration(final LayerConfiguration config) {
      switchActiveConfiguration(config);
      layerState.updateState(this);
   }

   protected void switchActiveConfiguration(final LayerConfiguration nextConfiguration) {
      if (nextConfiguration == currentConfiguration) {
         return;
      }
      currentConfiguration.setActive(false);
      currentConfiguration = nextConfiguration;
      currentConfiguration.setActive(true);
   }

   private void ensureDevicePointer(final DeviceManager deviceManager) {
      if (deviceManager == null) {
         return;
      }
      deviceManager.getCurrentFollower().ensurePosition();
   }

   private void focusDevice(final DeviceManager deviceManager) {
      if (driver.getMixerMode().get() == MixerMode.DRUM) {
         getDriver().getCursorDeviceControl().focusOnDrumDevice();
      } else if (deviceManager.getCurrentFollower() != null) {
         final Device device = deviceManager.getCurrentFollower().getFocusDevice();
         getDriver().getCursorDeviceControl().selectDevice(device);
      }
   }

   public void applyUpdate() {
      layerState.updateState(this);
   }

   public void notifyBlink(final int ticks) {
      launchButtonLayer.notifyBlink(ticks);
      scaleButtonLayer.notifyBlink(ticks);
      drumGroup.notifyBlink(ticks);
      if (noteSequenceLayer != null) {
         noteSequenceLayer.notifyBlink(ticks);
      }
      globalViewLayerConfiguration.notifyBlink(ticks);
      activeDisplayLayer.triggerTimer();
   }

   private void handleTouch(final boolean touched) {
      if (touched) {
         touchCount++;
      } else if (touchCount > 0) {
         touchCount--;
      }
      if (touchCount > 0 && !fadersTouched.get()) {
         fadersTouched.set(true);
      } else if (touchCount == 0 && fadersTouched.get()) {
         fadersTouched.set(false);
      }
   }

   public void handleNameDisplay(final boolean pressed) {
      final DisplayLayer displayLayer = getActiveDisplayLayer();
      displayLayer.setNameValueState(pressed);
      displayLayer.invokeRefresh();
//      layerState.updateDisplayState(displayLayer);
//
//      final MixerLayerGroup activeMixerGroup = getActiveMixGroup();
//      if (activeMixerGroup.notifyDisplayName(pressed)) {
//         layerState.updateState(this);
//      } else if (currentConfiguration.notifyDisplayName(pressed)) {
//         layerState.updateState(this);
//      }
   }

   public void handleInfoDisplay(final boolean active) {
      final DisplayLayer displayLayer = getActiveDisplayLayer();
      final Layer buttonLayer = getButtonLayer();

      HelperInfo.getInfo(buttonLayer.getName(), displayLayer.getName(),
         driver.getControllerConfig().getManufacturerType()).ifPresent(info -> {
         if (active) {
            displayLayer.showInfo(info.getTopInfo(), info.getBottomInfo());
         } else {
            displayLayer.invokeRefresh();
         }
      });
      //getDriver().getHost().println(" " + displayLayer.getName() + " " + buttonLayer.getName());
   }

   public LayerConfiguration getCurrentConfiguration() {
      return currentConfiguration;
   }

   public void fullHardwareUpdate() {
      hwControls.fullHardwareUpdate();
   }

   public void resetLeds() {
      hwControls.resetLeds();
   }

   public void initMainControl(final TrackBank mixerTrackBank, final TrackBank globalTrackBank,
                               final DrumPadBank drumPadBank) {
      final NoteInput noteInput = getHwControls().getMidiIn().createNoteInput("MIDI", "80????", "90????");
      noteInput.setShouldConsumeEvents(true);
      final CursorTrack cursorTrack = getDriver().getCursorTrack();
      drumNoteHandler = new DrumNoteHandler(noteInput, drumPadBank, cursorTrack);
      final ScaleNoteHandler scaleNoteHandler = new ScaleNoteHandler(noteInput, getDriver().getNotePlayingSetup(),
         cursorTrack);
      mainGroup.init(mixerTrackBank);
      mainGroup.setActive(true);
      globalGroup.init(globalTrackBank);
      if (type == SectionType.MAIN) {
         mainGroup.initMainSlider(driver.getMasterTrack(), driver.getMasterSlider());
         globalGroup.initMainSlider(driver.getMasterTrack(), driver.getMasterSlider());
      }
      drumGroup.init(drumPadBank, drumNoteHandler);
      if (type == SectionType.MAIN) {
         drumGroup.initMainSlider(cursorTrack, driver.getMasterSlider());
         noteSequenceLayer.init(getDriver().getNotePlayingSetup(), noteInput);
      }
      scaleButtonLayer.init(scaleNoteHandler, getHwControls());
      launchButtonLayer.initTrackBank(getHwControls(), mixerTrackBank);

      globalViewLayerConfiguration.init(mixerTrackBank, globalTrackBank);
   }

   public DrumNoteHandler getDrumNoteHandler() {
      return drumNoteHandler;
   }

   public void initTrackControl(final CursorTrack cursorTrack, final CursorDeviceLayer drumCursor,
                                final DeviceTypeBank deviceTypeBank) {
      this.deviceTypeBank = deviceTypeBank;

      final SendBank sendBank = cursorTrack.sendBank();
      final SendBank drumSendBank = drumCursor.sendBank();

      final CursorDeviceControl cursorDeviceControl = getDriver().getCursorDeviceControl();

      final DeviceManager cursorDeviceManager = deviceTypeBank.getDeviceManager(VPotMode.INSTRUMENT);
      final DeviceManager eqDevice = deviceTypeBank.getDeviceManager(VPotMode.EQ);

      for (int i = 0; i < 8; i++) {
         sendTrackConfiguration.addBinding(i, sendBank.getItemAt(i), RingDisplayType.FILL_LR);
         sendDrumTrackConfiguration.addBinding(i, drumSendBank.getItemAt(i), RingDisplayType.FILL_LR);
         cursorDeviceConfiguration.addBinding(i, cursorDeviceManager.getParameter(i), RingDisplayType.FILL_LR);
         eqTrackConfiguration.addBinding(i, eqDevice.getParameterPage(i),
            (pindex, pslot) -> eqDevice.handleResetInvoked(pindex, driver.getModifier()));
      }
      sendTrackConfiguration.addBindingFader(8, cursorTrack, driver.getMasterSlider());
      sendDrumTrackConfiguration.addBindingFader(8, cursorTrack, driver.getMasterSlider());
      cursorDeviceConfiguration.addBindingFader(8, cursorTrack, driver.getMasterSlider());
      eqTrackConfiguration.addBindingFader(8, cursorTrack, driver.getMasterSlider());

      final DeviceMenuConfiguration menuConfig = new DeviceMenuConfiguration("DEVICE_MENU", this);
      menuConfig.initMenuControl(cursorDeviceControl);

      cursorDeviceConfiguration.setDeviceManager(cursorDeviceManager, menuConfig);
      cursorDeviceConfiguration.registerFollowers(deviceTypeBank.getStandardFollowers());
      eqTrackConfiguration.setDeviceManager(eqDevice, menuConfig);
      eqTrackConfiguration.registerFollowers(deviceTypeBank.getFollower(VPotMode.EQ));

      sendTrackConfiguration.setNavigateHorizontalHandler(direction -> handleSendNavigation(sendBank, direction));
      sendDrumTrackConfiguration.setNavigateHorizontalHandler(
         direction -> handleSendNavigation(drumSendBank, direction));

      cursorDeviceConfiguration.setNavigateHorizontalHandler(cursorDeviceManager::navigateDeviceParameters);
      cursorDeviceConfiguration.setNavigateVerticalHandler(
         direction -> cursorDeviceControl.navigateDevice(direction, driver.getModifier()));

      eqTrackConfiguration.setNavigateHorizontalHandler(eqDevice::navigateDeviceParameters);
      eqTrackConfiguration.setNavigateVerticalHandler(
         direction -> cursorDeviceControl.navigateDevice(direction, driver.getModifier()));

      cursorTrack.name().addValueObserver(trackName -> ensureModeFocus());
      setUpDeviceModeHandling(deviceTypeBank);
   }

   private void setUpDeviceModeHandling(final DeviceTypeBank deviceTypeBank) {
      final BrowserConfiguration browserConfiguration = getDriver().getBrowserConfiguration();

      final PinnableCursorDevice cursorDevice = driver.getCursorDeviceControl().getCursorDevice();
      cursorDevice.position().addValueObserver(p -> updateDeviceMode(p, browserConfiguration, cursorDevice));
      cursorDevice.isNested()
         .addValueObserver(
            nested -> updateDeviceMode(cursorDevice.position().get(), browserConfiguration, cursorDevice));

      deviceTypeBank.addListener((type, exists) -> {
         if (driver.getVpotMode().getMode() == type) {
            focusDevice(currentConfiguration.getDeviceManager());
         }
      });
   }

   private void updateDeviceMode(final int p, final BrowserConfiguration browserConfiguration,
                                 final PinnableCursorDevice cursorDevice) {
      final VPotMode fittingMode = VPotMode.fittingMode(cursorDevice);
      if (p >= 0 && fittingMode != null && activeVPotMode.isDeviceMode()) {
         if (!browserConfiguration.isActive()) {
            driver.getVpotMode().setMode(fittingMode);
            doModeChange(fittingMode, false);
         }
      }
   }

   private void handleSendNavigation(final SendBank sendBank, final int direction) {
      if (direction < 0) {
         sendBank.scrollBackwards();
      } else {
         sendBank.scrollForwards();
      }
   }

   private void ensureModeFocus() {

   }

   public void handleSoloAction(final Channel channel) {
      if (!channel.exists().get()) {
         return;
      }
      channel.solo().toggle();
   }

   public void handlePadSelection(final DrumPad pad) {
      pad.selectInEditor();
      pad.selectInMixer();
      if (activeVPotMode == VPotMode.INSTRUMENT) {
         driver.getCursorDeviceControl().focusOnDrumDevice();
      }
   }

   public void advanceMode(final ButtonViewState mode) {
      if (mode == ButtonViewState.STEP_SEQUENCER) {
         drumGroup.advanceMode();
      }
   }
}
