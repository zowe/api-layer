package com.ca.mfaas.enable.services;

import com.netflix.appinfo.InstanceInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.cloud.client.ServiceInstance;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
public class DiscoveredServiceInstance {
    List<InstanceInfo> instanceInfos = new ArrayList<>();
    List<ServiceInstance> serviceInstances = new ArrayList<>();

    public boolean hasInstances() {
        return !instanceInfos.isEmpty() || !serviceInstances.isEmpty();
    }
}
