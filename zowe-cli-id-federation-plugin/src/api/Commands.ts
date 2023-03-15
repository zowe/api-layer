/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import {ImperativeError, TextUtils} from "@zowe/imperative";
import { warn } from "console";
import * as fs from "fs";
import {IIdentity} from "./CsvParser";
import {hasValidLength} from "./ValidateUtil";
import {Constants} from "./Constants";
import {IHandlerResponseApi} from "@zowe/imperative/lib/cmd/src/doc/response/api/handler/IHandlerResponseApi";

export class Commands {

    readonly maxLengthMainframeId = 8;
    readonly maxLengthDistributedId = 246;
    readonly maxLengthLabel = 32;

    constructor(
        private registry: string,
        private identities: IIdentity[],
        private esm: string,
        private response: IHandlerResponseApi
    ) {
    }

    getCommands(): string[] {
        const commandTemplate = fs.readFileSync(`src/api/templates/${this.esm.toLowerCase()}.jcl`).toString();
        const refreshCommand = fs.readFileSync(`src/api/templates/${this.esm.toLowerCase()}_refresh.jcl`).toString();

        const commands = this.identities
            .map(identity => this.getCommand(identity, commandTemplate))
            .filter(command => command);

        if (!commands.some(Boolean)) {
            this.response.data.setExitCode(Constants.fatalCode);
            throw new ImperativeError({msg: "Error when trying to create the identity mapping."});
        }
        commands.push("");
        commands.push(refreshCommand);
        return commands;
    }

    private getCommand(identity: IIdentity, commandTemplate: string): string {
        if(!hasValidLength(identity.mainframeId, this.maxLengthMainframeId)) {
            warn(`The mainframe user ID '${identity.mainframeId}' has exceeded maximum length of ${this.maxLengthMainframeId} characters. ` +
           `Identity mapping for the user '${identity.userName}' has not been created.`);
            this.response.data.setExitCode(Constants.warnCode);
            return '';
        }

        if(!hasValidLength(identity.distributedId, this.maxLengthDistributedId)) {
            warn(`The distributed user ID '${identity.distributedId}' has exceeded maximum length of ${this.maxLengthDistributedId} characters. ` +
                `Identity mapping for the user '${identity.userName}' has not been created.`);
            this.response.data.setExitCode(Constants.warnCode);
            return '';
        }

        if(!hasValidLength(identity.userName, this.maxLengthLabel)) {
            warn(`The user name '${identity.userName}' has exceeded maximum length of ${this.maxLengthLabel} characters. ` +
                `Identity mapping for the user '${identity.userName}' has not been created.`);
            this.response.data.setExitCode(Constants.warnCode);
            return '';
        }

        return TextUtils.renderWithMustache(commandTemplate, {
            mainframe_id: identity.mainframeId.trim(),
            distributed_id: identity.distributedId.trim(),
            registry: this.registry,
            user_name: identity.userName.trim(),
            escape: function() {
                return function(text: string, render: any) {
                    return render(text).replace(/'/g, "''");
                };
            }
        });
    }

}
