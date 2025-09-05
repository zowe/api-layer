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

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSource;
import org.zowe.commons.usermap.MapperResponse;

@RequiredArgsConstructor
@Component("oidcMapper")
@ConditionalOnExpression("'${apiml.security.oidc.enabled:false}' == 'true' && '${apiml.security.useInternalMapper:false}' == 'true'")
public class OIDCNativeMapper implements AuthenticationMapper {

    private final NativeMapperWrapper nativeMapper;

    private final OIDCMapperHelper mapperHelper;

    @Override
    public String mapToMainframeUserId(AuthSource authSource) {
        return mapperHelper.mapToMainframeUserId(authSource, distributedId -> {
            MapperResponse response = nativeMapper.getUserIDForDN(distributedId, mapperHelper.registry);
            if (response.getRc() == 0 && StringUtils.isNotBlank(response.getUserId())) {
                return response.getUserId();
            }
            return null;
        });
    }
}
