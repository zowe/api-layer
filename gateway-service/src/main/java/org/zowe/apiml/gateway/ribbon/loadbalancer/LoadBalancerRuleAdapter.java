/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.ribbon.loadbalancer;

import com.google.common.base.Optional;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.*;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;
import lombok.extern.slf4j.Slf4j;
import org.zowe.apiml.gateway.context.ConfigurableNamedContextFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

/**
 * This adapter holds the load balancing logic by facilitating server selection.
 * There is plenty of debug log to increase supportability
 *
 */
@Slf4j
public class LoadBalancerRuleAdapter extends ClientConfigEnabledRoundRobinRule {

    private InstanceInfo instanceInfo;
    private ConfigurableNamedContextFactory<?> configurableNamedContextFactory;
    private Map<String, RequestAwarePredicate> predicateMap;

    // used zuul's implementation of round-robin server selection
    private AvailabilityPredicate availabilityPredicate;
    private AbstractServerPredicate zuulPredicate;


    /**
     * no-argument constructor has to be present for zuul
     */
    public LoadBalancerRuleAdapter() {
    }

    public LoadBalancerRuleAdapter(InstanceInfo instanceInfo, ConfigurableNamedContextFactory<?> configurableNamedContextFactory, IClientConfig config) {
        this.predicateMap = configurableNamedContextFactory.getInstances(instanceInfo.getAppName(), RequestAwarePredicate.class);

        this.instanceInfo = instanceInfo;
        this.configurableNamedContextFactory = configurableNamedContextFactory;

        //mirror original zuul setup
        availabilityPredicate = new AvailabilityPredicate(this, config);
        zuulPredicate = CompositePredicate.withPredicates(availabilityPredicate)
            .addFallbackPredicate(AbstractServerPredicate.alwaysTrue())
            .build();
    }

    @Override
    public Server choose(Object key) {
        log.debug("Choosing server: {}", key);
        ILoadBalancer lb = getLoadBalancer();
        LoadBalancingContext ctx = new LoadBalancingContext(instanceInfo.getAppName(), instanceInfo);
        List<Server> allServers = new ArrayList<>(lb.getAllServers());
        allServers.sort(comparing(Server::isReadyToServe).reversed().thenComparing(Server::getId)); // the original list is in the random order
        log.debug("Path: {}, List of servers from LoadBalancer: {}", ctx.getPath() ,allServers);
        for (RequestAwarePredicate predicate : predicateMap.values()) {
            log.debug("Running predicate: {}, list of servers: {}", allServers, predicate);
            allServers = allServers.stream()
                .map(server -> {
                    if (server instanceof DiscoveryEnabledServer) {
                        return (DiscoveryEnabledServer) server;
                    } else {
                        throw new IllegalStateException("Supplied Server is not instance of DiscoveryEnabledServer class");
                    }
                })
                .filter(server -> predicate.apply(ctx, server))
                .collect(Collectors.toList());
            log.debug("List of servers after predicate: {}", allServers);
        }
        log.debug("Running Zuul predicates");
        Optional<Server> server = zuulPredicate.chooseRoundRobinAfterFiltering(allServers, key); //NOSONAR following zuul's api
        if (server.isPresent()) {
            log.debug("Selected server: {}", server.get());
            return server.get();
        } else {
            log.debug("Did not select any server");
            return null;
        }
    }

    @Override
    public String toString() {
        return "LoadBalancerRuleAdapter{" +
            "info=" + instanceInfo +
            ", predicateFactory=" + configurableNamedContextFactory +
            ", predicates=" + predicateMap +
            '}';
    }
}
