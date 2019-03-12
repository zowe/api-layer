package com.ca.mfaas.apicatalog.services.initialisation;

import com.ca.mfaas.product.registry.ApplicationWrapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.netflix.discovery.shared.Application;
import lombok.Data;

import java.util.List;

@JsonDeserialize(as = ApplicationWrapper2.class)
@Data
public class ApplicationWrapper2 {

    private List<com.netflix.discovery.shared.Application> applications;

    public ApplicationWrapper2() {
    }

    public ApplicationWrapper2(List<Application> applications) {
        this.applications = applications;
    }
}
