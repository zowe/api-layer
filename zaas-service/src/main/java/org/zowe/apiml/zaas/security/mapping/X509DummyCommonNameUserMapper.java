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

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSource;
import java.util.List;

/**
 * Dummy mapper to extract username from CN supporting non-mapped users.
 */
@Component("x509Mapper")
@ConditionalOnExpression(
    "T(org.apache.commons.lang3.StringUtils).isEmpty('${apiml.security.x509.externalMapperUrl:}') && '${apiml.security.useInternalMapper:false}' == 'false' && '${apiml.security.useDummyCNMapper:false}' == 'true'"
)
public class X509DummyCommonNameUserMapper extends X509CommonNameUserMapper {

    private static final List<String> nonMappedUsers = List.of(
        "unknownuser",
        "unknown_user"
    );

    @Override
    public String mapToMainframeUserId(AuthSource authSource) {
        var mappedUser = super.mapToMainframeUserId(authSource);
        return nonMappedUsers.contains(mappedUser.toLowerCase()) ? null : mappedUser;
    }

}
