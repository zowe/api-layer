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
import * as fs from "fs";
import {IIdentities} from "./CsvParser";

export class RacfCommands {

    constructor(
        private registry: string,
        private identities: IIdentities[]
    ) {
    }

    getCommands(): string {
        const racfTemplate = fs.readFileSync('src/api/templates/racf.jcl').toString();

        let racfCommands = '';
        this.identities.forEach(identity => racfCommands +=
            TextUtils.renderWithMustache(racfTemplate, {
                mainframe_id: identity.mainframeId.trim(),
                distributed_id: identity.distributedId.trim(),
                registry: this.registry,
                user_name: identity.userName.trim()
            })
        );

        return racfCommands.trimRight();
    }

}
