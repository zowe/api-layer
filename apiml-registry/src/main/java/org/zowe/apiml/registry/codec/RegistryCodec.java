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

import com.fasterxml.jackson.core.JsonFactory;
import org.zowe.apiml.registry.model.Application;
import org.zowe.apiml.registry.model.Applications;
import org.zowe.apiml.registry.model.ServiceInstance;
import org.zowe.apiml.registry.replication.ReplicationAction;
import org.zowe.apiml.registry.replication.ReplicationBatch;
import org.zowe.apiml.registry.replication.ReplicationItem;
import org.zowe.apiml.registry.replication.ReplicationResponse;

import javax.xml.stream.XMLStreamException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Encodes registry objects in the Eureka wire format.
 * <p>
 * The field order below is the contract. It is asserted byte-for-byte against the fixtures in
 * apiml-registry/src/test/resources/wire-contract, which were captured from Eureka 2.0.6 itself, so reordering
 * anything here is a breaking change for every deployed enabler regardless of whether the JSON stays semantically
 * equivalent. The order is written once and shared by both syntaxes; the only structural difference between them
 * is where {@code metadata} lands, which {@link WireWriter#metadataBeforeTimestamps()} decides.
 */
public final class RegistryCodec {

    private final JsonFactory jsonFactory = new JsonFactory();
    private final WireParsers parsers = new WireParsers();

    public String encode(ServiceInstance instance, WireFormat format) {
        return write(format, writer -> {
            writer.beginObject(null);
            writeInstanceBody(writer, instance, format, WireConstants.ROOT_INSTANCE);
            writer.endObject(null);
        });
    }

    public String encode(Application application, WireFormat format) {
        return write(format, writer -> {
            writer.beginObject(null);
            writeApplication(writer, application, format, WireConstants.ROOT_APPLICATION);
            writer.endObject(null);
        });
    }

    public String encode(Applications applications, WireFormat format) {
        return write(format, writer -> {
            writer.beginObject(null);
            writeApplications(writer, applications, format);
            writer.endObject(null);
        });
    }

    /**
     * Writes an instance's fields into a generator the caller owns, without the {@code instance} root wrapper.
     * <p>
     * Exists for {@link RegistryJacksonModule}: it lets a {@code ServiceInstance} appearing inside some other
     * response body be encoded in the registry's own format, in the correct field order, rather than by Jackson
     * bean introspection.
     */
    public void writeInstanceFields(com.fasterxml.jackson.core.JsonGenerator generator, ServiceInstance instance)
        throws IOException {

        WireWriter writer = new WireWriter.Json(generator);
        writeInstanceFields(writer, instance, WireFormat.JSON_FULL);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Peer replication
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Encodes an outbound replication batch.
     * <p>
     * JSON only - the replication client has always used the full JSON codec, so there is no XML form of this to
     * stay compatible with. The nested instance uses exactly the same encoder as a registry response, which is the
     * subtlety worth knowing: serialising these DTOs with a plain Jackson mapper looks like it works but drops the
     * port wrapper, and a peer would then register the instance with no usable port.
     */
    public String encode(ReplicationBatch batch) {
        return write(WireFormat.JSON_FULL, writer -> {
            writer.beginObject(null);
            writer.beginArray("replicationList");
            for (ReplicationItem item : batch.items()) {
                writer.beginArrayElement("replicationList");
                writeReplicationItem(writer, item);
                writer.endArrayElement("replicationList");
            }
            writer.endArray();
            writer.endObject(null);
        });
    }

    private void writeReplicationItem(WireWriter writer, ReplicationItem item) throws IOException {
        writer.optionalString("appName", item.appName());
        writer.optionalString("id", item.id());
        if (item.lastDirtyTimestamp() != null) {
            writer.number("lastDirtyTimestamp", item.lastDirtyTimestamp());
        }
        writer.optionalString("overriddenStatus", item.overriddenStatus());
        writer.optionalString("status", item.status());
        // Absent rather than null for everything except a registration - the codec omits nulls, and a peer
        // distinguishes "no body" from "empty body".
        if (item.instance() != null) {
            writer.beginObject("instanceInfo");
            writeInstanceFields(writer, item.instance(), WireFormat.JSON_FULL);
            writer.endObject("instanceInfo");
        }
        writer.optionalString("action", item.action() == null ? null : item.action().name());
    }

    public String encode(ReplicationResponse response) {
        return write(WireFormat.JSON_FULL, writer -> {
            writer.beginObject(null);
            writer.beginArray("responseList");
            for (ReplicationResponse.Item item : response.items()) {
                writer.beginArrayElement("responseList");
                writer.number("statusCode", item.statusCode());
                if (item.responseEntity() != null) {
                    writer.beginObject("responseEntity");
                    writeInstanceFields(writer, item.responseEntity(), WireFormat.JSON_FULL);
                    writer.endObject("responseEntity");
                }
                writer.endArrayElement("responseList");
            }
            writer.endArray();
            writer.endObject(null);
        });
    }

    public ReplicationBatch decodeReplicationBatch(String payload) {
        WireNode root = parse(payload);
        List<ReplicationItem> items = new ArrayList<>();
        for (WireNode node : root.all("replicationList")) {
            WireNode instance = node.child("instanceInfo");
            items.add(new ReplicationItem(
                ReplicationAction.fromWire(node.string("action")),
                node.string("appName"),
                node.string("id"),
                node.child("lastDirtyTimestamp") == null ? null : node.number(0L, "lastDirtyTimestamp"),
                node.string("status"),
                node.string("overriddenStatus"),
                instance == null ? null : WireMapper.toInstance(instance)
            ));
        }
        return new ReplicationBatch(items);
    }

    public ReplicationResponse decodeReplicationResponse(String payload) {
        WireNode root = parse(payload);
        List<ReplicationResponse.Item> items = new ArrayList<>();
        for (WireNode node : root.all("responseList")) {
            WireNode entity = node.child("responseEntity");
            items.add(new ReplicationResponse.Item(
                (int) node.number(0L, "statusCode"),
                entity == null ? null : WireMapper.toInstance(entity)
            ));
        }
        return new ReplicationResponse(items);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Decoding
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Reads a single instance, as sent by an enabler registering itself.
     * <p>
     * The syntax is detected from the payload rather than taken from a Content-Type header, because the header is
     * not reliable in practice - the Python enabler, for instance, labels its GETs {@code application/xml} while
     * its POST body is JSON.
     */
    public ServiceInstance decodeInstance(String payload) {
        WireNode root = parse(payload);
        WireNode instance = root.child(WireConstants.ROOT_INSTANCE);
        return WireMapper.toInstance(instance == null ? root : instance);
    }

    public Application decodeApplication(String payload) {
        WireNode root = parse(payload);
        WireNode application = root.child(WireConstants.ROOT_APPLICATION);
        return WireMapper.toApplication(application == null ? root : application);
    }

    public Applications decodeApplications(String payload) {
        WireNode root = parse(payload);
        WireNode applications = root.child(WireConstants.ROOT_APPLICATIONS);
        return WireMapper.toApplications(applications == null ? root : applications);
    }

    /** Sniffs the syntax: anything whose first non-whitespace character is '<' is XML, otherwise JSON. */
    private WireNode parse(String payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Cannot decode a null registry payload");
        }
        String trimmed = payload.stripLeading();
        try {
            return trimmed.startsWith("<") ? parsers.parseXml(payload) : parsers.parseJson(payload);
        } catch (IOException e) {
            throw new UncheckedIOException("Malformed registry payload", e);
        } catch (XMLStreamException e) {
            throw new IllegalArgumentException("Malformed XML registry payload", e);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Structure
    // ---------------------------------------------------------------------------------------------------------

    private void writeApplications(WireWriter writer, Applications applications, WireFormat format)
        throws IOException {

        writer.beginObject(WireConstants.ROOT_APPLICATIONS);

        List<Application> list = applications.applications();
        // JSON always emits the array, even when empty; XML simply omits the repeated element. Matches the
        // applications-empty fixtures.
        if (!list.isEmpty() || format.syntax() == WireFormat.Syntax.JSON) {
            writer.beginArray(WireConstants.ROOT_APPLICATION);
            for (Application application : list) {
                writer.beginArrayElement(WireConstants.ROOT_APPLICATION);
                writeApplicationBody(writer, application, format);
                writer.endArrayElement(WireConstants.ROOT_APPLICATION);
            }
            writer.endArray();
        }

        writer.bool(WireConstants.FIELD_REGISTERED_APPLICATIONS_EMPTY, applications.empty());
        // versions__delta and apps__hashcode are strings on the wire, including when the version is numeric.
        writer.optionalString(WireConstants.FIELD_VERSIONS_DELTA,
            applications.version() == null ? null : Long.toString(applications.version()));
        writer.optionalString(WireConstants.FIELD_APPS_HASHCODE,
            applications.appsHashCode() == null ? "" : applications.appsHashCode());

        writer.endObject(WireConstants.ROOT_APPLICATIONS);
    }

    private void writeApplication(WireWriter writer, Application application, WireFormat format, String elementName)
        throws IOException {

        writer.beginObject(elementName);
        writeApplicationBody(writer, application, format);
        writer.endObject(elementName);
    }

    private void writeApplicationBody(WireWriter writer, Application application, WireFormat format)
        throws IOException {

        writer.optionalString("name", application.name());
        writer.beginArray(WireConstants.ROOT_INSTANCE);
        for (ServiceInstance instance : application.instances()) {
            writer.beginArrayElement(WireConstants.ROOT_INSTANCE);
            writeInstanceFields(writer, instance, format);
            writer.endArrayElement(WireConstants.ROOT_INSTANCE);
        }
        writer.endArray();
    }

    private void writeInstanceBody(WireWriter writer, ServiceInstance instance, WireFormat format, String elementName)
        throws IOException {

        writer.beginObject(elementName);
        writeInstanceFields(writer, instance, format);
        writer.endObject(elementName);
    }

    /**
     * The instance field order, captured from Eureka. Do not reorder.
     * <p>
     * Compact mode drops the fields a load balancer does not consult - the lease, the override, the metadata and
     * the identity extras - keeping only what is needed to reach the instance. Which ones exactly is pinned by the
     * {@code *.mini.json} / {@code *.mini.xml} fixtures.
     */
    private void writeInstanceFields(WireWriter writer, ServiceInstance instance, WireFormat format)
        throws IOException {

        boolean full = !format.compact();

        writer.optionalString("instanceId", instance.instanceId());
        writer.optionalString("app", instance.appName());
        if (full) {
            // Compact drops appGroupName but keeps asgName further down - an asymmetry with no obvious rationale,
            // confirmed against the instance-all-fields.mini fixtures.
            writer.optionalString("appGroupName", instance.appGroupName());
        }
        writer.optionalString("ipAddr", instance.ipAddr());
        if (full) {
            writer.optionalString("sid", instance.sid());
        }
        if (full) {
            writer.optionalString("homePageUrl", instance.homePageUrl());
            writer.optionalString("statusPageUrl", instance.statusPageUrl());
            writer.optionalString("healthCheckUrl", instance.healthCheckUrl());
            writer.optionalString("secureHealthCheckUrl", instance.secureHealthCheckUrl());
        }
        writer.optionalString("vipAddress", instance.vipAddress());
        writer.optionalString("secureVipAddress", instance.secureVipAddress());
        if (full) {
            writer.number("countryId", instance.countryId());
        }
        writer.dataCenterInfo(instance.dataCenterInfo());
        writer.optionalString("hostName", instance.hostName());
        writer.optionalString("status", instance.status() == null ? null : instance.status().name());
        if (full) {
            writer.optionalString("overriddenStatus",
                instance.overriddenStatus() == null ? null : instance.overriddenStatus().name());
            writeLease(writer, instance);
            writer.bool("isCoordinatingDiscoveryServer", instance.coordinatingDiscoveryServer());
        }

        boolean metadataFirst = full && writer.metadataBeforeTimestamps();
        if (metadataFirst) {
            writer.metadata(instance.metadata());
        }

        writer.number("lastUpdatedTimestamp", instance.lastUpdatedTimestamp());
        if (full) {
            writer.number("lastDirtyTimestamp", instance.lastDirtyTimestamp());
        }
        writer.optionalString("actionType", instance.actionType() == null ? null : instance.actionType().name());
        writer.optionalString("asgName", instance.asgName());

        writer.port("port", instance.port());
        writer.port("securePort", instance.securePort());

        if (full && !metadataFirst) {
            writer.metadata(instance.metadata());
        }
    }

    private void writeLease(WireWriter writer, ServiceInstance instance) throws IOException {
        var lease = instance.lease();
        if (lease == null) {
            return;
        }
        writer.beginObject("leaseInfo");
        writer.number("renewalIntervalInSecs", lease.renewalIntervalSecs());
        writer.number("durationInSecs", lease.durationSecs());
        writer.number("registrationTimestamp", lease.registrationTimestamp());
        writer.number("lastRenewalTimestamp", lease.lastRenewalTimestamp());
        writer.number("evictionTimestamp", lease.evictionTimestamp());
        writer.number("serviceUpTimestamp", lease.serviceUpTimestamp());
        writer.endObject("leaseInfo");
    }

    // ---------------------------------------------------------------------------------------------------------
    // Plumbing
    // ---------------------------------------------------------------------------------------------------------

    private interface Body {
        void write(WireWriter writer) throws IOException;
    }

    private String write(WireFormat format, Body body) {
        try (WireWriter writer = newWriter(format)) {
            body.write(writer);
            return writer.result();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to encode registry payload as " + format, e);
        }
    }

    private WireWriter newWriter(WireFormat format) throws IOException {
        return format.syntax() == WireFormat.Syntax.JSON
            ? new WireWriter.Json(jsonFactory)
            : new WireWriter.Xml();
    }

}
