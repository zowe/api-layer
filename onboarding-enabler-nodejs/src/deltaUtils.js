/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

/*
  General utilities for handling processing of delta changes from eureka.
*/
export function arrayOrObj(mysteryValue) {
  return Array.isArray(mysteryValue) ? mysteryValue : [mysteryValue];
}

export function findInstance(a) {
  return b => a.hostName === b.hostName && a.port.$ === b.port.$;
}

export function normalizeDelta(appDelta) {
  return arrayOrObj(appDelta).map((app) => {
    app.instance = arrayOrObj(app.instance);
    return app;
  });
}
