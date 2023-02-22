/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import {Logger, LoggingConfigurer} from "@zowe/imperative";
import config = require("../imperative");

export class Mapper {
    file: string;
    esm: string;
    lpar: string;
    registry: string;

    constructor(file: string, esm: string, lpar: string, registry: string) {
        Logger.initLogger(LoggingConfigurer.configureLogger(".zowe", config));
        this.file = file;
        this.esm = esm;
        this.lpar = lpar;
        this.registry = registry;
    }

    map() {
        Logger.getAppLogger().info("AppLogger: TBD");//Here will be the code which generate JCL
    }
}
