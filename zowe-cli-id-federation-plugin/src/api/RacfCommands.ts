/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import {TextUtils} from "@zowe/imperative";
import { warn } from "console";
import * as fs from "fs";
import {IIdentity} from "./CsvParser";
import {hasValidLength} from "./ValidateUtil";

export class RacfCommands {

    readonly maxLengthMainframeId = 8;
    readonly maxLengthDistributedId = 246;
    readonly maxLengthLabel = 32;

    constructor(
        private registry: string,
        private identities: IIdentity[]
    ) {
    }

    getCommands(): string {
        const racfTemplate = fs.readFileSync('src/api/templates/racf.jcl').toString();
        const racfRefreshCommand = fs.readFileSync('src/api/templates/racf_refresh.jcl').toString();

        let racfCommands = '';
        this.identities.forEach(identity => racfCommands += this.getCommand(identity, racfTemplate));

        return racfCommands + racfRefreshCommand.trimEnd();
    }

    private getCommand(identity: IIdentity, racfTemplate: string): string {
        if(!hasValidLength(identity.mainframeId, this.maxLengthMainframeId)) {
            warn(`The mainframe user ID '${identity.mainframeId}' has exceeded maximum length of ${this.maxLengthMainframeId} characters. ` +
           `Identity mapping for the user '${identity.userName}' is not be created.`);
            return '';
        }

        if(!hasValidLength(identity.distributedId, this.maxLengthDistributedId)) {
            warn(`The distributed user ID '${identity.distributedId}' has exceeded maximum length of ${this.maxLengthDistributedId} characters. ` +
                `Identity mapping for the user '${identity.userName}' is not be created.`);
            return '';
        }

        if(!hasValidLength(identity.userName, this.maxLengthLabel)) {
            warn(`The user name '${identity.userName}' has exceeded maximum length of ${this.maxLengthLabel} characters. ` +
                `Identity mapping for the user '${identity.userName}' is not be created.`);
            return '';
        }

        return TextUtils.renderWithMustache(racfTemplate, {
            mainframe_id: identity.mainframeId.trim(),
            distributed_id: identity.distributedId.trim(),
            registry: this.registry,
            user_name: identity.userName.trim()
        });
    }

}
