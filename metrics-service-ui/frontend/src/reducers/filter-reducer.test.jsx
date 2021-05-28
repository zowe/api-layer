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

import { CLEAR_FILTER, FILTER_TEXT } from '../constants/filter-constants';
import filtersReducer from './filtersReducer';

describe('>>> Filter reducer tests', () => {
    it('should handle CLEAR_FILTER', () => {
        const expectedState = {
            text: '',
        };

        expect(filtersReducer({ text: 'Test' }, { type: CLEAR_FILTER, defaultFilter: '' })).toEqual(expectedState);
    });

    it('should handle FILTER_TEXT', () => {
        const expectedState = {
            text: 'Test',
        };

        expect(filtersReducer({ text: 'blem' }, { type: FILTER_TEXT, text: 'Test' })).toEqual(expectedState);
    });

    it('should handle DEFAULT', () => {
        const expectedState = {
            text: 'Test',
        };

        expect(filtersReducer({ text: 'Test' }, { type: 'UNKNOWN', text: 'Test' })).toEqual(expectedState);
    });
});
