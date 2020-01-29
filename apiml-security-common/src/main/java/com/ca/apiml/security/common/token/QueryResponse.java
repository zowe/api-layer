/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.security.common.token;

import com.ca.mfaas.cache.EntryExpiration;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.StringUtils;

import java.util.Date;

/**
 * Represents the query JSON response with the token information
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryResponse implements EntryExpiration {

    private String domain;
    private String userId;
    private Date creation;
    private Date expiration;
    private Source source;

    @Override
    public boolean isExpired() {
        return expiration.before(new Date());
    }

    public enum Source {

            ZOWE,
            ZOSMF

        ;

        public static Source valueByIssuer(String issuer) {
            if (StringUtils.equalsIgnoreCase(issuer, "zOSMF")) {
                return ZOSMF;
            }

            return ZOWE;
        }

    }

}
