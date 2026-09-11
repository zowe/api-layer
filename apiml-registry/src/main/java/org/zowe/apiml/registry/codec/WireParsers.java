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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;

/** Turns a JSON or XML payload into a {@link WireNode} tree. */
final class WireParsers {

    private final ObjectMapper jsonMapper = new ObjectMapper();

    /**
     * XML reader factory, hardened against entity attacks.
     * <p>
     * This matters: registration payloads are untrusted input, and Zowe's own documentation tells customers to
     * POST XML directly to /eureka/apps. Disabling DTD support and external entities closes XXE and billion-laughs
     * at the parser rather than relying on a downstream default. Doing this explicitly - rather than inheriting
     * whatever jackson-dataformat-xml and woodstox happen to be configured with - is part of why XML is read with
     * the JDK's own StAX here.
     */
    private static final XMLInputFactory XML_INPUT_FACTORY = createHardenedXmlInputFactory();

    private static XMLInputFactory createHardenedXmlInputFactory() {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        return factory;
    }

    WireNode parseJson(String payload) throws IOException {
        JsonNode root = jsonMapper.readTree(payload);
        WireNode node = new WireNode("");
        convertJson(root, node);
        return node;
    }

    private void convertJson(JsonNode source, WireNode target) {
        if (source.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = source.properties().iterator();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                addJsonChild(target, field.getKey(), field.getValue());
            }
        } else if (source.isArray()) {
            // An unnamed array only appears nested under a named field, handled in addJsonChild.
            for (JsonNode element : source) {
                addJsonChild(target, "", element);
            }
        } else if (!source.isNull()) {
            target.text(source.asText());
        }
    }

    private void addJsonChild(WireNode parent, String name, JsonNode value) {
        if (value.isArray()) {
            for (JsonNode element : value) {
                WireNode child = new WireNode(name);
                convertJson(element, child);
                parent.add(child);
            }
            return;
        }
        WireNode child = new WireNode(name);
        convertJson(value, child);
        parent.add(child);
    }

    WireNode parseXml(String payload) throws XMLStreamException {
        XMLStreamReader reader = XML_INPUT_FACTORY.createXMLStreamReader(new StringReader(payload));
        try {
            WireNode root = new WireNode("");
            Deque<WireNode> stack = new ArrayDeque<>();
            stack.push(root);

            while (reader.hasNext()) {
                switch (reader.next()) {
                    case XMLStreamConstants.START_ELEMENT -> {
                        WireNode node = new WireNode(reader.getLocalName());
                        // Attributes are flattened into children so the mapper can treat them like JSON keys.
                        for (int i = 0; i < reader.getAttributeCount(); i++) {
                            WireNode attribute = new WireNode(reader.getAttributeLocalName(i));
                            attribute.text(reader.getAttributeValue(i));
                            node.add(attribute);
                        }
                        stack.peek().add(node);
                        stack.push(node);
                    }
                    case XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA -> {
                        String text = reader.getText();
                        if (!text.isBlank()) {
                            WireNode current = stack.peek();
                            current.text(current.text() == null ? text : current.text() + text);
                        }
                    }
                    case XMLStreamConstants.END_ELEMENT -> stack.pop();
                    default -> {
                        // Comments, processing instructions and whitespace carry no contract meaning.
                    }
                }
            }
            return root;
        } finally {
            reader.close();
        }
    }

}
