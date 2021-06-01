/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import { REQUEST_VERSION_DIFF, RECEIVE_VERSION_DIFF } from '../actions/service-version-diff-actions';

const defaultState = {
    diffText: undefined,
    oldVersion: undefined,
    newVersion: undefined,
};

const serviceVersionDiffReducer = (state = defaultState, action = {}) => {
    switch (action.type) {
        case REQUEST_VERSION_DIFF:
            return {
                ...state,
                oldVersion: action.oldVersion,
                newVersion: action.newVersion,
            };
        case RECEIVE_VERSION_DIFF:
            return {
                ...state,
                diffText: action.diffText,
            };
        default:
            return state;
    }
};

export default serviceVersionDiffReducer;
