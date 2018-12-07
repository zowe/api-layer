package io.apiml.security.common.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class Error implements Message {
    private final int status;
    private final String code;
    private final String message;
    private final String traceId;
}
