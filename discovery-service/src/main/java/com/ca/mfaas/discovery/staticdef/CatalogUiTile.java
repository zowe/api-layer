/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.discovery.staticdef;

import lombok.Data;

/**
 * Represents one API Catalog UI tile (groups services together)
 */
@Data
public class CatalogUiTile {
    private String id;
    private String title;
    private String description;
    private String version;
}
