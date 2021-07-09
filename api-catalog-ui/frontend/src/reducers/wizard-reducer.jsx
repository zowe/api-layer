/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import { TOGGLE_DISPLAY } from '../constants/wizard-constants';

const wizardReducerDefaultState = {
    wizardIsOpen: false,
};

const wizardReducer = (state = wizardReducerDefaultState, action = {}) => {
    if(action == null) return state;
    switch (action.type) {
        case TOGGLE_DISPLAY:
            return {
                ...state,
                wizardIsOpen: !state.wizardIsOpen,
            };
        default:
            return state;
    }
};

export default wizardReducer;
