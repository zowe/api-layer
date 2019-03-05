package com.broadcom.apiml.service.gateway.dto;

public class UserTokenQueryResponseDto {
    private static UserTokenQueryResponseDto ourInstance = new UserTokenQueryResponseDto();

    private UserTokenQueryResponseDto() {
    }

    public static UserTokenQueryResponseDto getInstance() {
        return ourInstance;
    }
}
