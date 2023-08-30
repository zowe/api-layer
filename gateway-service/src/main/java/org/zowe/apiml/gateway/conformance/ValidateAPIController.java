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
import org.zowe.apiml.constants.EurekaMetadataDefinition;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.product.gateway.GatewayClient;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Controller offers method to check the validation of the given serviceID under conformance criteria
 */
@RestController
@RequiredArgsConstructor
public class ValidateAPIController {

    private static final int MAXIMUM_SERVICE_ID_LENGTH = 64;
    private static final String INVALID_SERVICE_ID_REGEX_PATTERN = "[^a-z0-9]";

    private static final String WRONG_SERVICE_ID_KEY = "org.zowe.apiml.gateway.verifier.wrongServiceId";
    private static final String NO_METADATA_KEY = "org.zowe.apiml.gateway.verifier.noMetadata";
    private static final String NON_CONFORMANT_KEY = "org.zowe.apiml.gateway.verifier.nonConformant";


    private static final String REGISTRATION_PROBLEMS = "Registration problems";
    private static final String METADATA_PROBLEMS = "Metadata problems";
    private static final String CONFORMANCE_PROBLEMS = "Conformance problems";


    private final MessageService messageService;
    private final VerificationOnboardService verificationOnboardService;
    private final DiscoveryClient discoveryClient;
    private final GatewayClient gatewayClient;


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
    public ResponseEntity<String> checkConformance(@PathVariable String serviceId, @CookieValue(value = "apimlAuthenticationToken", defaultValue = "dummy") String authenticationToken) {
        ConformanceProblemsContainer foundNonConformanceIssues = new ConformanceProblemsContainer(serviceId);
        foundNonConformanceIssues.add(CONFORMANCE_PROBLEMS, validateServiceIdFormat(serviceId));
        if (!foundNonConformanceIssues.isEmpty())
            return generateBadRequestResponseEntity(NON_CONFORMANT_KEY, foundNonConformanceIssues);


        try {
            checkServiceIsOnboarded(serviceId);

            List<ServiceInstance> serviceInstances = discoveryClient.getInstances(serviceId);
            checkInstanceCanBeRetrieved(serviceInstances);

            ServiceInstance serviceInstance = serviceInstances.get(0);
            Map<String, String> metadata = getMetadata(serviceInstance);

            checkMetadataCanBeRetrieved(metadata);

            Optional<String> swaggerUrl = verificationOnboardService.findSwaggerUrl(metadata);

            validateSwaggerDocument(serviceId, foundNonConformanceIssues, metadata, swaggerUrl, authenticationToken);
        } catch (ValidationException e) {
            switch (e.getKey()) {
                case WRONG_SERVICE_ID_KEY:
                    foundNonConformanceIssues.add(REGISTRATION_PROBLEMS, e.getMessage());
                    break;
                case NO_METADATA_KEY:
                    foundNonConformanceIssues.add(METADATA_PROBLEMS, e.getMessage());
                    break;
                default:
                    foundNonConformanceIssues.add(CONFORMANCE_PROBLEMS, e.getMessage());
            }
            return generateBadRequestResponseEntity(e.getKey(), foundNonConformanceIssues);
        }


        if (!foundNonConformanceIssues.isEmpty())
            return generateBadRequestResponseEntity(NON_CONFORMANT_KEY, foundNonConformanceIssues);

        return new ResponseEntity<>("{\"message\":\"Service " + serviceId + " fulfills all checked conformance criteria\"}", HttpStatus.OK);
    }


    private void validateSwaggerDocument(String serviceId, ConformanceProblemsContainer foundNonConformanceIssues, Map<String, String> metadata, Optional<String> swaggerUrl, String token) throws ValidationException {
        if (!swaggerUrl.isPresent()) {
            throw new ValidationException("Could not find Swagger Url", NON_CONFORMANT_KEY);
        }

        String swagger = verificationOnboardService.getSwagger(swaggerUrl.get());
        AbstractSwaggerValidator swaggerParser;
        swaggerParser = ValidatorFactory.parseSwagger(swagger, metadata, gatewayClient.getGatewayConfigProperties(), serviceId);

        if (!VerificationOnboardService.supportsSSO(metadata)) {
            foundNonConformanceIssues.add(CONFORMANCE_PROBLEMS, "Service doesn't claim to support SSO in its metadata, flag should be set to true for " + EurekaMetadataDefinition.AUTHENTICATION_SSO);
        }

        List<String> parserResponses = swaggerParser.getMessages();
        if (parserResponses != null) foundNonConformanceIssues.add(CONFORMANCE_PROBLEMS, parserResponses);

        Set<Endpoint> getMethodEndpoints = swaggerParser.getAllEndpoints();
        if (!getMethodEndpoints.isEmpty())
            foundNonConformanceIssues.add(CONFORMANCE_PROBLEMS, verificationOnboardService.testEndpointsByCalling(getMethodEndpoints, token));

        foundNonConformanceIssues.add(CONFORMANCE_PROBLEMS, VerificationOnboardService.getProblemsWithEndpointUrls(swaggerParser));
    }

    /**
     * Mapping so the old endpoint keeps working.
     *
     * @param serviceId serviceId to check for conformance
     * @return 200 if service is conformant, 400 + JSON explanation if not
     */
    @PostMapping(value = "/validate", produces = MediaType.APPLICATION_JSON_VALUE)
    @HystrixCommand
    public ResponseEntity<String> checkValidateLegacy(@RequestBody String serviceId, @CookieValue(value = "apimlAuthenticationToken", defaultValue = "dummy") String authenticationToken) {
        if (serviceId.startsWith("serviceID")) {
            serviceId = serviceId.replace("serviceID=", "");
        }
        return checkConformance(serviceId, authenticationToken);
    }


    /**
     * Creates a response when a conformance criteria is failed.
     *
     * @param foundNonConformanceIssues list of found issues
     * @return Response that this controller returns
     */
    private ResponseEntity<String> generateBadRequestResponseEntity(String key, ConformanceProblemsContainer foundNonConformanceIssues) {
        Message message = messageService.createMessage(key, "ThisWillBeRemoved");
        return new ResponseEntity<>(foundNonConformanceIssues.createBadRequestAPIResponseBody(key, message.mapToApiMessage()), HttpStatus.BAD_REQUEST);
    }


    /**
     * Accepts serviceId and checks if the service is onboarded to the API Mediation Layer
     * If it's not than it doesn't fulfill Item 1 of conformance criteria
     *
     * @param serviceId serviceId to check
     * @throws ValidationException describing the issue
     */
    public void checkServiceIsOnboarded(String serviceId) throws ValidationException {
        if (!verificationOnboardService.checkOnboarding(serviceId)) {
            throw new ValidationException("The service is not registered", WRONG_SERVICE_ID_KEY);
        }
    }


    /**
     * Retrieves metadata
     *
     * @param serviceInstance serviceInstance from which to retrieve the metadata.
     * @return Metadata of the instance
     */
    private Map<String, String> getMetadata(ServiceInstance serviceInstance) {
        return serviceInstance.getMetadata();
    }


    /**
     * Checks if metadata was retrieved.
     *
     * @param metadata which to test
     * @throws ValidationException describing the issue
     */
    public void checkMetadataCanBeRetrieved(Map<String, String> metadata) throws ValidationException {
        if (!(metadata != null && !metadata.isEmpty())) {
            throw new ValidationException("Cannot Retrieve MetaData", NO_METADATA_KEY);
        }
    }

    /**
     * Checks if a single instance can be retrieved.
     *
     * @param serviceInstances to check
     * @throws ValidationException describing the issue
     */
    public void checkInstanceCanBeRetrieved(List<ServiceInstance> serviceInstances) throws ValidationException {
        if (serviceInstances.isEmpty()) {
            throw new ValidationException("Cannot retrieve metadata - no active instance of the service", WRONG_SERVICE_ID_KEY);
        }
    }


    /**
     * Accept serviceId and checks if it is Zowe conformant according to the specification,
     * Item 5 from the conformance criteria list. That means that the serviceId contains only lower case
     * characters without symbols and is shorter than 64 characters
     *
     * @param serviceId to check
     * @return list of found issues, empty when conformant
     */
    public List<String> validateServiceIdFormat(String serviceId) {
        ArrayList<String> result = new ArrayList<>();
        if (serviceId.length() > MAXIMUM_SERVICE_ID_LENGTH) {
            result.add("The serviceId is longer than 64 characters");
        }
        // Check for invalid characters
        final Pattern symbolPattern = Pattern.compile(INVALID_SERVICE_ID_REGEX_PATTERN);
        Matcher findSymbol = symbolPattern.matcher(serviceId);
        if (findSymbol.find()) {
            result.add("The serviceId contains symbols or upper case letters");
        }

        return result;
    }


}
