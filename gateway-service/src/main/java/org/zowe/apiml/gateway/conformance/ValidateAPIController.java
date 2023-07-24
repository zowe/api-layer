/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.conformance;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Controller offers method to check the validation of the given serviceID under conformance criteria
 */
@RestController
@RequiredArgsConstructor
public class ValidateAPIController {

    private static final int MaximumServiceIdLength = 64;
    private static final String InvalidServiceIdRegex = "[^a-z0-9]";


    private static final String wrongServiceIdKey = "org.zowe.apiml.gateway.verifier.wrongServiceId";
    private static final String NoMetadataKey = "org.zowe.apiml.gateway.verifier.NoMetadata";
    private static final String NonConformantKey = "org.zowe.apiml.gateway.verifier.NonConformant";


    private final MessageService messageService;

    private final VerificationOnboardService verificationOnboardService;

    private final DiscoveryClient discoveryClient;


    /**
     * Accepts serviceID and checks conformance criteria
     *
     * @param serviceId accepted serviceID to check for conformance
     * @return 200 if service is conformant, 400 + JSON explanation if not
     */
    @GetMapping(
        value = "/gateway/conformance/{serviceId}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @HystrixCommand
    public ResponseEntity<String> checkConformance(@PathVariable String serviceId) {


        ConformanceProblemsContainer foundNonConformanceIssues = new ConformanceProblemsContainer();


        List<ServiceInstance> serviceInstances = discoveryClient.getInstances(serviceId);


        foundNonConformanceIssues.put("Registration problems", validatorItem1(serviceId));
        foundNonConformanceIssues.put("Conformance problems", validatorItem5(serviceId));

        if (foundNonConformanceIssues.size() != 0) {     // cant continue if a service isn't registered
            return GenerateBadRequestResponseEntity(wrongServiceIdKey, foundNonConformanceIssues);
        }


        foundNonConformanceIssues.put("Registration problems", instanceCheck(serviceInstances));

        Map<String, String> metadata = getMetadata(serviceInstances);

        foundNonConformanceIssues.put("Metadata problems", metaDataCheck(metadata));

        if (foundNonConformanceIssues.size() != 0) {     // cant continue without metadata
            return GenerateBadRequestResponseEntity(NoMetadataKey, foundNonConformanceIssues);
        }

        // Other non conformance checks here

        if (foundNonConformanceIssues.size() != 0) {
            return GenerateBadRequestResponseEntity(NonConformantKey, foundNonConformanceIssues);
        }

//        return new ResponseEntity<>(HttpStatus.OK);

        return new ResponseEntity<>("{\"message\":\"Service fulfills all checked conformance criteria\"}", HttpStatus.OK);
    }


    /**
     * Mapping so the old endpoint keeps working.
     *
     * @param serviceId serviceId to check for conformance
     * @return 200 if service is conformant, 400 + JSON explanation if not
     */
    @PostMapping(value = "/validate", produces = MediaType.APPLICATION_JSON_VALUE)
    @HystrixCommand
    public ResponseEntity<String> checkValidateLegacy(@RequestBody String serviceId) {
        return checkConformance(serviceId);
    }


    /**
     * Creates a response when a conformance criteria is failed.
     *
     * @param foundNonConformanceIssues list of found issues
     * @return Response that this controller returns
     */
    private ResponseEntity<String> GenerateBadRequestResponseEntity(String key, ConformanceProblemsContainer foundNonConformanceIssues) {
        Message message = messageService.createMessage(key, "ThisWillBeRemoved");
        return new ResponseEntity<>(foundNonConformanceIssues.createBadRequestAPIResponseBody(key, message.mapToApiMessage()), HttpStatus.BAD_REQUEST);
    }


    /**
     * Checks if the service is onboarded.
     * If it's not than it doesn't fulfill Item 1 of conformance criteria
     *
     * @param serviceId serviceId to check
     * @return list of found issues, empty when conformant
     */
    private ArrayList<String> validatorItem1(String serviceId) {
        ArrayList<String> result = new ArrayList<>();
        if (!verificationOnboardService.checkOnboarding(serviceId)) {
            result.add("The service is not registered");
        }
        return result;
    }


    /**
     * Checks if metadata can be retrieved.
     *
     * @return Either empty list or list containing one item with the explanation of the problem
     */
    private Map<String, String> getMetadata(List<ServiceInstance> serviceInstances) {
        ServiceInstance serviceInstance = serviceInstances.get(0);
        return serviceInstance.getMetadata();
    }


    /**
     * Checks if metadata can be retrieved.
     *
     * @return Either empty list or list containing one item with the explanation of the problem
     */
    private ArrayList<String> metaDataCheck(Map<String, String> metadata) {
        ArrayList<String> result = new ArrayList<>();
        if (metadata != null && !metadata.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Collections.singleton("Cannot Retrieve MetaData"));
    }

    private ArrayList<String> instanceCheck(List<ServiceInstance> serviceInstances) {
        if (serviceInstances.isEmpty()) {
            return new ArrayList<>(Collections.singleton("Cannot retrieve metadata - no active instance of the service"));
        }
        return new ArrayList<>();
    }


    /**
     * Accept serviceId and checks if it is Zowe conformant according to the specification,
     * specifically Item 5 from the list. That means that the serviceId contains only lower case characters
     * without symbols and is shorter than 64 characters
     *
     * @param serviceId accept serviceID to check
     * @return list of found issues, empty when conformant
     */
    private ArrayList<String> validatorItem5(String serviceId) {
        ArrayList<String> result = new ArrayList<>();

        if (serviceId.length() > MaximumServiceIdLength) {
            result.add("The serviceId is longer than 64 characters");
        }


        // Check for invalid characters
        final Pattern symbolPattern = Pattern.compile(InvalidServiceIdRegex);
        Matcher findSymbol = symbolPattern.matcher(serviceId);
        if (findSymbol.find()) {
            result.add("The serviceId contains symbols or upper case letters");
        }

        return result;
    }


}
