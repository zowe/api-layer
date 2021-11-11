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
import { filterText, clear } from './filter-actions';

describe('>>> Selected Service actions tests', () => {
    it('should create actions to clear text', () => {
        const expectedAction = {
            type: CLEAR_FILTER,
            defaultFilter: '',
        };

        expect(clear()).toEqual(expectedAction);
    });

    it('should create actions to filter text', () => {
        const expectedText = 'sampleText';
        const expectedAction = {
            type: FILTER_TEXT,
            text: expectedText,
        };

        expect(filterText(expectedText)).toEqual(expectedAction);
    });
});
