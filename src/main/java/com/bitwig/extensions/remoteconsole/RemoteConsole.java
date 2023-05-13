package com.bitwig.extensions.remoteconsole;

import com.bitwig.extension.controller.api.ControllerHost;

public interface RemoteConsole {
   RemoteConsole out = new RemoteConsoleHost();

   static void init(final ControllerHost host) {
      ((RemoteConsoleHost) out).init(host);
   }

   void printSysEx(String prefix, byte[] data);

   void println(String format, Object... params);

   String getStackTrace(final int max);
}
