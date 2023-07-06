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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.Serializable;

@Data
@Configuration
@ConfigurationProperties(prefix = "apiml.catalog.custom-style", ignoreInvalidFields = true)
@JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.PUBLIC_ONLY, setterVisibility = Visibility.PUBLIC_ONLY)
public class CustomStyleConfig implements Serializable {

    private String logo = "";
    private String titlesColor = "";
    private String fontFamily = "";
    private String hoverColor = "";
    private String focusColor = "";
    private String hyperlinksColor = "";
    private String boxShadowColor = "";
    private String headerColor = "";
    private String backgroundColor = "";
    private String docLink = "";

}
