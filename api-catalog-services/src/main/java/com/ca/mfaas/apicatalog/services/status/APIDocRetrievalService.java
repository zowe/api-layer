package com.ca.mfaas.apicatalog.services.status;

import com.ca.mfaas.apicatalog.services.initialisation.InstanceRetrievalService;
import com.netflix.appinfo.InstanceInfo;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class APIDocRetrievalService {
    private final RestTemplate restTemplate;
    private final InstanceRetrievalService instanceRetrievalService;

    @Autowired
    public APIDocRetrievalService(RestTemplate restTemplate, InstanceRetrievalService instanceRetrievalService) {
        this.restTemplate = restTemplate;
        this.instanceRetrievalService = instanceRetrievalService;
    }

    /**
     * Retrieve the API docs for a registered service (all versions)
     *
     * @param serviceId  the unique service id
     * @param apiVersion the version of the API
     * @return the api docs as a string
     */
    public ResponseEntity<String> retrieveApiDoc(@NonNull String serviceId, String apiVersion) {
        InstanceInfo instanceInfo = instanceRetrievalService.getInstanceInfo(serviceId);

        String scheme;
        int port = instanceInfo.getSecurePort();
        if (port != 0) {
            scheme = "https";
        } else {
            scheme = "http";
            port = instanceInfo.getPort();
        }

        UriComponents uri = UriComponentsBuilder
            .newInstance()
            .scheme(scheme)
            .host(instanceInfo.getHostName())
            .port(port)
            .path(instanceInfo.getMetadata().get("routed-services.api-doc.service-url"))
            .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON_UTF8));

        ResponseEntity<String> response = restTemplate.exchange(
            uri.toUri(),
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class);

        return response;
    }
}
