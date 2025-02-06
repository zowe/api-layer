package org.zowe.apiml;

import com.netflix.appinfo.EurekaAccept;
import com.netflix.eureka.resources.ApplicationResource;
import com.netflix.eureka.resources.ApplicationsResource;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/eureka/apps")
@DependsOn("modulithConfig")
public class MyApplicationsResource extends ApplicationsResource {
    private static final String HEADER_ACCEPT = "Accept";
    private static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";



    @GetMapping("/{appId}")
    public Mono<ApplicationResource> getAppResource(@PathParam("version") String version,
                                                    @PathParam("appId") String appId) {
        return Mono.just(super.getApplicationResource(version, appId));
    }

    @GetMapping
    public Mono<Response> getAllContainer(@PathParam("version") String version,
                                        @HeaderParam(HEADER_ACCEPT) String acceptHeader,
                                        @HeaderParam(HEADER_ACCEPT_ENCODING) String acceptEncoding,
                                        @HeaderParam(EurekaAccept.HTTP_X_EUREKA_ACCEPT) String eurekaAccept,
                                        @Context UriInfo uriInfo,
                                        @Nullable @QueryParam("regions") String regionsStr) {

        return Mono.just(super.getContainers(version, acceptHeader, acceptEncoding, eurekaAccept, uriInfo, regionsStr));
    }

    @GetMapping("/delta")
    public Mono<Response> getContainerDiff(@PathParam("version") String version,
                                                   @HeaderParam(HEADER_ACCEPT) String acceptHeader,
                                                   @HeaderParam(HEADER_ACCEPT_ENCODING) String acceptEncoding,
                                                   @HeaderParam(EurekaAccept.HTTP_X_EUREKA_ACCEPT) String eurekaAccept,
                                                   @Context UriInfo uriInfo,
                                                   @Nullable @QueryParam("regions") String regionsStr) {
        return Mono.just(super.getContainerDifferential(version, acceptHeader, acceptEncoding, eurekaAccept, uriInfo, regionsStr));
    }
}
