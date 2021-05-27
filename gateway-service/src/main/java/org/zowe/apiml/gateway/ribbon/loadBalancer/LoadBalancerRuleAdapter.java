/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.ribbon.loadBalancer;

import com.google.common.base.Optional;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.loadbalancer.*;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LoadBalancerRuleAdapter extends ClientConfigEnabledRoundRobinRule {

    private InstanceInfo info;
    private PredicateFactory predicateFactory;
    private Map<String, RequestAwarePredicate> instances;

    // used zuul's implementation of round robin server selection
    private static AbstractServerPredicate zuulNonFilteringPredicate = new AbstractServerPredicate() {
        @Override
        public boolean apply(@Nullable PredicateKey input) {
            return true;
        }
    };

    public LoadBalancerRuleAdapter() {
    }

    public LoadBalancerRuleAdapter(InstanceInfo info, PredicateFactory predicateFactory) {
        this.instances = predicateFactory.getInstances(info.getAppName(), RequestAwarePredicate.class);
        this.info = info;
        this.predicateFactory = predicateFactory;
    }

    @Override
    public Server choose(Object key) {
        ILoadBalancer lb = getLoadBalancer();
        LoadBalancingContext ctx = new LoadBalancingContext(info.getId(), info);
        List<Server> allServers = lb.getAllServers();
        for (RequestAwarePredicate predicate : instances.values()) {
            allServers = allServers.stream().filter(server -> predicate.apply(ctx, server)).collect(Collectors.toList());
        }
        Optional<Server> server = zuulNonFilteringPredicate.chooseRoundRobinAfterFiltering(allServers, key);
        if (server.isPresent()) {
            return server.get();
        } else {
            return null;
        }
    }
}
