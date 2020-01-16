/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.security.common.ticket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents /ticket JSON response with the ticket information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {
    private String token;
    private String userId;
    private String applicationName;
    private String ticket;
}
