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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.X509AuthSource;
import org.zowe.commons.usermap.CertificateResponse;
import org.zowe.commons.usermap.UserMapper;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

@Slf4j
@Component("x509Mapper")
@ConditionalOnExpression("'${apiml.security.x509.externalMapperUrl:}'.isEmpty() && '${isRunningOnZ:false}' == 'true'")
public class X509NativeMapper implements AuthenticationMapper {

    @Override
    public String mapToMainframeUserId(AuthSource authSource) {
        if(authSource instanceof X509AuthSource) {
            X509AuthSource x509AuthSource = (X509AuthSource)authSource;
            X509Certificate certificate = x509AuthSource.getRawSource();
            if(certificate != null) {
                UserMapper userMapper = new UserMapper();
                try {
                   CertificateResponse response = userMapper.getUserIDForCertificate(certificate.getEncoded());
                   if(response.getRc() == 0 && StringUtils.isNotEmpty(response.getUserId())){
                       return response.getUserId();
                   }
                   log.debug(response.toString());
                } catch (CertificateEncodingException e) {
                    log.error("Can`t get encoded data from certificate", e);
                }
            }
        }
        return null;
    }
}
