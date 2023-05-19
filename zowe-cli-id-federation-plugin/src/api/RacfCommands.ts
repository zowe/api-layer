/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import * as fs from "fs";
import {IIdentity} from "./CsvParser";
import {IHandlerResponseApi} from "@zowe/imperative/lib/cmd/src/doc/response/api/handler/IHandlerResponseApi";
import { Commands } from "./Commands";

export class RacfCommands extends Commands{

    constructor(
        registry: string,
        identities: IIdentity[],
        response: IHandlerResponseApi
    ) {
        const racfTemplate = fs.readFileSync(`${__dirname}/templates/racf.jcl`).toString();
        const racfRefreshCommand = fs.readFileSync(`${__dirname}/templates/racf_refresh.jcl`).toString();
        super(registry,identities,racfTemplate,racfRefreshCommand,response);
    }
}
