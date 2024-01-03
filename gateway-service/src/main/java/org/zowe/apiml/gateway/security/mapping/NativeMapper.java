/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.mapping;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.zowe.commons.usermap.CertificateResponse;
import org.zowe.commons.usermap.MapperResponse;
import org.zowe.commons.usermap.UserMapper;

/**
 * Native on platform mapper. Depends on <a href="https://github.com/zowe/common-java/tree/v2.x.x/zos-utils">zos-utils</a> library
 * which provides native calls to z/OS.
 */
@Component
@ConditionalOnExpression("'${apiml.security.useInternalMapper:false}' == 'true'")
public class NativeMapper implements NativeMapperWrapper {
    UserMapper userMapper;

    public NativeMapper() {
        this.userMapper = new UserMapper();
    }
    @Override
    public CertificateResponse getUserIDForCertificate(byte[] cert) {
        return userMapper.getUserIDForCertificate(cert);
    }

    @Override
    public MapperResponse getUserIDForDN(String dn, String registry) {
        return userMapper.getUserIDForDN(dn, registry);
    }
}
