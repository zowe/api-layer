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

import { ENABLER_CHANGED, SELECT_ENABLER, TOGGLE_DISPLAY } from '../constants/wizard-constants';
import { data } from '../components/Wizard/wizard_config';
import wizardReducer, { wizardReducerDefaultState } from './wizard-reducer';

describe('>>> Wizard reducer tests', () => {
    it('should return default state in the default action', () => {
        expect(wizardReducer()).toEqual(wizardReducerDefaultState);
    });

    it('should handle TOGGLE_DISPLAY true -> false', () => {
        const expectedState = {
            wizardIsOpen: true,
        };

        expect(wizardReducer({ wizardIsOpen: false }, { type: TOGGLE_DISPLAY, payload: null })).toEqual(expectedState);
    });

    it('should handle TOGGLE_DISPLAY false -> true', () => {
        const expectedState = {
            wizardIsOpen: false,
        };

        expect(wizardReducer({ wizardIsOpen: true }, { type: TOGGLE_DISPLAY, payload: null })).toEqual(expectedState);
    });

    it('should handle DEFAULT', () => {
        const expectedState = {
            wizardIsOpen: true,
        };

        expect(wizardReducer({ wizardIsOpen: true }, { type: 'UNKNOWN', payload: null })).toEqual(expectedState);
    });

    it('should handle null action', () => {
        const expectedState = {
            wizardIsOpen: true,
        };

        expect(wizardReducer({ wizardIsOpen: true }, null)).toEqual(expectedState);
    });

    it('should handle SELECT_ENABLER', () => {
        const expectedState = {
            inputData: data,
            enablerChanged: true,
            enablerName: 'Plain Java Enabler',
        };

        expect(wizardReducer({ inputData: [], enablerChanged: false }, {
            type: SELECT_ENABLER,
            payload: { enablerName: 'Plain Java Enabler' },
        })).toEqual(expectedState);
    });

    it('should handle default state in SELECT_ENABLER', () => {
        const expectedState = {
            inputData: [],
            enablerChanged: true,
            enablerName: 'Non-existent Enabler',
        };

        expect(wizardReducer({ inputData: [], enablerChanged: false }, {
            type: SELECT_ENABLER,
            payload: { enablerName: 'Non-existent Enabler' },
        })).toEqual(expectedState);
    });

    it('should handle ENABLER_CHANGED', () => {
        const expectedState = {
            enablerChanged: false,
        };

        expect(wizardReducer({ enablerChanged: true }, {
            type: ENABLER_CHANGED,
            payload: null
        })).toEqual(expectedState);
    });
});
