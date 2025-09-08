/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.mapping;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSource;
import org.zowe.apiml.zaas.security.service.schema.source.OIDCAuthSource;

import java.util.function.UnaryOperator;

import static org.zowe.apiml.zaas.security.mapping.model.MapperResponse.OIDC_FAILED_MESSAGE_KEY;

@Component
@ConditionalOnBean(name = "oidcMapper")
public class OIDCMapperHelper implements InitializingBean {

    @Value("${apiml.security.oidc.registry:}")
    protected String registry;

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    protected boolean isConfigError = false;

    @Override
    public void afterPropertiesSet() {
        if (StringUtils.isBlank(registry)) {
            isConfigError = true;
            apimlLog.log("org.zowe.apiml.security.common.OIDCConfigError");
        }
    }

    /**
     * Maps the authSource distribution id to a mainframe user. The method validates OIDC mapping configuration and authSource and if these are valid, invokes the mapper.
     * @param authSource OidcAuthSource with the distributed id to map
     * @param mapper the mapper function with the actual mapping logic, accepts the authSource distributed id and returns a mainframe user id on success or null otherwise
     * @return returns result of the mapper or null on validation failure
     */

    public String mapToMainframeUserId(AuthSource authSource, UnaryOperator<String> mapper) {

        if (isConfigError) {
            apimlLog.log("org.zowe.apiml.security.common.OIDCConfigError");
            return null;
        }

        if (mapper == null) {
            apimlLog.log(MessageType.ERROR, "OIDC token mapping invoked but no mapper provided");
            return null;
        }

        if (!(authSource instanceof OIDCAuthSource)) {
            apimlLog.log(MessageType.DEBUG, "The used authentication source type is {} and not OIDC", authSource.getType());
            return null;
        }

        var distributedIds = ((OIDCAuthSource) authSource).getDistributedId();
        if (distributedIds == null || distributedIds.isEmpty()) {
            apimlLog.log(OIDC_FAILED_MESSAGE_KEY,
                "OIDC token is missing the distributed ID. Make sure your distributed identity provider is" +
                    " properly configured.");
            return null;
        }

        for (String distributedId : distributedIds) {
            if (StringUtils.isNotBlank(distributedId)) {
                var mainframeUserId = mapper.apply(distributedId);
                if (StringUtils.isNotBlank(mainframeUserId)) {
                    return mainframeUserId;
                }
            }
        }

        apimlLog.log(MessageType.DEBUG, "No mainframe user mapping found for distributed ids {}", distributedIds);
        return null;
    }
}
