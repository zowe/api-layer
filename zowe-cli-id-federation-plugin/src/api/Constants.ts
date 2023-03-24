/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

export class Constants {

    // Parameters maximum length
    public static readonly MAX_LENGTH_REGISTRY = 255;
    public static readonly MAX_LENGTH_SYSTEM = 8;

    // Plugin return codes
    public static readonly OKAY_CODE = 0;
    public static readonly ZOWE_ERROR_CODE = 1;
    public static readonly WARN_CODE = 4;
    public static readonly FATAL_CODE = 16;

    // CSV
    public static readonly HEADERS = ['userName', 'distributedId', 'mainframeId'];

}
