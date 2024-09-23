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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.zowe.apiml.constants.EurekaMetadataDefinition;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.security.common.token.TokenAuthentication;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Controller offers method to check the validation of the given serviceID under conformance criteria
 */
@RestController
@Tag(name = "Services")
@RequiredArgsConstructor
@RequestMapping({"/gateway", "/gateway/api/v1"})
public class ValidateAPIController {

    private static final int MAXIMUM_SERVICE_ID_LENGTH = 64;
    private static final String INVALID_SERVICE_ID_REGEX_PATTERN = "[^a-z0-9]";

    private static final String REGISTRATION_PROBLEMS = "Registration problems";
    private static final String METADATA_PROBLEMS = "Metadata problems";
    private static final String CONFORMANCE_PROBLEMS = "Conformance problems";

    static final String WRONG_SERVICE_ID_KEY = "org.zowe.apiml.gateway.verifier.wrongServiceId";
    static final String NO_METADATA_KEY = "org.zowe.apiml.gateway.verifier.noMetadata";
    static final String NON_CONFORMANT_KEY = "org.zowe.apiml.gateway.verifier.nonConformant";

    public static final String LEGACY_CONFORMANCE_SHORT_URL = "gateway/validate";
    public static final String LEGACY_CONFORMANCE_LONG_URL = "gateway/api/v1/validate";
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
        value = "/conformance/{serviceId}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Checks service conformance.",
        operationId = "checkConformanceUsingGET",
        description = "Accepts serviceID and checks conformance criteria.",
        security = {
            @SecurityRequirement(name = "CookieAuth"),
            @SecurityRequirement(name = "Bearer")
        }
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service conforms to the criteria",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(type = "object", maxProperties = 1),
                schemaProperties = {
                    @SchemaProperty(name = "message",
                        schema = @Schema(type = "string", example = "Service {serviceId} fulfills all checked conformance criteria"))
                }
        )),
        @ApiResponse(responseCode = "400", description = "Service does not conform to the criteria",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(type = "object", maxProperties = 2),
                schemaProperties = {
                    @SchemaProperty(name = "error", schema = @Schema(type = "string")),
                    @SchemaProperty(name = "details", array = @ArraySchema(schema = @Schema(name = "items", type = "string")))
                }
        ))
    })
    public ResponseEntity<String> checkConformance(@Parameter(in = ParameterIn.PATH, required = true, description = "Service ID of the service to check") @PathVariable String serviceId,
                                                   Authentication authentication) {
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

            validateSwaggerDocument(serviceId, foundNonConformanceIssues, metadata, swaggerUrl, getToken(authentication));
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

    private String getToken(Authentication authentication) {
        if (authentication instanceof TokenAuthentication tokenAuthentication) {
            return tokenAuthentication.getCredentials();
        }
        return null;
    }

    private void validateSwaggerDocument(String serviceId, ConformanceProblemsContainer foundNonConformanceIssues, Map<String, String> metadata, Optional<String> swaggerUrl, String token) throws ValidationException {
        if (swaggerUrl.isEmpty()) {
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

        Set<Endpoint> allEndpoints = swaggerParser.getAllEndpoints();
        if (!allEndpoints.isEmpty())
            foundNonConformanceIssues.add(CONFORMANCE_PROBLEMS, verificationOnboardService.testEndpointsByCalling(allEndpoints, token));

        foundNonConformanceIssues.add(CONFORMANCE_PROBLEMS, VerificationOnboardService.getProblemsWithEndpointUrls(swaggerParser));
    }

    /**
     * Mapping so the old endpoint keeps working.
     *
     * @param serviceId serviceId to check for conformance
     * @return 200 if service is conformant, 400 + JSON explanation if not
     */

    @PostMapping(value = {LEGACY_CONFORMANCE_SHORT_URL, LEGACY_CONFORMANCE_LONG_URL}, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Legacy endpoint for checking service conformance.",
        operationId = "checkValidateLegacyUsingPOST",
        description = "Mapping so the old endpoint keeps working.",
        security = {
            @SecurityRequirement(name = "CookieAuth"),
            @SecurityRequirement(name = "Bearer")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(example = "serviceID=serviceId")
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service conforms to the criteria",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(type = "object", maxProperties = 1),
                schemaProperties = {
                    @SchemaProperty(name = "message",
                        schema = @Schema(type = "string", example = "Service {serviceId} fulfills all checked conformance criteria"))
                }
            )),
        @ApiResponse(responseCode = "400", description = "Service does not conform to the criteria",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(type = "object", maxProperties = 2),
                schemaProperties = {
                    @SchemaProperty(name = "error", schema = @Schema(type = "string")),
                    @SchemaProperty(name = "details", array = @ArraySchema(schema = @Schema(name = "items", type = "string")))
                }
            ))
    })
    public ResponseEntity<String> checkValidateLegacy(@RequestBody String serviceId, Authentication authentication) {
        if (serviceId.startsWith("serviceID")) {
            serviceId = serviceId.replace("serviceID=", "");
        }
        return checkConformance(serviceId, authentication);
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
