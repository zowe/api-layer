/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.util.requests;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpMethod;

import java.net.URI;

@Builder
@Getter
public class RequestParams {
    private final URI uri;
    private final String authentication;

    @Builder.Default
    private final HttpMethod method = HttpMethod.GET;
}
