package com.bitwig.extensions.controllers.novation.launchkey_mk4.control;

import com.bitwig.extensions.controllers.novation.launchkey_mk4.ModeType;

public interface ModeListener {
    void handleModeChange(ModeType type, int id);
}
