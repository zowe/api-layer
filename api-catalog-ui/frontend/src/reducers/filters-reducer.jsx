/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import { CLEAR_FILTER, FILTER_TEXT } from '../constants/filter-constants';

const filtersReducerDefaultState = {
    text: '',
};

const filtersReducer = (state = filtersReducerDefaultState, action = {}) => {
    switch (action.type) {
        case FILTER_TEXT:
            return {
                ...state,
                text: action.text,
            };
        case CLEAR_FILTER:
            return {
                ...state,
                text: '',
            };
        default:
            return state;
    }
};

export default filtersReducer;
