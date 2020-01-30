/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.discovery.staticdef;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * A wrapper for static definition file contents.
 * It used by Jackson object mapper.
 */
@Data
public class Definition {
    private List<Service> services;
    private Map<String, CatalogUiTile> catalogUiTiles;
    private List<ServiceOverride> additionalServiceMetadata;
}
