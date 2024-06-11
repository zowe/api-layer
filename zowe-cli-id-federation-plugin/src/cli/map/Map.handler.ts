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
import * as fs from "fs";
import {Constants} from "../../api/Constants";

export default class MapHandler implements ICommandHandler {

    public async process(params: IHandlerParameters): Promise<void> {
        const file: string = params.arguments.inputFile;
        const system: string = params.arguments.system ?? '';

        if (!fs.existsSync(file)) {
            const msg = `The input CSV file does not exist.`;
            params.response.data.setExitCode(Constants.FATAL_CODE);
            throw new ImperativeError({msg});
        }

        const mapper = new Mapper(file, params.arguments.esm, system, params.arguments.registry, params.response);
        params.response.console.log(await mapper.map());
    }

}
