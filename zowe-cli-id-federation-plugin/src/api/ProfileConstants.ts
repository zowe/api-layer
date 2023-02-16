/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import { ICommandOptionDefinition } from "@zowe/imperative";

export class ProfileConstants {
    public static IDF_CONNECTION_OPTION_GROUP = "IDF Connection Options";

    public static IDF_OPTION_ESM: ICommandOptionDefinition = {
        name: "esm",
        aliases: ["e"],
        description: "The ESM to execute command",
        type: "string",
        allowableValues: { values: ["RACF", "TSS", "ACF2"] },
        group: ProfileConstants.IDF_CONNECTION_OPTION_GROUP
    };

    public static IDF_OPTION_LPAR: ICommandOptionDefinition = {
        name: "lpar",
        aliases: ["l"],
        description: "The security domain on which command will be executed",
        type: "string",
        group: ProfileConstants.IDF_CONNECTION_OPTION_GROUP
    };

    public static IDF_OPTION_REGISTRY: ICommandOptionDefinition = {
        name: "registry",
        aliases: ["r"],
        description: "The registry that contains the distributed-identity user name",
        type: "string",
        group: ProfileConstants.IDF_CONNECTION_OPTION_GROUP
    };

    public static IDF_CONNECTION_OPTIONS: ICommandOptionDefinition[] = [
        ProfileConstants.IDF_OPTION_ESM,
        ProfileConstants.IDF_OPTION_LPAR,
        ProfileConstants.IDF_OPTION_REGISTRY
    ];
}
