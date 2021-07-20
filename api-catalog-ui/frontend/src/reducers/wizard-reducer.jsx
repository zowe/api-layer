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
import { data } from '../components/Wizard/wizard_config';

export const wizardReducerDefaultState = {
    wizardIsOpen: false,
    enablerName: 'Static Onboarding',
    inputData: data,
};

const wizardReducer = (state = wizardReducerDefaultState, action = {}) => {
    if (action == null) return state;
    switch (action.type) {
        case TOGGLE_DISPLAY:
            return {
                ...state,
                wizardIsOpen: !state.wizardIsOpen,
            };
        case SELECT_ENABLER:
            return {
                ...state,
                enablerName: action.payload.enablerName,
            };
        default:
            return state;
    }
};

export default wizardReducer;
