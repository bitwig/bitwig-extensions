package com.bitwig.extensions.controllers.allenheath.xonek3.layer;

public enum GridMode {
    CLIP(LayerId.CLIP_LAUNCHER), //
    SCENE(LayerId.SCENE_LAUNCHER),  //
    TRANSPORT(LayerId.GRID_TRANSPORT);
    private LayerId layerId;

    GridMode(final LayerId layerId) {
        this.layerId = layerId;
    }

    public LayerId getLayerId() {
        return layerId;
    }
}
