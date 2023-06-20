/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "apiml.catalog.custom-style", ignoreInvalidFields = true)
public class CustomStyleConfig {
    private String titlesColor = "";
    private String font = "";
    private String hoverColor = "";
    private String focusColor = "";
    private String hyperlinksColor = "";
    private String boxShadowColor = "";

    private DashboardPage dashboardPage;
    private DetailPage detailPage;
    private Header header;
    private TilesAndNavMenu tilesAndNavMenu;

    @Data
    public static class Header {
        private String backgroundColor = "";

    }

    @Data
    public static class DashboardPage {
        private String backgroundColor = "";
    }

    @Data
    public static class DetailPage {
        private String backgroundColor = "";
    }

    @Data
    public static class TilesAndNavMenu {
        private String backgroundColor = "";
        private String borderColor = "";
    }
}
