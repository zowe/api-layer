package io.apiml.security.common.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public class ApiErrorResponse implements ApiMessage {
    private final Error error;

    public ApiErrorResponse(Error message) {
        this.error = message;
    }

    @JsonProperty("error")
    public Error getMessage() {
        return error;
    }
}
