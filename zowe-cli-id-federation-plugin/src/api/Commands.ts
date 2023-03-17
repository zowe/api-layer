/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import {IHandlerResponseApi, ImperativeError, TextUtils} from "@zowe/imperative";
import { warn } from "console";
import {IIdentity} from "./CsvParser";
import {Constants} from "./Constants";
import {hasValidLength} from "./ValidateUtil";

export class Commands {

    readonly maxLengthMainframeId = 8;
    readonly maxLengthDistributedId = 246;
    readonly maxLengthLabel = 32;

    constructor(
        private registry: string,
        private identities: IIdentity[],
        private commandTemplate: string,
        private refreshCommand: string,
        private response: IHandlerResponseApi
    ) {
    }

    getCommands(): string[] {
        const commands = this.identities
            .map(identity => this.getCommand(identity))
            .filter(command => command);

        if (!commands.some(Boolean)) {
            this.response.data.setExitCode(Constants.FATAL_CODE);
            throw new ImperativeError({msg: "Error when trying to create the identity mapping."});
        }
        commands.push(this.refreshCommand);
        return commands;
    }

    private getCommand(identity: IIdentity): string {
        if(!hasValidLength(identity.mainframeId, this.maxLengthMainframeId)) {
            warn(`The mainframe user ID '${identity.mainframeId}' has exceeded maximum length of ${this.maxLengthMainframeId} characters. ` +
           `Identity mapping for the user '${identity.userName}' has not been created.`);
           this.response.data.setExitCode(Constants.WARN_CODE);
            return '';
        }

        if(!hasValidLength(identity.distributedId, this.maxLengthDistributedId)) {
            warn(`The distributed user ID '${identity.distributedId}' has exceeded maximum length of ${this.maxLengthDistributedId} characters. ` +
                `Identity mapping for the user '${identity.userName}' has not been created.`);
            this.response.data.setExitCode(Constants.WARN_CODE);
                return '';
        }

        if(!hasValidLength(identity.userName, this.maxLengthLabel)) {
            warn(`The user name '${identity.userName}' has exceeded maximum length of ${this.maxLengthLabel} characters. ` +
                `Identity mapping for the user '${identity.userName}' has not been created.`);
                this.response.data.setExitCode(Constants.WARN_CODE);
            return '';
        }

        return TextUtils.renderWithMustache(this.commandTemplate, {
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
