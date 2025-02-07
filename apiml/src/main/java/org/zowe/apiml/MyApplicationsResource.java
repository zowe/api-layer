package org.zowe.apiml;

import com.netflix.appinfo.EurekaAccept;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.cluster.PeerEurekaNode;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import com.netflix.eureka.resources.ApplicationResource;
import com.netflix.eureka.resources.ApplicationsResource;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.IOException;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.zowe.apiml.EurekaConfiguration.JACKSON_JSON;

@RestController
@RequiredArgsConstructor
@RequestMapping(path="/eureka/apps",produces="application/json")
@DependsOn("modulithConfig")
@Slf4j
public class MyApplicationsResource extends ApplicationsResource {
    private static final String HEADER_ACCEPT = "Accept";
    private static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";

    @Autowired
    private PeerAwareInstanceRegistry registry;

    private final EurekaServerConfig serverConfig;

    @GetMapping("/{appId}")
    public Mono<ApplicationResource> getAppResource(
        @PathVariable("appId") String appId) {
        return Mono.just(super.getApplicationResource(null, appId));
    }

    @GetMapping(produces = { "application/xml", "application/json" }, consumes = MediaType.ALL_VALUE)
    public Mono<String> getAllContainer(
        @Nullable @RequestHeader(HEADER_ACCEPT) String acceptHeader,
        @Nullable @RequestHeader(HEADER_ACCEPT_ENCODING) String acceptEncoding,
        @Nullable @RequestHeader(EurekaAccept.HTTP_X_EUREKA_ACCEPT) String eurekaAccept,
        @Nullable @RequestParam(value = "regions", required = false) String regionsStr) {

        var res = super.getContainers(null, acceptHeader, acceptEncoding, eurekaAccept, null, regionsStr);
        return Mono.just((String)res.getEntity());
    }

    @GetMapping("/delta")
    public Mono<String> getContainerDiff(
        @Nullable @RequestHeader(HEADER_ACCEPT) String acceptHeader,
        @Nullable @RequestHeader(HEADER_ACCEPT_ENCODING) String acceptEncoding,
        @Nullable @RequestHeader(EurekaAccept.HTTP_X_EUREKA_ACCEPT) String eurekaAccept,
        @Nullable @RequestParam("regions") String regionsStr) {
        return Mono.just((String)super.getContainerDifferential(null, acceptHeader, acceptEncoding, eurekaAccept, null, regionsStr).getEntity());
    }

        @PostMapping(value = "/{appName}")
    public Mono<ResponseEntity<Void>> addInstance(@PathVariable String appName,
                                                  @RequestBody String info,
                                                  @Nullable @RequestHeader(PeerEurekaNode.HEADER_REPLICATION) String isReplication) throws IOException {
          var i =  JACKSON_JSON.decode(info,InstanceInfo.class);
            log.debug("Registering instance {} (replication={})", i.getId(), isReplication);
            if (isBlank(i.getId())) {
            return Mono.just(ResponseEntity.badRequest().header("error", "Missing instanceId").build());
        } else if (isBlank(i.getHostName())) {
            return Mono.just(ResponseEntity.badRequest().header("error", "Missing hostname").build());
        } else if (isBlank(i.getIPAddr())) {
            return Mono.just(ResponseEntity.badRequest().header("error", "Missing IP address").build());
        } else if (isBlank(i.getAppName())) {
            return Mono.just(ResponseEntity.badRequest().header("error", "Missing appName").build());
        } else if (!appName.equals(i.getAppName())) {
            return Mono.just(ResponseEntity.badRequest().header("error", "Mismatched appName").build());
        } else if (i.getDataCenterInfo() == null) {
            return Mono.just(ResponseEntity.badRequest().header("error", "Missing dataCenterInfo").build());
        } else if (i.getDataCenterInfo().getName() == null) {
            return Mono.just(ResponseEntity.badRequest().header("error", "Missing dataCenterInfo Name").build());
        }

        registry.register(i, "true".equals(isReplication));
        return Mono.just(ResponseEntity.noContent().build()); // 204 to be backwards compatible
    }

    @PutMapping(value = "/{appName}/{instanceId}")
    public Mono<Response> renewLease(
        @Nullable @RequestHeader(PeerEurekaNode.HEADER_REPLICATION) String isReplication,
        @PathVariable String appName,
        @PathVariable String instanceId,
        @Nullable @RequestParam("overriddenstatus") String overriddenStatus,
        @Nullable @RequestParam("status") String status,
       @Nullable @RequestParam("lastDirtyTimestamp") String lastDirtyTimestamp) {
        boolean isFromReplicaNode = "true".equals(isReplication);
//        var appResource =  getApplicationResource(null,appName);
//        var ii =  appResource.getInstanceInfo(instanceId);
        boolean isSuccess = registry.renew(appName, instanceId, isFromReplicaNode);

        // Not found in the registry, immediately ask for a register
        if (!isSuccess) {
            log.warn("Not Found (Renew): {} - {}", appName, instanceId);
            return Mono.just(Response.status(Response.Status.NOT_FOUND).build());
        }
        // Check if we need to sync based on dirty time stamp, the client
        // instance might have changed some value
        Response response;
        if (lastDirtyTimestamp != null && serverConfig.shouldSyncWhenTimestampDiffers()) {
            response = this.validateDirtyTimestamp(Long.valueOf(lastDirtyTimestamp), isFromReplicaNode, appName, instanceId);
            // Store the overridden status since the validation found out the node that replicates wins
            if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()
                && (overriddenStatus != null)
                && !(InstanceInfo.InstanceStatus.UNKNOWN.name().equals(overriddenStatus))
                && isFromReplicaNode) {
                registry.storeOverriddenStatusIfRequired(appName, instanceId, InstanceInfo.InstanceStatus.valueOf(overriddenStatus));
            }
        } else {
            response = Response.ok().build();
        }
        log.debug("Found (Renew): {} - {}; reply status={}", appName, instanceId, response.getStatus());
        return Mono.just(response);
    }


    private Response validateDirtyTimestamp(Long lastDirtyTimestamp,
                                            boolean isReplication, String appName, String id) {
        InstanceInfo appInfo = registry.getInstanceByAppAndId(appName, id, false);
        if (appInfo != null) {
            if ((lastDirtyTimestamp != null) && (!lastDirtyTimestamp.equals(appInfo.getLastDirtyTimestamp()))) {
                Object[] args = {id, appInfo.getLastDirtyTimestamp(), lastDirtyTimestamp, isReplication};

                if (lastDirtyTimestamp > appInfo.getLastDirtyTimestamp()) {
                    log.debug(
                        "Time to sync, since the last dirty timestamp differs -"
                            + " ReplicationInstance id : {},Registry : {} Incoming: {} Replication: {}",
                        args);
                    return Response.status(Response.Status.NOT_FOUND).build();
                } else if (appInfo.getLastDirtyTimestamp() > lastDirtyTimestamp) {
                    // In the case of replication, send the current instance info in the registry for the
                    // replicating node to sync itself with this one.
                    if (isReplication) {
                        log.debug(
                            "Time to sync, since the last dirty timestamp differs -"
                                + " ReplicationInstance id : {},Registry : {} Incoming: {} Replication: {}",
                            args);
                        return Response.status(Response.Status.CONFLICT).entity(appInfo).build();
                    } else {
                        return Response.ok().build();
                    }
                }
            }

        }
        return Response.ok().build();
    }
}
