package org.zowe.apiml;

import com.netflix.appinfo.EurekaAccept;
import com.netflix.eureka.resources.ApplicationResource;
import com.netflix.eureka.resources.ApplicationsResource;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/eureka/apps")
@DependsOn("modulithConfig")
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
        return Mono.just(res.getEntity());
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
