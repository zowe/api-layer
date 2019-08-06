package com.ca.mfaas.product.gateway;

import org.springframework.context.ApplicationEvent;

public class GatewayLookupCompleteEvent extends ApplicationEvent {

    /**
     * Create a new ApplicationEvent.
     *
     * @param source the object on which the event initially occurred (never {@code null})
     */
    public GatewayLookupCompleteEvent(Object source) {
        super(source);
    }
}
