/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

const { TextDecoder, TextEncoder } = require('node:util');
const { performance } = require("node:perf_hooks");
const { ReadableStream, TransformStream } = require('node:stream/web');
const { clearImmediate, markResourceTiming } = require('node:timers');

Object.defineProperties(globalThis, {
    TextDecoder: { value: TextDecoder },
    TextEncoder: { value: TextEncoder },
    ReadableStream: { value: ReadableStream },
    TransformStream: { value: TransformStream },
    performance: { value: performance },
    clearImmediate: { value: clearImmediate },
    markResourceTiming: { value: markResourceTiming },
});

const { fetch, Headers, FormData, Request, Response } = require('undici');

Object.defineProperties(globalThis, {
    fetch: { value: fetch, writable: true },
    Headers: { value: Headers },
    FormData: { value: FormData },
    Request: { value: Request },
    Response: { value: Response },
});
