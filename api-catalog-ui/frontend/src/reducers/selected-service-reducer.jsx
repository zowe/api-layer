/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import { CLEAR_SERVICE, SELECT_SERVICE } from '../constants/selected-service-constants';

const defaultState = {
    selectedService: {},
    selectedTile: null,
};

const selectedServiceReducer = (state = defaultState, action = {}) => {
    switch (action.type) {
        case SELECT_SERVICE:
            return {
                ...state,
                selectedService: action.selectedService,
                selectedTile: action.selectedTile,
            };
        case CLEAR_SERVICE:
            return {
                ...state,
                selectedService: {},
                selectedTile: null,
            };
        default:
            return state;
    }
};

export default selectedServiceReducer;
