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
import serviceVersionDiffReducer from './service-version-diff-reducer';

describe('>>> Service Version Diff reducer tests', () => {
    it('Should return requested diff versions', () => {
        const state = { diffText: undefined, oldVersion: undefined, newVersion: undefined };
        const expectedState = { diffText: undefined, oldVersion: 'v1', newVersion: 'v2' };
        const action = { type: REQUEST_VERSION_DIFF, oldVersion: 'v1', newVersion: 'v2' };
        expect(serviceVersionDiffReducer(state, action)).toEqual(expectedState);
    });

    it('Should return received diff', () => {
        const state = { diffText: undefined, oldVersion: 'v1', newVersion: 'v2' };
        const expectedState = { diffText: '<html>diff</html>', oldVersion: 'v1', newVersion: 'v2' };
        const action = { type: RECEIVE_VERSION_DIFF, diffText: '<html>diff</html>' };
        expect(serviceVersionDiffReducer(state, action)).toEqual(expectedState);
    });
});
