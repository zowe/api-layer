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

    // Plugin return codes
    public static readonly okayCode = 0;
    public static readonly zoweErrorCode = 1;
    public static readonly warnCode = 4;
    public static readonly fatalCode = 16;

    // CSV
    public static readonly headers = ['userName', 'distributedId', 'mainframeId'];

}
