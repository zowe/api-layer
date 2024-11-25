/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Copyright Contributors to the Zowe Project.
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
