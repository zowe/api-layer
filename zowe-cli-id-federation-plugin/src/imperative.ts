/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import {IImperativeConfig} from "@zowe/imperative";
import { ProfileConstants } from "./api/ProfileConstants";

const config: IImperativeConfig = {
    commandModuleGlobs: ["**/cli/*/*.definition!(.d).*s"],
    pluginHealthCheck: __dirname + "/healthCheck.Handler",
    pluginSummary: "Zowe CLI Identity Federation plug-in",
    pluginAliases: ["idf"],
    rootCommandDescription: "Welcome to the Identity Federation plug-in for Zowe CLI!\n\n The Identity Federation plug-in " +
        "(& CLI) follows the Zowe CLI command syntax 'zowe [action] [object] [options]'. ",
    productDisplayName: "Zowe CLI Identity Federation Plug-in",
    name: "id-federation",
    envVariablePrefix: "IDF",
    logging: {
        appLogging: {
            level: "warn"
        }
    },
    profiles: [
        {
            type: "id-federation",
            schema: {
                type: "object",
                title: "Identity Federation Profile",
                description: "A Identity Federation profile is optional.",
                properties: {
                    esm: {
                        type: "string",
                        optionDefinition: ProfileConstants.IDF_OPTION_ESM
                    },
                    lpar: {
                        type: "string",
                        optionDefinition: ProfileConstants.IDF_OPTION_LPAR
                    },
                    registry: {
                        type: "string",
                        optionDefinition: ProfileConstants.IDF_OPTION_REGISTRY
                    }
                }
            }
        }
    ]
};

export = config;
