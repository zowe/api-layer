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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A syntax-neutral parse tree.
 * <p>
 * JSON and XML disagree about how the same value is spelled - an XML attribute versus a JSON {@code @}-prefixed
 * key, element text versus a {@code $} key - but they agree on the shape. Both parsers therefore normalise into
 * this tree and a single mapper turns it into the model, so the field-by-field interpretation is written once.
 * <p>
 * Attributes are flattened into children, which is what makes the unification work: an XML
 * {@code <port enabled="true">} and a JSON {@code {"@enabled":"true"}} both become a child under a name the mapper
 * looks up with and without the {@code @} prefix.
 */
final class WireNode {

    private final String name;
    private String text;
    private final Map<String, List<WireNode>> children = new LinkedHashMap<>();

    WireNode(String name) {
        this.name = name;
    }

    String name() {
        return name;
    }

    String text() {
        return text;
    }

    void text(String text) {
        this.text = text;
    }

    void add(WireNode child) {
        children.computeIfAbsent(child.name(), key -> new ArrayList<>()).add(child);
    }

    /** All children under {@code name}, never null. */
    List<WireNode> all(String name) {
        List<WireNode> found = children.get(name);
        return found == null ? List.of() : found;
    }

    /**
     * The first child under any of {@code names}.
     * <p>
     * Several names are accepted so the mapper can ask for {@code "@enabled"} and {@code "enabled"} in one call
     * without caring which syntax produced the tree.
     */
    WireNode child(String... names) {
        for (String candidate : names) {
            List<WireNode> found = children.get(candidate);
            if (found != null && !found.isEmpty()) {
                return found.get(0);
            }
        }
        return null;
    }

    String string(String... names) {
        WireNode child = child(names);
        return child == null ? null : child.text();
    }

    /** The node's own text, or the text of a child - JSON puts a port's number under {@code $}, XML inline. */
    String valueOrChild(String... names) {
        if (text != null) {
            return text;
        }
        return string(names);
    }

    long number(long fallback, String... names) {
        String value = string(names);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    boolean bool(boolean fallback, String... names) {
        String value = string(names);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Boolean.parseBoolean(value.trim());
    }

    boolean hasChildren() {
        return !children.isEmpty();
    }

    Map<String, List<WireNode>> children() {
        return children;
    }

    @Override
    public String toString() {
        return "WireNode[" + name + (text != null ? "=" + text : "") + ", " + children.keySet() + "]";
    }

}
