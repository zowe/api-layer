package com.broadcom.apiml.library.response;

import java.util.UUID;

public class MessageService {
    public ApiMessage createMessage(int code, String message) {
        return new ApiMessage(code, message, "client.error", UUID.randomUUID());
    }
}
