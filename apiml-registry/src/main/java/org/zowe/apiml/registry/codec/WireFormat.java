/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.codec;

/**
 * The four representations the registry serves: two syntaxes, each in a full and a compact projection.
 * <p>
 * The compact ("mini") projection is selected by the {@code X-Eureka-Accept: compact} request header and omits the
 * fields a load balancer does not need. Which fields it omits is not a judgement call - it is pinned by the
 * wire-contract fixtures.
 */
public enum WireFormat {

    JSON_FULL(Syntax.JSON, false),
    JSON_COMPACT(Syntax.JSON, true),
    XML_FULL(Syntax.XML, false),
    XML_COMPACT(Syntax.XML, true);

    public enum Syntax {
        JSON,
        XML
    }

    private final Syntax syntax;
    private final boolean compact;

    WireFormat(Syntax syntax, boolean compact) {
        this.syntax = syntax;
        this.compact = compact;
    }

    public Syntax syntax() {
        return syntax;
    }

    public boolean compact() {
        return compact;
    }

    public static WireFormat of(Syntax syntax, boolean compact) {
        if (syntax == Syntax.JSON) {
            return compact ? JSON_COMPACT : JSON_FULL;
        }
        return compact ? XML_COMPACT : XML_FULL;
    }

}
