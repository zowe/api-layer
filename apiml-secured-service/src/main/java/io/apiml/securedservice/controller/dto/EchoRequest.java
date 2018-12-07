package io.apiml.securedservice.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class EchoRequest {
    private final String message;
}
