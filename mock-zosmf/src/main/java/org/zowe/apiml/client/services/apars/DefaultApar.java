/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.client.services.apars;

import org.springframework.http.ResponseEntity;
import org.zowe.apiml.client.services.versions.Apar;

import java.util.Optional;

public class DefaultApar implements Apar {
    @Override
    public Optional<ResponseEntity<?>> apply(Object... parameters) {
        return (Optional<ResponseEntity<?>>) parameters[2];
    }
}
