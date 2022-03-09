/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.service.schema.source;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.zowe.apiml.gateway.security.login.x509.X509CommonNameUserMapper;

/**
 * Implementation of AuthSourceService interface which uses client certificate as an authentication source.
 * <p>
 * This implementation does not perform the mapping between common name from the client certificate and the mainframe
 * user ID, it treats common name as user ID. For this purpose the instance of {@link X509CommonNameUserMapper}
 * is always used for validation and parsing of the client certificate.
 */
@Slf4j
@Service
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class X509CNAuthSourceService extends AbstractX509AuthSourceService {
    private X509CommonNameUserMapper mapper;

    public X509CNAuthSourceService() {
        mapper = new X509CommonNameUserMapper();
    }

    // Method for testing purpose only
    protected void setMapper(X509CommonNameUserMapper mapper) { this.mapper = mapper; }

    @Override
    public boolean isValid(AuthSource authSource) {
        return super.isValid(authSource, mapper);
    }

    @Override
    public AuthSource.Parsed parse(AuthSource authSource) {
        return super.parse(authSource, mapper);
    }
}
