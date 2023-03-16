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
import {parse} from "csv-parse/sync";
import {ImperativeError} from "@zowe/imperative";
import {IHandlerResponseApi} from "@zowe/imperative/lib/cmd/src/doc/response/api/handler/IHandlerResponseApi";
import {Constants} from "./Constants";

export interface IIdentity {
    userName: string;
    distributedId: string;
    mainframeId: string;
}

export class CsvParser {

    constructor(
        public file: string,
        public response: IHandlerResponseApi) {
    }

    getIdentities(): IIdentity[] {
        let fileContent;
        try {
            fileContent = fs.readFileSync(this.file);
        } catch (error) {
            this.response.data.setExitCode(Constants.FATAL_CODE);
            throw new ImperativeError({msg: `Cannot open the input CSV file. ${error.message}`});
        }

        try {
            return parse(fileContent, {columns: Constants.HEADERS}) as IIdentity[];
        } catch (e) {
            this.response.data.setExitCode(Constants.FATAL_CODE);
            throw new ImperativeError({msg: `Invalid CSV format: ${e.message ?? ''}`});
        }
    }

}
