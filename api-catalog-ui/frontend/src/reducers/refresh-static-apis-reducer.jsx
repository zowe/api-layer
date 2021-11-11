/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import {
    REFRESH_STATIC_APIS_SUCCESS,
    REFRESH_STATIC_APIS_ERROR,
    CLEAR_ERROR,
} from '../constants/refresh-static-apis-constants';

const staticApisDefaultState = {
    refreshTimestamp: null,
    error: null,
};

const refreshStaticApisReducer = (state = staticApisDefaultState, action = {}) => {
    switch (action.type) {
        case REFRESH_STATIC_APIS_SUCCESS:
            return {
                ...state,
                refreshTimestamp: action.refreshTimestamp,
                error: null,
            };
        case REFRESH_STATIC_APIS_ERROR:
            return {
                error: action.error,
            };
        case CLEAR_ERROR:
            return { error: null };
        default:
            return state;
    }
};

export default refreshStaticApisReducer;
