package com.bitwig.extensions.controllers.softube.console1;

record TrackId(int channelIndex, String trackId, String name) {
    //
    public String getDisableData() {
        return "{\"trackId\":\"" + trackId + "\"," + //
            "\"track\":" + channelIndex + "," + //
            "\"isActive\":false" + //
            "}";
    }
}
