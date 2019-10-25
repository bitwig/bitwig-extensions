package com.bitwig.extensions.framework.targets;

import com.bitwig.extension.controller.api.HardwareControl;

public interface Target
{
   default void assignToHardwareControl(final HardwareControl hardwareControl)
   {
   }
}
