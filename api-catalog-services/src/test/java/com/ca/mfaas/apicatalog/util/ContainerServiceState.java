package com.ca.mfaas.apicatalog.util;

import com.ca.mfaas.apicatalog.model.APIContainer;
import com.ca.mfaas.apicatalog.model.APIService;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import lombok.Data;

import java.util.List;

@Data
public class ContainerServiceState {
    private List<APIContainer> containers;
    private List<APIService> services;
    private List<InstanceInfo> instances;
    private List<Application> applications;
}
