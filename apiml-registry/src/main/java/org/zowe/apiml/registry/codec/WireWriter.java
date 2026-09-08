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

import com.fasterxml.jackson.core.JsonGenerator;
import org.zowe.apiml.registry.model.DataCenterInfo;
import org.zowe.apiml.registry.model.PortInfo;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

/**
 * The primitives the two syntaxes differ on.
 * <p>
 * Everything about <em>which</em> fields are written and in <em>what order</em> lives in {@link RegistryCodec};
 * this interface only covers how a single value is spelled. Keeping the ordering in one place matters because the
 * order is byte-compared against golden fixtures - duplicating it per syntax would guarantee they eventually drift.
 */
interface WireWriter extends AutoCloseable {

    void beginObject(String name) throws IOException;

    /** {@code name} is required by XML to emit the closing tag; JSON ignores it. */
    void endObject(String name) throws IOException;

    /** Writes a string field, or nothing at all when {@code value} is null. */
    void optionalString(String name, String value) throws IOException;

    void number(String name, long value) throws IOException;

    void bool(String name, boolean value) throws IOException;

    /** A port plus its enabled flag - an object in JSON, element text with an attribute in XML. */
    void port(String name, PortInfo port) throws IOException;

    void dataCenterInfo(DataCenterInfo dataCenterInfo) throws IOException;

    void metadata(Map<String, String> metadata) throws IOException;

    void beginArray(String name) throws IOException;

    void endArray() throws IOException;

    /** Starts an element/object that is a member of the enclosing array. */
    void beginArrayElement(String name) throws IOException;

    void endArrayElement(String name) throws IOException;

    /**
     * Where {@code metadata} falls in the field order.
     * <p>
     * This is the single structural difference between the two syntaxes: XML emits metadata immediately after
     * {@code isCoordinatingDiscoveryServer}, JSON emits it last, after {@code securePort}. Verified against the
     * instance-all-fields fixtures.
     */
    boolean metadataBeforeTimestamps();

    String result() throws IOException;

    @Override
    void close() throws IOException;

    /** JSON, via Jackson's streaming generator. */
    final class Json implements WireWriter {

        private final StringWriter out = new StringWriter();
        private final JsonGenerator generator;

        Json(com.fasterxml.jackson.core.JsonFactory factory) throws IOException {
            this.generator = factory.createGenerator(out);
        }

        @Override
        public void beginObject(String name) throws IOException {
            if (name == null) {
                generator.writeStartObject();
            } else {
                generator.writeObjectFieldStart(name);
            }
        }

        @Override
        public void endObject(String name) throws IOException {
            generator.writeEndObject();
        }

        @Override
        public void optionalString(String name, String value) throws IOException {
            if (value != null) {
                generator.writeStringField(name, value);
            }
        }

        @Override
        public void number(String name, long value) throws IOException {
            generator.writeNumberField(name, value);
        }

        @Override
        public void bool(String name, boolean value) throws IOException {
            generator.writeBooleanField(name, value);
        }

        @Override
        public void port(String name, PortInfo port) throws IOException {
            generator.writeObjectFieldStart(name);
            generator.writeNumberField(WireConstants.PORT_VALUE_KEY, port.port());
            // Deliberately a string, not a boolean - Eureka encodes the flag as "true"/"false".
            generator.writeStringField(WireConstants.PORT_ENABLED_KEY, Boolean.toString(port.enabled()));
            generator.writeEndObject();
        }

        @Override
        public void dataCenterInfo(DataCenterInfo dataCenterInfo) throws IOException {
            generator.writeObjectFieldStart("dataCenterInfo");
            generator.writeStringField(WireConstants.TYPE_DISCRIMINATOR_JSON, WireConstants.DATA_CENTER_INFO_CLASS);
            generator.writeStringField("name", dataCenterInfo.name().name());
            generator.writeEndObject();
        }

        @Override
        public void metadata(Map<String, String> metadata) throws IOException {
            generator.writeObjectFieldStart("metadata");
            if (metadata == null || metadata.isEmpty()) {
                generator.writeStringField(WireConstants.TYPE_DISCRIMINATOR_JSON, WireConstants.EMPTY_METADATA_CLASS);
            } else {
                for (Map.Entry<String, String> entry : metadata.entrySet()) {
                    generator.writeStringField(entry.getKey(), entry.getValue());
                }
            }
            generator.writeEndObject();
        }

        @Override
        public void beginArray(String name) throws IOException {
            generator.writeArrayFieldStart(name);
        }

        @Override
        public void endArray() throws IOException {
            generator.writeEndArray();
        }

        @Override
        public void beginArrayElement(String name) throws IOException {
            generator.writeStartObject();
        }

        @Override
        public void endArrayElement(String name) throws IOException {
            generator.writeEndObject();
        }

        @Override
        public boolean metadataBeforeTimestamps() {
            return false;
        }

        @Override
        public String result() throws IOException {
            generator.flush();
            return out.toString();
        }

        @Override
        public void close() throws IOException {
            generator.close();
        }

    }

    /**
     * XML, written directly.
     * <p>
     * Hand-writing this rather than using jackson-dataformat-xml keeps woodstox and stax2-api out of the
     * dependency tree - which is the point of the exercise - and makes the exact byte output, including the
     * attribute-versus-element choices, explicit and reviewable. The document shape is small and flat: elements,
     * two attributes, no namespaces, no mixed content beyond the port elements.
     */
    final class Xml implements WireWriter {

        private final StringBuilder out = new StringBuilder(512);

        @Override
        public void beginObject(String name) {
            if (name != null) {
                out.append('<').append(name).append('>');
            }
        }

        @Override
        public void endObject(String name) {
            if (name != null) {
                out.append("</").append(name).append('>');
            }
        }

        @Override
        public void optionalString(String name, String value) {
            if (value != null) {
                out.append('<').append(name).append('>');
                escape(value);
                out.append("</").append(name).append('>');
            }
        }

        @Override
        public void number(String name, long value) {
            out.append('<').append(name).append('>').append(value).append("</").append(name).append('>');
        }

        @Override
        public void bool(String name, boolean value) {
            out.append('<').append(name).append('>').append(value).append("</").append(name).append('>');
        }

        @Override
        public void port(String name, PortInfo port) {
            out.append('<').append(name).append(' ').append(WireConstants.PORT_ENABLED_ATTRIBUTE).append("=\"")
                .append(port.enabled()).append("\">").append(port.port())
                .append("</").append(name).append('>');
        }

        @Override
        public void dataCenterInfo(DataCenterInfo dataCenterInfo) {
            out.append("<dataCenterInfo ").append(WireConstants.TYPE_DISCRIMINATOR_XML).append("=\"");
            escape(WireConstants.DATA_CENTER_INFO_CLASS);
            out.append("\"><name>").append(dataCenterInfo.name().name()).append("</name></dataCenterInfo>");
        }

        @Override
        public void metadata(Map<String, String> metadata) {
            if (metadata == null || metadata.isEmpty()) {
                out.append("<metadata/>");
                return;
            }
            out.append("<metadata>");
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                optionalString(entry.getKey(), entry.getValue());
            }
            out.append("</metadata>");
        }

        @Override
        public void beginArray(String name) {
            // XML repeats the element rather than wrapping a list, so there is no array marker to write.
        }

        @Override
        public void endArray() {
            // See beginArray.
        }

        @Override
        public void beginArrayElement(String name) {
            out.append('<').append(name).append('>');
        }

        @Override
        public void endArrayElement(String name) {
            out.append("</").append(name).append('>');
        }

        @Override
        public boolean metadataBeforeTimestamps() {
            return true;
        }

        @Override
        public String result() {
            return out.toString();
        }

        @Override
        public void close() {
            // Nothing to release.
        }

        private void escape(String value) {
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                switch (c) {
                    case '&' -> out.append("&amp;");
                    case '<' -> out.append("&lt;");
                    case '>' -> out.append("&gt;");
                    case '"' -> out.append("&quot;");
                    case '\'' -> out.append("&apos;");
                    default -> out.append(c);
                }
            }
        }

    }

}
