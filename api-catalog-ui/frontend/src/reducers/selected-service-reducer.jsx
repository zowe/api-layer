/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import { CLEAR_SERVICE, SELECT_SERVICE, STORE_CONTENT_ANCHOR } from '../constants/selected-service-constants';

const defaultState = {
    selectedService: {},
    selectedTile: null,
    selectedContentAnchor: null,
};

const selectedServiceReducer = (state = defaultState, action = {}) => {
    switch (action.type) {
        case SELECT_SERVICE:
            return {
                ...state,
                selectedService: action.selectedService,
                selectedTile: action.selectedTile,
                selectedContentAnchor: state.selectedContentAnchor,
            };
        case CLEAR_SERVICE:
            return {
                ...state,
                selectedService: {},
                selectedTile: null,
                selectedContentAnchor: null,
            };
        case STORE_CONTENT_ANCHOR:
            return {
                ...state,
                selectedContentAnchor: action.payload,
            };
        default:
            return state;
    }
};

export default selectedServiceReducer;
