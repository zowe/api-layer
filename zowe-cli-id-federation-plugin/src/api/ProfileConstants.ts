/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import {ICommandOptionDefinition} from "@zowe/imperative";
import {Constants} from "./Constants";

export class ProfileConstants {
    public static IDF_CONNECTION_OPTION_GROUP = "IDF Connection Options";

    public static IDF_OPTION_ESM: ICommandOptionDefinition = {
        name: "esm",
        aliases: ["e"],
        description: "The ESM product on the target system",
        required: true,
        type: "string",
        allowableValues: {values: ["RACF", "TSS", "ACF2"]},
        group: ProfileConstants.IDF_CONNECTION_OPTION_GROUP
    };

    public static IDF_OPTION_SYSTEM: ICommandOptionDefinition = {
        name: "system",
        aliases: ["s"],
        description: "The target JES system on which the command will be executed",
        stringLengthRange: [1, Constants.MAX_LENGTH_SYSTEM],
        type: "string",
        group: ProfileConstants.IDF_CONNECTION_OPTION_GROUP
    };

    public static IDF_OPTION_REGISTRY: ICommandOptionDefinition = {
        name: "registry",
        aliases: ["r"],
        description: "The distributed identities registry (e.g., ldaps://enterprise.com, ldap://12.34.56.78:389)",
        required: true,
        type: "string",
        stringLengthRange: [1, Constants.MAX_LENGTH_REGISTRY],
        group: ProfileConstants.IDF_CONNECTION_OPTION_GROUP
    };

    public static IDF_CONNECTION_OPTIONS: ICommandOptionDefinition[] = [
        ProfileConstants.IDF_OPTION_ESM,
        ProfileConstants.IDF_OPTION_SYSTEM,
        ProfileConstants.IDF_OPTION_REGISTRY
    ];
}
