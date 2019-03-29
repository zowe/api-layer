package com.ca.mfaas.product.routing;

import lombok.Getter;

@Getter
public enum ServiceType {
    ALL(1, "All services"),
    API(2, "API"),
    UI(3, "UI"),
    WS(4, "WS");

    private int serviceCode;
    private String name;
    ServiceType(int serviceCode, String name) {
        this.serviceCode = serviceCode;
        this.name = name;
    }
}
