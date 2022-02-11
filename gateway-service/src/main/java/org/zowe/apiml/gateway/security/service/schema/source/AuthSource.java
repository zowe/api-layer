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

import java.io.Serializable;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.zowe.apiml.security.common.token.QueryResponse.Source;

/**
 * Interface defines simple source of authentication.
 */
public interface AuthSource extends Serializable {
    Object getSource();

    @AllArgsConstructor
    @Getter
    @EqualsAndHashCode
    class Parsed {
        private final String userId;
        private Date creation;
        private Date expiration;
        private final Source source;
    }
}
