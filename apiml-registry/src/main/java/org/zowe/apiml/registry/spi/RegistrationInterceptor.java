/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.spi;

import org.zowe.apiml.registry.model.ServiceInstance;

/**
 * A hook applied to every instance on its way into the registry.
 * <p>
 * This is the seam that APIML's registry customisations plug into. In the Eureka-based implementation they were
 * subclass overrides of {@code register}, which is why {@code ApimlInstanceRegistry} needed reflection into
 * Eureka's private state to work at all. As an interceptor chain, service-id prefix rewriting, metadata
 * translation, conformance warnings and the domain allow-list are all just ordinary implementations.
 */
public interface RegistrationInterceptor {

    /**
     * Inspect or transform an instance before it is stored.
     *
     * @param instance the incoming instance
     * @return the instance to store; return the argument unchanged to pass it through
     * @throws org.zowe.apiml.registry.RegistrationRejectedException to refuse the registration outright
     */
    ServiceInstance intercept(ServiceInstance instance);

    /** Lower runs first. */
    default int order() {
        return 0;
    }

}
