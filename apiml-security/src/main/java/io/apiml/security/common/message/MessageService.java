package io.apiml.security.common.message;

import org.springframework.stereotype.Service;

@Service
public class MessageService {
    public ApiMessage createError(int status, String code, String message, String traceId) {
        return ApiErrorResponse.builder()
            .error(Error.builder()
                    .status(status)
                    .code(code)
                    .message(message)
                    .traceId(traceId)
                    .build()
            ).build();
    }
}
