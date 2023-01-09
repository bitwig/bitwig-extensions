package com.bitwig.extensions.controllers.novation.launchpad_pro_mk3;

import java.time.LocalTime;

public class LongPress {

    private LocalTime longPressTimeStamp;
    private LaunchpadProMK3Workflow driver;

    public LongPress(LaunchpadProMK3Workflow d) {
        driver = d;
    }


    public void delayedAction(Runnable action) {
        LocalTime tempTimeStamp = delayedActionInit();

        driver.mHost.scheduleTask(() -> {
            if (tempTimeStamp == longPressTimeStamp)
                action.run();
        }, 300);
    }

    public void delayedAction(Runnable action, Runnable elseAction) {
        LocalTime tempTimeStamp = delayedActionInit();

        driver.mHost.scheduleTask(() -> {
            if (tempTimeStamp == longPressTimeStamp)
                action.run();
            else 
                elseAction.run();
        }, 300);
    }

    private LocalTime delayedActionInit() {
        driver.isTrackBankNavigated = true;
        driver.mHost.scheduleTask(() -> driver.isTrackBankNavigated = false, (long) 100.0);

        longPressTimeStamp = java.time.LocalTime.now();
        LocalTime tempTimeStamp = longPressTimeStamp;
        return tempTimeStamp;
    }

    public void releasedAction() {
        longPressTimeStamp = java.time.LocalTime.now();
    }

    public void pressedAction(Runnable action) {
        longPressTimeStamp = java.time.LocalTime.now();
        LocalTime tempTimeStamp = longPressTimeStamp;
        action.run();
        longPressed(action, tempTimeStamp);
    }

    private void longPressed(Runnable action, LocalTime timeStamp) {
        longPressed(action, (long) 300.0, timeStamp);
    }

    private void longPressed(Runnable action, long timeout, LocalTime timeStamp) {
        driver.isTrackBankNavigated = true;
        driver.mHost.scheduleTask(() -> driver.isTrackBankNavigated = false, (long) 100.0);
        if (timeStamp == longPressTimeStamp) {
            driver.mHost.scheduleTask(() -> {
                if (timeStamp == longPressTimeStamp) {
                    action.run();
                    longPressed(action, (long) 100.0, timeStamp);
                }
            }, timeout);
        }
    }

}
