/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

/**
 * Use the index.ts to export any public facing APIs/intefaces/etc.
 *
 * If your plugin introduces a set of APIs/functions that others would find useful when building node apps
 * (or editor extensions, etc.) export them here.
 *
 * For example, Zowe CLI offers Jobs APIs that can be invoke programmatically from a VS code extension to create
 * a Job viewer/tree extension.
 */
export * from "./api/Mapper";
