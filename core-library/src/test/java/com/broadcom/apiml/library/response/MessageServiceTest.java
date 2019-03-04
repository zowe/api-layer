package com.broadcom.apiml.library.response;

import org.junit.Test;

import static org.junit.Assert.*;

public class MessageServiceTest {

    @Test
    public void createMessage() {
        MessageService messageService = new MessageService();

        ApiMessage message = messageService.createMessage(200, "hello");

        assertEquals(message.getMessage(), "hello");
    }
}
