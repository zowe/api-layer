package com.broadcom.apiml.library.service.security.library.core;

import java.util.UUID;

public class ApiMessage {
    private final int code;
    private final String message;
    private final String type;
    private final UUID trackId;

    public ApiMessage(int code, String message, String type, UUID trackId) {
        this.code = code;
        this.message = message;
        this.type = type;
        this.trackId = trackId;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getType() {
        return type;
    }

    public UUID getTrackId() {
        return trackId;
    }
}
