/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.webfinger;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class WebFingerResponse {

    private static final String RELATION_URI = "http://openid.net/specs/connect/1.0/issuer";
    private String subject;
    private List<Link> links;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Link {

        private String rel;
        private String href;

        public Link(String href) {
            this.rel = WebFingerResponse.RELATION_URI;
            this.href = href;
        }
    }
}
