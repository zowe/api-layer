/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.zowe.apiml;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.cloud.client.discovery.event.HeartbeatMonitor;
import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent;
import org.springframework.cloud.client.discovery.event.ParentHeartbeatEvent;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.util.Assert;

@Slf4j
public class RouteRefreshListener implements ApplicationListener<ApplicationEvent> {

    private final ApplicationEventPublisher publisher;

    private HeartbeatMonitor monitor = new HeartbeatMonitor();

    public RouteRefreshListener(ApplicationEventPublisher publisher) {
        Assert.notNull(publisher, "publisher may not be null");
        this.publisher = publisher;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            ContextRefreshedEvent refreshedEvent = (ContextRefreshedEvent) event;
            boolean isManagementCtxt = WebServerApplicationContext
                .hasServerNamespace(refreshedEvent.getApplicationContext(), "management");
            boolean isLoadBalancerCtxt = refreshedEvent.getApplicationContext().getDisplayName() != null
                    && refreshedEvent.getApplicationContext().getDisplayName().startsWith("LoadBalancerClientFactory-");

            if (!isManagementCtxt && !isLoadBalancerCtxt) {
                reset();
            }
        }
        else if (event instanceof RefreshScopeRefreshedEvent || event instanceof InstanceRegisteredEvent || event instanceof EurekaInstanceRegisteredEvent) {
            reset();
        }
        else if (event instanceof ParentHeartbeatEvent) {
            ParentHeartbeatEvent e = (ParentHeartbeatEvent) event;
            resetIfNeeded(e.getValue());
        }
        else if (event instanceof HeartbeatEvent) {
            HeartbeatEvent e = (HeartbeatEvent) event;
            resetIfNeeded(e.getValue());
        }
    }

    private void resetIfNeeded(Object value) {
        if (this.monitor.update(value)) {
            reset();
        }
    }

    private void reset() {
        this.publisher.publishEvent(new RefreshRoutesEvent(this));
    }

}
