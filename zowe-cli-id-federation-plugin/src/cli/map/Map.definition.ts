/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import { ICommandDefinition } from "@zowe/imperative";
import {ProfileConstants} from "../../api/ProfileConstants";
/**
 * Command one [object] definition. This definition is of imperative type "command" and therefore must have a
 * command handler (which performs the "work" for this command).
 *
 * In this case, "error-messages" will simply print console error (stderr) messages.
 *
 * Property Summary:
 * =================
 * "name" of the [object]. Should be a noun (e.g. data-set)
 * "aliases" normally contains a shortened form of the command
 * "summary" will display when issuing the help on this [objects] [action]
 * "type" is "command" which means a handler is required
 * "handler" is the file path to the handler (does the work)
 * "options" an array of options
 */
const MapDefinition: ICommandDefinition = {
    name: "map",
    summary: "Generate a mapping association between a mainframe user ID and a distributed user identity",
    description: "Generate a JCL based on [inputFile] that creates a mapping association between a mainframe user ID and a distributed user identity",
    type: "command",
    handler: __dirname + "/Map.handler",
    positionals: [
        {
            name: "input-file",
            description: "File with the list of users",
            type: "string",
            required: true
        }
    ],
    profile: {
        optional: ["id-federation"]
    },
    options: [...ProfileConstants.IDF_CONNECTION_OPTIONS]
};

export = MapDefinition;
