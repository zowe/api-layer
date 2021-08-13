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
    INPUT_UPDATED, NAV_NUMBER,
    NEXT_CATEGORY,
    READY_YAML_OBJECT,
    REMOVE_INDEX,
    SELECT_ENABLER,
    TOGGLE_DISPLAY
} from '../constants/wizard-constants';
import wizardReducer, { addDefaultValues, setDefault, wizardReducerDefaultState } from './wizard-reducer';

xdescribe('>>> Wizard reducer tests', () => {
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

        const expectedData = [{
            ...dummyData[0],
            indentation: false,
            nav: "Test Category",
        }];

        expect(wizardReducer({ inputData: [], navTabAmount: 0 }, {
            type: SELECT_ENABLER,
            payload: { enablerName: 'Test Enabler' },
        }, { enablerData: dummyEnablerData, categoryData: dummyData }))
            .toEqual({
                enablerName: 'Test Enabler',
                inputData: expectedData,
                navTabAmount: 1,
                selectedCategory: 0
            });
    });

    it('should handle SELECT_ENABLER without indentation', () => {
        const dummyEnablerData = [{
            text: 'Test Enabler',
            categories: [{ name: 'Test Category' }]
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

        const expectedData = [{
            ...dummyData[0],
            nav: "Test Category",
        }];

        expect(wizardReducer({ inputData: [] }, {
            type: SELECT_ENABLER,
            payload: { enablerName: 'Test Enabler' },
        }, { enablerData: dummyEnablerData, categoryData: dummyData }))
            .toEqual({
                enablerName: 'Test Enabler',
                inputData: expectedData,
                navTabAmount: 1,
                selectedCategory: 0
            });
    });

    it('should handle SELECT_ENABLER with navs', () => {
        const dummyEnablerData = [{
            text: 'Test Enabler',
            categories: [{ name: 'CAT #0', nav: '#1' }, { name: 'CAT #1', nav: '#1' }]
        }];

        const dummyData = [
            { text: 'CAT #0' },
            { text: 'CAT #1' },
        ];

        expect(wizardReducer({ inputData: [] }, {
            type: SELECT_ENABLER,
            payload: { enablerName: 'Test Enabler' },
        }, { enablerData: dummyEnablerData, categoryData: dummyData }))
            .toEqual({
                enablerName: 'Test Enabler',
                inputData: [ { text: 'CAT #0', nav: '#1' }, { text: 'CAT #1', nav: '#1' },],
                navTabAmount: 2,
                selectedCategory: 0
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
        }, { enablerData: dummyEnablerData, categoryData: dummyData }))
            .toEqual({
                enablerName: 'Test Enabler',
                inputData: [{
                    text: 'Test Category',
                    nav: 'Test Category',
                    content: [{
                        myCategory: {
                            value: 'dummy value',
                            question: 'This is a dummy question',
                        }
                    }],
                    multiple: true,
                    indentation: false,
                }],
                navTabAmount: 1,
                selectedCategory: 0
            });
    });

    it('should handle default state in SELECT_ENABLER', () => {
        const enablerData = [{
            text: 'Test Enabler',
            categories: [{ name: 'Wrong Category' }]
        }];
        const categoryData = [{
            text: 'Right Category',
            content: {},
        }];
        const expectedState = {
            inputData: [],
            enablerName: 'Test Enabler',
            navTabAmount: 0,
            selectedCategory: 0
        };
        expect(wizardReducer({ inputData: [] }, {
            type: SELECT_ENABLER,
            payload: { enablerName: 'Test Enabler' },
        }, { enablerData, categoryData })).toEqual(expectedState);
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

    it('should handle READY_YAML_OBJECT', () => {
        const expectedState = {
            inputData: [{
                text: 'TEST 2',
                content: {
                    key: { value: '0', question: 'Why?' },
                }
            }],
            yamlObject: [{
                text: 'TEST 2',
                content: {
                    key: { value: '0', question: 'Why?' },
                }
            }],
        };
        expect(wizardReducer({
            inputData: [{
                text: 'TEST 2',
                content: {
                    key: { value: '0', question: 'Why?' },
                }
            }], yamlObject: [{}, {}]
        }, {
            type: READY_YAML_OBJECT,
            payload: {
                yaml: [{
                    text: 'TEST 2',
                    content: {
                        key: { value: '0', question: 'Why?' },
                    }
                }],
            },
        })).toEqual(expectedState);
    });

    it('should handle REMOVE_INDEX', () => {
        const expectedState = {
            inputData: [{
                text: 'Category 1',
                content: [{
                    test: {
                        value: '',
                        question: 'Why?',
                    },
                },
                    {
                        test: {
                            value: '',
                            question: 'Why?',
                        },
                    },
                ],
            },
            ]
        };
        expect(wizardReducer({
            inputData: [{
                text: 'Category 1',
                content: [{
                    test: {
                        value: '',
                        question: 'Why?',
                    },
                },
                    {
                        test: {
                            value: '',
                            question: 'Why?',
                        },
                    },
                    {
                        test: {
                            value: '',
                            question: 'Why?',
                        },
                    },
                ],
            }]
        }, {
            type: REMOVE_INDEX,
            payload: { index: 1, text: 'Category 1' },
        })).toEqual(expectedState);
    });

    it('should handle REMOVE_INDEX when category doesn\'t match', () => {
        const expectedState = {
            inputData: [{
                text: 'Category 1',
                content: [{
                    test: {
                        value: '',
                        question: 'Why?',
                    },
                },]
            },]
        };
        expect(wizardReducer({
            inputData: [{
                text: 'Category 1',
                content: [{
                    test: {
                        value: '',
                        question: 'Why?',
                    },
                },
                ],
            }]
        }, {
            type: REMOVE_INDEX,
            payload: { index: 1, text: 'Category 2' },
        })).toEqual(expectedState);
    });

    it('should add default values', () => {
        const content = {
            test: {
                value: '',
                question: 'Why?',
            },
        };
        const defaultsArr = [['test', 'val']];
        const newContent = addDefaultValues(content, defaultsArr);
        expect(newContent).toEqual({ test: { value: 'val', question: 'Why?', }, });
    });

    it('should not add default values if key does not match', () => {
        const content = {
            test: {
                value: '',
                question: 'Why?',
            },
        };
        const defaultsArr = [['someKey', 'val']];
        const newContent = addDefaultValues(content, defaultsArr);
        expect(newContent).toEqual(content);
    });

    it('should set the default value when content is an array', () => {
        const category = {
            text: 'Category 1',
            content: [{
                test: {
                    value: '',
                    question: 'Why?',
                },
                test2: {
                    value: '',
                    question: 'Why not?',
                },
            }]
        };
        const expectedCategory = {
            text: 'Category 1',
            content: [{
                test: {
                    value: 'val1',
                    question: 'Why?',
                },
                test2: {
                    value: 'val2',
                    question: 'Why not?',
                },
            }]
        };
        const defaults = {
            'Category 1': { test: 'val1', test2: 'val2' },
        };
        const newCategory = setDefault(category, defaults);
        expect(newCategory).toEqual(expectedCategory);
    });

    it('should set the default value when content is an object', () => {
        const category = {
            text: 'Category 1',
            content: {
                test: {
                    value: '',
                    question: 'Why?',
                },
            }
        };
        const expectedCategory = {
            text: 'Category 1',
            content: {
                test: {
                    value: 'val1',
                    question: 'Why?',
                },
            }
        };
        const defaults = {
            'Category 1': { test: 'val1', },
        };
        const newCategory = setDefault(category, defaults);
        expect(newCategory).toEqual(expectedCategory);
    });

    it('should handle NAV_NUMBER', () => {
        const expectedState = {
            inputData: [{
                text: 'Category 1',
                content: [{
                    test: {
                        value: 'val1',
                        question: 'Why?',
                    },
                }],
            }],
            navTabAmount: 1,
        };
        expect(wizardReducer({ inputData: [{
                text: 'Category 1',
                content: [{
                    test: {
                        value: 'val1',
                        question: 'Why?',
                    },
                }],
            }], navTabAmount: 0 }, {
            type: NAV_NUMBER,
            payload: { tabAmount: 1 },
        })).toEqual(expectedState);
    });
});
