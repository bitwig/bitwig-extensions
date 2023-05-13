package com.bitwig.extensions.controllers.mackie.devices;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.controllers.mackie.value.ModifierValueObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Fully Customized control of the Bitwig EQ+ Device Missing Parameters:
 * <p>
 * OUTPUT_GAIN GLOBAL_SHIFT BAND SOLO
 * <p>
 * ADAPTIVE_Q DECIBEL_RANGE
 */
public class EqDevice extends SpecificDevice {
   private final static String[] PNAMES = {"TYPE", "FREQ", "GAIN", "Q"};
   private final static RingDisplayType[] RING_TYPES = {RingDisplayType.FILL_LR, RingDisplayType.SINGLE, RingDisplayType.FILL_LR, RingDisplayType.FILL_LR};
   private final static double[] SENSITIVITIES = {1, 0.25, 0.25, 0.25};
   private static final String[] TYPES = {"*Off*", "LowC-1", "LowC-2", "LowC-4", "LowC-6", "LowC-8", "LoShlv", "Bell", //
      "HiC-1", "HiC-2", "HiC-4", "HiC-6", "HiC-8", "HiShlv", "Notch"};
   private static final String[] PAGE_NAMES = {"Bands 1 & 2", "Bands 3 & 4", "Bands 5 & 6", "Bands 7 & 8", "General"};

   private static final ParameterSetting[] PAGE_5 = { //
      new ParameterSetting("OUTPUT_GAIN", 0.125, RingDisplayType.FILL_LR), //
      new ParameterSetting("GLOBAL_SHIFT", 0.125, RingDisplayType.FILL_LR), //
      new ParameterSetting("BAND", 2, RingDisplayType.FILL_LR), //
      new ParameterSetting("SOLO", 2, RingDisplayType.FILL_LR), //
      new ParameterSetting("ADAPTIVE_Q", 2, RingDisplayType.FILL_LR), //
      new ParameterSetting("DECIBEL_RANGE", 1, RingDisplayType.FILL_LR), //
      new ParameterSetting("----", 1, RingDisplayType.FILL_LR), //
      new ParameterSetting("----", 1, RingDisplayType.SINGLE),};

   private final int[] enableValues = new int[8];
   private final List<Parameter> enableParams = new ArrayList<>();
   private final List<ParameterPage> parameterSlots = new ArrayList<>();

   public EqDevice(final CursorDeviceControl cursorDeviceControl, final DeviceTypeFollower deviceFollower) {
      super(SpecialDevices.EQ_PLUS, cursorDeviceControl, deviceFollower);

      for (int i = 0; i < 8; i++) {
         parameterSlots.add(new ParameterPage(i, this));
      }

      for (int i = 0; i < 8; i++) {
         final Parameter enableParam = createEnableParam(i);
         enableParams.add(enableParam);
         final int page = i / 2;
         final int index = i % 2 * 4;
         final Parameter typeParam = parameterSlots.get(index).getParameter(page);
         typeParam.value().addValueObserver(v -> notifyEnablementFromType(page, index, enableParam, v));
         enableParam.value().addValueObserver(2, v -> notifyEnablementFromEnable(page, index, typeParam, v));
      }
   }

   @Override
   public String getDeviceInfo() {
      return "EQ+ Device";
   }

   @Override
   public String getPageInfo() {
      return "Parameter Page : " + PAGE_NAMES[pageIndex];
   }

   private void notifyEnablementFromEnable(final int page, final int bandIndexOffset, final Parameter typeParam,
                                           final int value) {
      final int enabledValue = value * (typeParam.value().get() > 0 ? 1 : 0);
      enableValues[bandIndexOffset / 4 + page * 2] = enabledValue;
      if (page == pageIndex) {
         for (int i = 0; i < 4; i++) {
            final ParameterPage paramSlot = parameterSlots.get(i + bandIndexOffset);
            paramSlot.notifyEnablement(enabledValue);
         }
      }
   }

   @Override
   public void handleResetInvoked(final int index, final ModifierValueObject modifier) {
      if (modifier.isShiftSet()) {
         toggleEnable(index);
      } else if (modifier.isControlSet()) {
         resetAll();
      } else if (modifier.isOptionSet()) {
         cursorDeviceControl.getCursorDevice().isEnabled().toggle();
      } else {
         parameterSlots.get(index).doReset();
      }
   }

   private void resetAll() {
      for (final ParameterPage slot : parameterSlots) {
         slot.resetAll();
      }
      for (final Parameter param : enableParams) {
         param.reset();
      }
   }

   private void notifyEnablementFromType(final int page, final int bandIndexOffset, final Parameter enableParam,
                                         final double value) {
      final int enabledValue = (value > 0 ? 1 : 0) * (enableParam.value().get() > 0 ? 1 : 0);
      enableValues[bandIndexOffset / 4 + page * 2] = enabledValue;
      if (page == pageIndex && pageIndex < 4) {
         for (int i = 0; i < 4; i++) {
            final ParameterPage paramSlot = parameterSlots.get(i + bandIndexOffset);
            paramSlot.notifyEnablement(enabledValue);
         }
      }
   }

   private Parameter createEnableParam(final int index) {
      final String enablePname = "ENABLE" + (index + 1);
      final Parameter enableParam = bitwigDevice.createParameter(enablePname);
      return enableParam;
   }

   @Override
   public int getPages() {
      return 5;
   }

   private void updateSlots() {
      parameterSlots.forEach(p -> p.updatePage(pageIndex));
      if (pageIndex < 4) {
         for (int i = 0; i < 4; i++) {
            final ParameterPage paramSlot1 = parameterSlots.get(i);
            paramSlot1.notifyEnablement(enableValues[pageIndex * 2]);
            final ParameterPage paramSlot2 = parameterSlots.get(i + 4);
            paramSlot2.notifyEnablement(enableValues[pageIndex * 2 + 1]);
         }
      } else {
         parameterSlots.forEach(ps -> ps.notifyEnablement(1));
      }
      parameterSlots.forEach(ParameterPage::triggerUpdate);
   }

   @Override
   public String getParamName(final int page, final int index) {
      return PNAMES[index % 4] + Integer.toString(1 + index / 4 + page * 2);
   }

   @Override
   public DeviceParameter createDeviceParameter(final int page, final int index) {
      if (page < 4) {
         return createBandParameter(page, index);
      } else {
         final ParameterSetting pageSetting = PAGE_5[index];
         final Parameter param = bitwigDevice.createParameter(pageSetting.getPname());
         final DeviceParameter parameter = new DeviceParameter(pageSetting.getPname(), param, pageSetting.getRingType(),
            pageSetting.getSensitivity());

         return parameter;
      }
   }

   private DeviceParameter createBandParameter(final int page, final int index) {
      final String pname = getParamName(page, index);
      final Parameter param = bitwigDevice.createParameter(pname);
      final DeviceParameter parameter = new DeviceParameter(pname, param, RING_TYPES[index % 4],
         SENSITIVITIES[index % 4]);
      if (index == 0 || index == 4) {
         parameter.setCustomResetAction(p -> p.value().setImmediately(0.0));
         parameter.setCustomValueConverter(new CustomValueConverter() {

            @Override
            public String convert(final int value) {
               return TYPES[Math.min(14, value)];
            }

            @Override
            public int getIntRange() {
               return 15;
            }

         });
      }
      return parameter;
   }

   @Override
   public ParameterPage getParameterPage(final int index) {
      return parameterSlots.get(index);
   }

   @Override
   public void triggerUpdate() {
      parameterSlots.forEach(ParameterPage::triggerUpdate);
   }

   @Override
   public Parameter getParameter(final int index) {
      return parameterSlots.get(index).getParameter(pageIndex);
   }

   @Override
   public void navigateDeviceParameters(final int direction) {
      if (direction < 0) {
         final int nextIndex = pageIndex - 1;
         pageIndex = nextIndex < 0 ? getPages() - 1 : nextIndex < getPages() ? nextIndex : 0;
         updateSlots();
      } else {
         pageIndex = (pageIndex + 1) % getPages();
         updateSlots();
      }
   }

   public void toggleEnable(final Integer pindex) {
      if (pageIndex < 4) {
         final int bandIndex = pageIndex * 2 + pindex / 4;
         final Parameter enable = enableParams.get(bandIndex);
         final double v = enable.get();
         if (v == 0) {
            enable.value().setImmediately(1.0);
         } else {
            enable.value().setImmediately(0);
         }
      }
   }

   @Override
   public int getPageCount() {
      return 5;
   }
}
