/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.service.security.service.discovery.staticdef;

import java.util.List;
import java.util.Map;

/**
 * A wrapper for static definition file contents.
 * It used by Jackson object mapper.
 */
class Definition {
    private List<Service> services;
    private Map<String, CatalogUiTile> catalogUiTiles;

    public Definition() {
    }

    public List<Service> getServices() {
        return this.services;
    }

    public void setServices(List<Service> services) {
        this.services = services;
    }

    public Map<String, CatalogUiTile> getCatalogUiTiles() {
        return this.catalogUiTiles;
    }

    public void setCatalogUiTiles(Map<String, CatalogUiTile> catalogUiTiles) {
        this.catalogUiTiles = catalogUiTiles;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Definition)) return false;
        final Definition other = (Definition) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$services = this.getServices();
        final Object other$services = other.getServices();
        if (this$services == null ? other$services != null : !this$services.equals(other$services)) return false;
        final Object this$catalogUiTiles = this.getCatalogUiTiles();
        final Object other$catalogUiTiles = other.getCatalogUiTiles();
        if (this$catalogUiTiles == null ? other$catalogUiTiles != null : !this$catalogUiTiles.equals(other$catalogUiTiles))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof Definition;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $services = this.getServices();
        result = result * PRIME + ($services == null ? 43 : $services.hashCode());
        final Object $catalogUiTiles = this.getCatalogUiTiles();
        result = result * PRIME + ($catalogUiTiles == null ? 43 : $catalogUiTiles.hashCode());
        return result;
    }

    public String toString() {
        return "Definition(services=" + this.getServices() + ", catalogUiTiles=" + this.getCatalogUiTiles() + ")";
    }
}
