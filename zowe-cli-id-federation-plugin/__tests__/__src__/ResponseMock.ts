/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import {IHandlerResponseApi} from "@zowe/imperative/lib/cmd/src/doc/response/api/handler/IHandlerResponseApi";
import {
    IHandlerFormatOutputApi,
    IHandlerProgressApi,
    IHandlerResponseConsoleApi,
    IHandlerResponseDataApi
} from "@zowe/imperative";

export class ResponseMock implements IHandlerResponseApi {
    console: IHandlerResponseConsoleApi;
    data: IHandlerResponseDataApi;
    format: IHandlerFormatOutputApi;
    progress: IHandlerProgressApi;

    exitCode = 0;

    constructor() {
        //const t = this;
        const setExitCode = (code: number) => {
            this.exitCode = code;
            return code;
        };
        this.data = {
            setExitCode: setExitCode,
            setObj: (data: any, merge?: boolean) => undefined,
            setMessage: (message: string, ...values: any[]) => 'not implemented'
        };
    }
}
