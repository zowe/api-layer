/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import selectedServiceReducer from './selected-service-reducer';
import { STORE_CONTENT_ANCHOR } from '../constants/selected-service-constants';

describe('>>> Selected Service reducer tests', () => {
    it('should return selected service', () => {
        const state = { selectedService: { id: 'one' }, selectedTile: 'aaaa' };
        const expectedState = { selectedService: { id: 'one' }, selectedTile: 'aaaa' };
        const action = { type: 'SELECT_SERVICE', selectedService: { id: 'one' }, selectedTile: 'aaaa' };
        expect(selectedServiceReducer(state, action)).toEqual(expectedState);
    });

    it('should return clear service', () => {
        const state = { selectedService: { id: 'one' }, selectedTile: 'aaaa' };
        const expectedState = { selectedContentAnchor: null, selectedService: {}, selectedTile: null };
        const action = { type: 'CLEAR_SERVICE' };
        expect(selectedServiceReducer(state, action)).toEqual(expectedState);
    });

    it('should return default state', () => {
        const state = { selectedService: { id: 'one' }, selectedTile: 'aaaa' };
        const action = { type: 'OOPS' };
        expect(selectedServiceReducer(state, action)).toEqual(state);
    });

    it('should return store content anchor', () => {
        const state = { selectedContentAnchor: '#id' };
        const action = { type: STORE_CONTENT_ANCHOR, payload: '#id' };
        expect(selectedServiceReducer(state, action)).toEqual(state);
    });
});
