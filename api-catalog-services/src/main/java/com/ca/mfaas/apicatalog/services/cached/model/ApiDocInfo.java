package com.ca.mfaas.apicatalog.services.cached.model;

import com.ca.mfaas.product.model.ApiInfo;
import com.ca.mfaas.product.routing.RoutedServices;
import com.netflix.appinfo.InstanceInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Data
@AllArgsConstructor
public class ApiDocInfo {
    ApiInfo apiInfo;
    ResponseEntity<String> apiDocResponse;
    RoutedServices routes;
    InstanceInfo gatewayInfo;
}
