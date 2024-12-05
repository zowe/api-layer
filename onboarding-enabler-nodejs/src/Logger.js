/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Jacob Quatier
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/* eslint-disable no-underscore-dangle */
const LEVELS = {
  error: 50,
  warn: 40,
  info: 30,
  debug: 20,
};
const DEFAULT_LEVEL = LEVELS.info;

export default class Logger {
  constructor() {
    this._level = DEFAULT_LEVEL;
  }

  level(inVal) {
    let val = inVal;
    if (val) {
      if (typeof val === 'string') {
        val = LEVELS[val];
      }
      this._level = val || DEFAULT_LEVEL;
    }
    return this._level;
  }

  // Abstract the console call:
  _log(method, args) {
    if (this._level <= LEVELS[method === 'log' ? 'debug' : method]) {
      /* eslint-disable no-console */
      console[method](...args);
      /* eslint-enable no-console */
    }
  }

  error(...args) {
    return this._log('error', args);
  }
  warn(...args) {
    return this._log('warn', args);
  }
  info(...args) {
    return this._log('info', args);
  }
  debug(...args) {
    return this._log('log', args);
  }
}
