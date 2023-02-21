/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import {ICommandHandler, IHandlerParameters, ImperativeError} from "@zowe/imperative";
import {Mapper} from "../../api/Mapper";
import {hasValidLength} from "../../api/ValidateUtil";
import * as fs from "fs";

export default class MapHandler implements ICommandHandler {

    //TODO: length valid for RACF, if other ESMs allow the different length -> refactor
    readonly maxLengthRegistry = 255;
    //TODO: Allow running on multiple possible systems? (ex. SYS1,SYS2)
    readonly maxLengthLpar = 8;

    public async process(params: IHandlerParameters): Promise<void> {
        const file: string = params.arguments.inputFile;
        const esm: string = params.arguments.esm;
        const lpar: string = params.arguments.lpar;
        const registry: string = params.arguments.registry;

        const missingArgs: string[] = [];
        if (!esm) {
            missingArgs.push('esm');
        }
        if (!lpar) {
            missingArgs.push('lpar');
        }
        if (!registry) {
            missingArgs.push('registry');
        }
        if (missingArgs.length != 0) {
            const msg = `Following arguments are missing: "${missingArgs.join(", ")}"`;
            throw new ImperativeError({msg});
        }

        if(!fs.existsSync(file)) {
            const msg = `The input CSV file does not exist.`;
            throw new ImperativeError({msg});
        }

        if(!hasValidLength(lpar, this.maxLengthLpar )) {
            const msg = `The registry '${lpar}' has exceeded maximum length of ${this.maxLengthLpar} characters.`;
            throw new ImperativeError({msg});
        }

        if(!hasValidLength(registry, this.maxLengthRegistry )) {
            const msg = `The registry '${registry}' has exceeded maximum length of ${this.maxLengthRegistry} characters.`;
            throw new ImperativeError({msg});
        }

        const mapper = new Mapper(file, esm, lpar, registry);
        params.response.console.log(await mapper.map());
    }

}
