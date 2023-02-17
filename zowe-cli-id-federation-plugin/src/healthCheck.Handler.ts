/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import {ICommandHandler, IHandlerParameters, Logger} from "@zowe/imperative";

export default class HealthCheckHandler implements ICommandHandler {
    public async process(params: IHandlerParameters): Promise<void> {
        Logger.getImperativeLogger().debug("Invoked health check handler");
        params.response.console.log("You would report problems identified by healthCheck.\n" +
            "This handler is not currently called by Imperative, but\n" +
            "its existence prevents a warning during plugin validation."
        );
    }
}
