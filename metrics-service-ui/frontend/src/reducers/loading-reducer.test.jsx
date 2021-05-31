/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

/* eslint-disable no-undef */

import loadingReducer from './loading-reducer';

describe('>>> Loading reducer tests', () => {
    it('should return current state if not a *_REQUEST', () => {
        const state = { payload: 'payload' };
        const action = { type: 'FETCH_TILES_SUCCESS' };
        const expectedState = { FETCH_TILES: false, payload: 'payload' };
        expect(loadingReducer(state, action)).toEqual(expectedState);
    });

    it('should return isRequest if state if state is a *_REQUEST', () => {
        const state = { payload: 'payload' };
        const expectedState = { FETCH_TILES: true, payload: 'payload' };
        const action = { type: 'FETCH_TILES_REQUEST' };

        expect(loadingReducer(state, action)).toEqual(expectedState);
    });
});
