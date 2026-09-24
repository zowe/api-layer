/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.staticapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.zowe.apiml.product.discovery.StaticServicesRegistration;
import org.zowe.apiml.registry.codec.RegistryJacksonModule;
import reactor.core.publisher.Mono;

import static org.apache.hc.core5.http.HttpStatus.SC_OK;

/**
 * The modulith's static-definition refresh, answering from the registry it shares a JVM with.
 * <p>
 * The response body is the service's own {@code StaticRegistrationResult}, which nests
 * {@link org.zowe.apiml.registry.model.ServiceInstance}. That model carries no Jackson annotations on purpose -
 * the wire format belongs to the codec - so a plain {@code ObjectMapper} cannot introspect its record-style
 * accessors and every refresh failed with:
 * <pre>
 *   InvalidDefinitionException: No serializer found for class org.zowe.apiml.registry.model.ServiceInstance
 *   and no properties discovered to create BeanSerializer
 *   (through reference chain: StaticRegistrationResult["instances"]-&gt;LinkedList[0])
 * </pre>
 * This class used to hold a private {@code new ObjectMapper()} - a mapper with no modules at all, which is
 * exactly the mapper that cannot serialise the model. It now uses the application's own {@code ObjectMapper}
 * and makes sure {@link RegistryJacksonModule} is registered on it, rather than depending on some other
 * component having done so. Registering a module the application already registers is a no-op: Jackson skips a
 * module whose id is already present.
 */
@Slf4j
@Service
@ConditionalOnBean(name = "modulithConfig")
public class StaticRegistrationServiceApi implements StaticRegistrationService {

    private final ObjectMapper mapper;
    private final StaticServicesRegistration staticServicesRegistration;

    public StaticRegistrationServiceApi(ObjectMapper mapper, StaticServicesRegistration staticServicesRegistration) {
        this.mapper = mapper.registerModule(new RegistryJacksonModule());
        this.staticServicesRegistration = staticServicesRegistration;
    }

    @Override
    public Mono<StaticAPIResponse> refresh() {
        try {
            var result = staticServicesRegistration.reloadServices();
            return Mono.just(new StaticAPIResponse(SC_OK, mapper.writeValueAsString(result)));
        } catch (JsonProcessingException e) {
            log.error("Cannot serialize the list of static API services", e);
            return Mono.error(new IllegalStateException(e));
        }
    }

}
