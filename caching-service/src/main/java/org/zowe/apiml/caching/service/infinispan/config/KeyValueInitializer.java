/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.caching.service.infinispan.config;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.zowe.apiml.caching.model.KeyValue;

@AutoProtoSchemaBuilder(includeClasses = {KeyValue.class},
    schemaFileName = "keyvalue.proto",
    schemaFilePath = "proto/",
    schemaPackageName = "proto_map")
public interface KeyValueInitializer extends GeneratedSchema {
}
