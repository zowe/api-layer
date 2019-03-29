package com.ca.mfaas.product.routing;

public enum ServiceType {
    API(1, "API"),
    UI(2, "UI"),
    WS(3, "WS");

    private int serviceCode;
    private String name;
    private ServiceType(int serviceCode, String name) {
        this.serviceCode = serviceCode;
        this.name = name;
    }
}
