/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.ticket;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents /ticket JSON response with the ticket information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponse {

    private String token;
    private String userId;
    private String applicationName;
    private String ticket;
    @JsonIgnore // to avoid a breaking change, this value is needed only in Otel via API call, not rest
    private List<String> distributedIds;
    @JsonIgnore // to avoid a breaking change, this value is needed only in Otel via API call, not rest
    private String authSourceType;
    @JsonIgnore // to avoid a breaking change, this value is needed only in Otel via API call, not rest
    private String errorType;

}
