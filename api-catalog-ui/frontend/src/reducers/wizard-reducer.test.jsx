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

import {
    CHANGE_CATEGORY,
    INPUT_UPDATED,
    NEXT_CATEGORY,
    SELECT_ENABLER,
    TOGGLE_DISPLAY
} from '../constants/wizard-constants';
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
        const expectedData = data.filter(o => {
            return !(o.text === 'API info' || o.text === 'Discovery Service URL');
        });
        const expectedState = {
            inputData: expectedData,
            enablerName: 'Plain Java Enabler',
        };

        expect(wizardReducer({ inputData: [] }, {
            type: SELECT_ENABLER,
            payload: { enablerName: 'Plain Java Enabler' },
        })).toEqual(expectedState);
    });

    it('should handle default state in SELECT_ENABLER', () => {
        const expectedState = {
            inputData: [],
            enablerName: 'Non-existent Enabler',
        };

        expect(wizardReducer({ inputData: [] }, {
            type: SELECT_ENABLER,
            payload: { enablerName: 'Non-existent Enabler' },
        })).toEqual(expectedState);
    });

    it('should update inputData on INPUT_UPDATED', () => {
        const initialState = {
            inputData: [
                {
                    text: 'TEST 2',
                    content: {
                        key: { value: '0', question: 'Why?' },
                    }
                },
            ],
        };

        const expectedState = {
            inputData: [
                {
                    text: 'TEST 2',
                    content: {
                        key: { value: '42', question: 'Why?' },
                    }
                },
            ],
        };

        expect(wizardReducer(initialState, {
            type: INPUT_UPDATED,
            payload: {
                category: {
                    text: 'TEST 2',
                    content: {
                        key: { value: '42', question: 'Why?' },
                    }
                },
            },
        })).toEqual(expectedState);
    });

    it('should not update inputData on INPUT_UPDATED, if the "text" doesnt match', () => {
        const initialState = {
            inputData: [
                {
                    text: 'TEST 2',
                    content: {
                        key: { value: '0', question: 'Why?' },
                    }
                },
            ],
        };

        const expectedState = {
            inputData: [
                {
                    text: 'TEST 2',
                    content: {
                        key: { value: '0', question: 'Why?' },
                    }
                },
            ],
        };

        expect(wizardReducer(initialState, {
            type: INPUT_UPDATED,
            payload: {
                category: {
                    text: 'ABC',
                    content: {}
                },
            },
        })).toEqual(expectedState);
    });

    it('should handle NEXT_CATEGORY', () => {
        const expectedState = {
            inputData: [{}, {}],
            selectedCategory: 1,
        };

        expect(wizardReducer({ inputData: [{}, {}], selectedCategory: 0 }, {
            type: NEXT_CATEGORY,
            payload: null,
        })).toEqual(expectedState);
    });

    it('should handle CHANGE_CATEGORY', () => {
        const expectedState = {
            inputData: [{}, {}],
            selectedCategory: 1,
        };

        expect(wizardReducer({ inputData: [{}, {}], selectedCategory: 0 }, {
            type: CHANGE_CATEGORY,
            payload: { category: 1 },
        })).toEqual(expectedState);
    });
});
