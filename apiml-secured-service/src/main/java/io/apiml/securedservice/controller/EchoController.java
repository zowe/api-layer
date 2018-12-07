package io.apiml.securedservice.controller;

import io.apiml.securedservice.controller.dto.EchoRequest;
import io.apiml.securedservice.controller.dto.Message;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/echo")
public class EchoController {

    @PostMapping(
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public Message createEchoMessage(@RequestBody EchoRequest echoRequest) {
        return new Message(echoRequest.getMessage());
    }

    @GetMapping(
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    public Message echoMessage() {
        return new Message("Hello");
    }
}
