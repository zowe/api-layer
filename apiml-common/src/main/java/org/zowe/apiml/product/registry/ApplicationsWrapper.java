/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.registry;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

@JsonDeserialize(as = ApplicationsWrapper.class)
@Data
public class ApplicationsWrapper {

    private com.netflix.discovery.shared.Applications applications;
}
