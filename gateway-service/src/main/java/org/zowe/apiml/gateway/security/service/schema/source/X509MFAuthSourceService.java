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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.zowe.apiml.gateway.security.login.x509.X509AbstractMapper;

/**
 * Implementation of AuthSourceService interface which uses client certificate as an authentication source.
 * <p>
 * This implementation perform the mapping between common name from the client certificate and the mainframe user ID.
 */
@Slf4j
@Service
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class X509MFAuthSourceService extends AbstractX509AuthSourceService  {
    private X509AbstractMapper mapper;

    public X509MFAuthSourceService(@Autowired X509AbstractMapper mapper) {
        this.mapper = mapper;
    }

    // Method for testing purpose only
    protected void setMapper(X509AbstractMapper mapper) { this.mapper = mapper; }

    @Override
    public boolean isValid(AuthSource authSource) {
        return super.isValid(authSource, mapper);
    }

    @Override
    public AuthSource.Parsed parse(AuthSource authSource) {
        return super.parse(authSource, mapper);
    }
}
