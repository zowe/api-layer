package org.zowe.apiml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.appinfo.EurekaAccept;
import com.netflix.discovery.converters.jackson.EurekaJsonJacksonCodec;
import com.netflix.discovery.shared.Applications;
import com.netflix.eureka.resources.ApplicationResource;
import com.netflix.eureka.resources.ApplicationsResource;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/eureka/apps")
@DependsOn("modulithConfig")
@Slf4j
public class MyApplicationsResource extends ApplicationsResource {
    private static final String HEADER_ACCEPT = "Accept";
    private static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";


    @GetMapping("/{appId}")
    public Mono<ApplicationResource> getAppResource(
        @PathVariable("appId") String appId) {
        return Mono.just(super.getApplicationResource(null, appId));
    }

    @GetMapping
    public Mono<Object> getAllContainer(
        @Nullable @RequestHeader(HEADER_ACCEPT) String acceptHeader,
        @Nullable @RequestHeader(HEADER_ACCEPT_ENCODING) String acceptEncoding,
        @Nullable @RequestHeader(EurekaAccept.HTTP_X_EUREKA_ACCEPT) String eurekaAccept,
        @Nullable @RequestParam(value = "regions", required = false) String regionsStr) {

        var res = super.getContainers(null, acceptHeader, acceptEncoding, eurekaAccept, null, regionsStr);
        ObjectMapper mapper = new EurekaJsonJacksonCodec().getObjectMapper(Applications.class);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            var applications = mapper.readValue((String)res.getEntity(), Applications.class);
            return Mono.just(applications);
        } catch (IOException e) {
           log.error(e.getMessage(), e);
        }
        return Mono.error(new RuntimeException("Could not get applications"));
    }

    @GetMapping("/delta")
    public Mono<Response> getContainerDiff(
        @RequestHeader(HEADER_ACCEPT) String acceptHeader,
        @RequestHeader(HEADER_ACCEPT_ENCODING) String acceptEncoding,
        @RequestHeader(EurekaAccept.HTTP_X_EUREKA_ACCEPT) String eurekaAccept,
        @Nullable @RequestParam("regions") String regionsStr) {
        return Mono.just(super.getContainerDifferential(null, acceptHeader, acceptEncoding, eurekaAccept, null, regionsStr));
    }
}
