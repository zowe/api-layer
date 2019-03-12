package com.ca.mfaas.apicatalog.util;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.netflix.discovery.shared.Applications;
import lombok.Data;

@JsonDeserialize(as = ApplicationsWrapper.class)
@Data
public class ApplicationsWrapper {

    private Applications applications;

    public ApplicationsWrapper() {
    }

    public ApplicationsWrapper(Applications applications) {
        this.applications = applications;
    }
}
