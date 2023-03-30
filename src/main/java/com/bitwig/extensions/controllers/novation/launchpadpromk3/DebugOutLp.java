package com.bitwig.extensions.controllers.novation.launchpadpromk3;

import com.bitwig.extension.controller.api.ControllerHost;

public class DebugOutLp {

    public static ControllerHost host;

    public static void println(final String format, final Object... args) {
        if (host != null) {
            host.println(String.format(format, args));
        }
    }

    public static void main(final String[] args) {
        // 1/4    1.0
        // 1/4 T  0.666
        // 1/8    0.5
        // 1/8T   0.33400
        // 1/16   0.25
        // 1/16T  0.166000
        // 1/32   0.125
        // 1/32T  0.084000
        final double sz = 0.25;
        for (int i = 1; i <= 32; i++) {
            System.out.println(i + " " + (i * sz));
        }
    }
}
