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

export default class MapHandler implements ICommandHandler {
    public async process(params: IHandlerParameters): Promise<void> {
        const file: string = params.arguments.inputFile;
        const esm: string = params.arguments.esm;
        const lpar: string = params.arguments.lpar;
        const registry: string = params.arguments.registry;
        params.response.console.log(`Input file: ${file}\nESM: ${esm}\nLPAR: ${lpar}\nRegistry: ${registry}`);

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
            const msg: string = `Following arguments are missing: "${missingArgs.join(", ")}"`;
            throw new ImperativeError({msg});
        }

        new Mapper(file, esm, lpar, registry).map();
    }
}
