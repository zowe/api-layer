/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import { SELECT_ENABLER, TOGGLE_DISPLAY } from '../constants/wizard-constants';

export function wizardToggleDisplay() {
    return {
        type: TOGGLE_DISPLAY,
        payload: null,
    };
}

export function selectEnabler(enablerName) {
    return {
        type: SELECT_ENABLER,
        payload: { enablerName },
    };
}
