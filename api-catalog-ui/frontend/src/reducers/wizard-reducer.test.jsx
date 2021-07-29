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
    CHANGE_CATEGORY,
    INPUT_UPDATED,
    NEXT_CATEGORY,
    SELECT_ENABLER,
    TOGGLE_DISPLAY
} from '../constants/wizard-constants';
import wizardReducer, { wizardReducerDefaultState } from './wizard-reducer';

describe('>>> Wizard reducer tests', () => {
    it('should return default state in the default action', () => {
        expect(wizardReducer()).toEqual(wizardReducerDefaultState);
    });

    it('should handle TOGGLE_DISPLAY true -> false & false -> true ', () => {
        expect(wizardReducer({ wizardIsOpen: true }, {
            type: TOGGLE_DISPLAY,
            payload: null
        })).toEqual({ wizardIsOpen: false });
        expect(wizardReducer({ wizardIsOpen: false }, {
            type: TOGGLE_DISPLAY,
            payload: null
        })).toEqual({ wizardIsOpen: true });
    });

    it('should handle DEFAULT', () => {
        const expectedState = {
            wizardIsOpen: true,
        };
        expect(wizardReducer({ wizardIsOpen: true }, { type: 'UNKNOWN', payload: null })).toEqual(expectedState);
        expect(wizardReducer({ wizardIsOpen: true }, null)).toEqual(expectedState);
    });

    it('should handle SELECT_ENABLER', () => {
        const dummyEnablerData = [{
            text: 'Test Enabler',
            categories: [{ name: 'Test Category', indentation: false }]
        }];

        const dummyData = [{
            text: 'Test Category',
            content: {
                myCategory: {
                    value: 'dummy value',
                    question: 'This is a dummy question',
                }
            }
        }];

        expect(wizardReducer({ inputData: [] }, {
            type: SELECT_ENABLER,
            payload: { enablerName: 'Test Enabler' },
        }, { enablerData: dummyEnablerData, data: dummyData }))
            .toEqual({
                enablerName: 'Test Enabler',
                inputData: dummyData
            });
    });

    it('should handle SELECT_ENABLER when the enabler allows multiple configs', () => {
        const dummyEnablerData = [{
            text: 'Test Enabler',
            categories: [{ name: 'Test Category', indentation: false, multiple: true }]
        }];

        const dummyData = [{
            text: 'Test Category',
            content: {
                myCategory: {
                    value: 'dummy value',
                    question: 'This is a dummy question',
                }
            },
            multiple: false,
        }];

        expect(wizardReducer({ inputData: [] }, {
            type: SELECT_ENABLER,
            payload: { enablerName: 'Test Enabler' },
        }, { enablerData: dummyEnablerData, data: dummyData }))
            .toEqual({
                enablerName: 'Test Enabler',
                inputData: [{
                    text: 'Test Category',
                    content: [{
                        myCategory: {
                            value: 'dummy value',
                            question: 'This is a dummy question',
                        }
                    }],
                    multiple: true,
                    indentation: false,
                }],
            });
    });

    it('should handle default state in SELECT_ENABLER', () => {
        const enablerData = [{
            text: 'Test Enabler',
            categories: [{ name: 'Wrong Category' }]
        }];
        const data = [{
            text: 'Right Category',
            content: {},
        }];
        const expectedState = {
            inputData: [],
            enablerName: 'Test Enabler',
        };
        expect(wizardReducer({ inputData: [] }, {
            type: SELECT_ENABLER,
            payload: { enablerName: 'Test Enabler' },
        }, { enablerData, data })).toEqual(expectedState);
    });

    it('should handle wrong category in SELECT_ENABLER', () => {
        const expectedState = {
            inputData: [],
            enablerName: 'Non-existent Enabler',
        };
        expect(wizardReducer({ inputData: [] }, {
            type: SELECT_ENABLER,
            payload: { enablerName: 'Non-existent Enabler' },
        }),).toEqual(expectedState);
    });

    it('should update inputData on INPUT_UPDATED', () => {
        const initialState = {
            inputData: [{
                text: 'TEST 2',
                content: {
                    key: { value: '0', question: 'Why?' },
                }
            }],
        };
        const expectedState = {
            inputData: [{
                text: 'TEST 2',
                content: {
                    key: { value: '42', question: 'Why?' },
                }
            }],
        };
        expect(wizardReducer(initialState, {
            type: INPUT_UPDATED,
            payload: {
                category: {
                    text: 'TEST 2',
                    content: { key: { value: '42', question: 'Why?' } }
                },
            },
        })).toEqual(expectedState);
    });

    it('should not update inputData on INPUT_UPDATED, if the "text" doesnt match', () => {
        const initialState = {
            inputData: [{
                text: 'TEST 2',
                content: {
                    key: { value: '0', question: 'Why?' },
                }
            }],
        };
        const expectedState = {
            inputData: [{
                text: 'TEST 2',
                content: {
                    key: { value: '0', question: 'Why?' },
                }
            }],
        };
        expect(wizardReducer(initialState, {
            type: INPUT_UPDATED,
            payload: {
                category: { text: 'ABC', content: {} },
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
