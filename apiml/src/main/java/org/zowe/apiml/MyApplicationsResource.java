package org.zowe.apiml;

import com.netflix.eureka.resources.ApplicationsResource;
import jakarta.ws.rs.core.Response;
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

    @GetMapping
    public Mono<Response> apps() {
        return Mono.just(super.getContainers(null, null, null, null, null, null));
    }


}
