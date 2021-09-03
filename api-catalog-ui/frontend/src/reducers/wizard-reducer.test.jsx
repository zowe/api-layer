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
    NEXT_CATEGORY, OVERRIDE_DEF,
    READY_YAML_OBJECT,
    REMOVE_INDEX,
    SELECT_ENABLER,
    TOGGLE_DISPLAY, UPDATE_SERVICE_ID,
    VALIDATE_INPUT,
    WIZARD_VISIBILITY_TOGGLE
} from '../constants/wizard-constants';
import wizardReducer, {
    addDefaultValues,
    setDefault,
    wizardReducerDefaultState
} from './wizard-reducer';

describe('>>> Wizard reducer tests', () => {
    it('should return default state in the default action', () => {
        expect(wizardReducer()).toEqual(wizardReducerDefaultState);
    });

    it('should handle WIZARD_VISIBILITY_TOGGLE true -> false & false -> true ', () => {
        expect(wizardReducer({ userCanAutoOnboard: false }, {
            type: WIZARD_VISIBILITY_TOGGLE,
            payload: { state: true }
        })).toEqual({ userCanAutoOnboard: true });
        expect(wizardReducer({ userCanAutoOnboard: true }, {
            type: WIZARD_VISIBILITY_TOGGLE,
            payload: { state: false }
        })).toEqual({ userCanAutoOnboard: false });
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
            content: [{
                myCategory: { value: 'dummy value', question: 'This is a dummy question', }
            }]
        }];

        const expectedData = [{
            ...dummyData[0],
            indentation: false,
            nav: 'Test Category',
        }];

        expect(wizardReducer({ inputData: [] }, {
            type: SELECT_ENABLER,
            payload: { enablerName: 'Test Enabler' },
        }, { enablerData: dummyEnablerData, categoryData: dummyData }))
            .toEqual({
                enablerName: 'Test Enabler',
                inputData: expectedData,
                selectedCategory: 0,
                navsObj: {
                    'Test Category': {
                        'Test Category': [[]],
                        silent: true,
                        warn: false,
                    }
                }
            });
    });

    it('should handle SELECT_ENABLER without indentation', () => {
        const dummyEnablerData = [{
            text: 'Test Enabler',
            categories: [{ name: 'Test Category' }]
        }];

        const dummyData = [{
            text: 'Test Category',
            content: [{
                myCategory: { value: 'dummy value', question: 'This is a dummy question', }
            }]
        }];

        const expectedData = [{
            ...dummyData[0],
            nav: 'Test Category',
        }];

        expect(wizardReducer({ inputData: [] }, {
            type: SELECT_ENABLER,
            payload: { enablerName: 'Test Enabler' },
        }, { enablerData: dummyEnablerData, categoryData: dummyData }))
            .toEqual({
                enablerName: 'Test Enabler',
                inputData: expectedData,
                navsObj: {
                    'Test Category': {
                        'Test Category': [[]],
                        silent: true,
                        warn: false,
                    }
                },
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
                inputData: [{ text: 'CAT #0', nav: '#1' }, { text: 'CAT #1', nav: '#1' },],
                navsObj: {
                    '#1': {
                        'CAT #0': [[]],
                        'CAT #1': [[]],
                        silent: true,
                        warn: false,
                    }
                },
                selectedCategory: 0
            });
    });

    it('should handle SELECT_ENABLER when the enabler allows multiple configs and it has to be in an array', () => {
        const dummyEnablerData = [{
            text: 'Test Enabler',
            categories: [{ name: 'Test Category', indentation: false, multiple: true, inArr: true }]
        }];

        const dummyData = [{
            text: 'Test Category',
            content: {
                myCategory: { value: 'dummy value', question: 'This is a dummy question', }
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
                        myCategory: { value: 'dummy value', question: 'This is a dummy question', }
                    }],
                    multiple: true,
                    indentation: false,
                    inArr: true,
                }],
                navsObj: {
                    'Test Category': {
                        'Test Category': [[]],
                        silent: true,
                        warn: false,
                    }
                },
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
            navsObj: {},
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
                content: { key: { value: '0', question: 'Why?' }, },
            }],
            yamlObject: [{
                text: 'TEST 2',
                content: { key: { value: '0', question: 'Why?' }, },
            }],
        };
        expect(wizardReducer({
            inputData: [{
                text: 'TEST 2',
                content: { key: { value: '0', question: 'Why?' }, },
            }], yamlObject: [{}, {}]
        }, {
            type: READY_YAML_OBJECT,
            payload: {
                yaml: [{
                    text: 'TEST 2',
                    content: { key: { value: '0', question: 'Why?' }, },
                }],
            },
        })).toEqual(expectedState);
    });

    it('should handle REMOVE_INDEX', () => {
        const expectedState = {
            inputData: [{
                text: 'Category 1',
                content: [{ test: { value: '', question: 'Why?', }, },
                    { test: { value: '', question: 'Why?', }, },
                ],
            },
            ]
        };
        expect(wizardReducer({
            inputData: [{
                text: 'Category 1',
                content: [{ test: { value: '', question: 'Why?', }, },
                    { test: { value: '', question: 'Why?', }, },
                    { test: { value: '', question: 'Why?', }, },],
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
                    test: { value: '', question: 'Why?', },
                },]
            },]
        };
        expect(wizardReducer({
            inputData: [{
                text: 'Category 1',
                content: [{
                    test: { value: '', question: 'Why?', },
                },],
            }]
        }, {
            type: REMOVE_INDEX,
            payload: { index: 1, text: 'Category 2' },
        })).toEqual(expectedState);
    });

    it('should handle VALIDATE_INPUT when content is an array', () => {
        const expectedState = {
            inputData: [{ text: 'Category 1', content: [{ test: { value: '', question: 'Why?' } }], nav: 'Nav' }],
            navsObj: {
                'Nav': {
                    'Category 1': [[]],
                    silent: true,
                    warn: false,
                }
            },
        };
        expect(wizardReducer({
            inputData: [{ text: 'Category 1', content: [{ test: { value: '', question: 'Why?' } }], nav: 'Nav' }],
            navsObj: {
                'Nav': {
                    'Category 1': [[]],
                    silent: true,
                    warn: false,
                }
            },
        }, {
            type: VALIDATE_INPUT,
            payload: { navName: 'Nav', silent: true },
        })).toEqual(expectedState);
    });

    it('should handle VALIDATE_INPUT when content is not an array', () => {
        const expectedState = {
            inputData: [{
                text: 'Category 1',
                content: [{
                    test: { value: '', question: 'Why?', empty: true },
                }],
                nav: 'Nav',
            },],
            navsObj: {
                'Nav': {
                    'Category 1': [['test']],
                    silent: false,
                    warn: true,
                }
            },
        };
        expect(wizardReducer({
            inputData: [{
                text: 'Category 1',
                content: [{
                    test: { value: '', question: 'Why?', },
                }],
                nav: 'Nav',
            },],
            navsObj: {
                'Nav': {
                    'Category 1': [[]],
                    silent: true,
                    warn: false,
                }
            },
        }, {
            type: VALIDATE_INPUT,
            payload: { navName: 'Nav', silent: false },
        })).toEqual(expectedState);
    });

    it('should handle VALIDATE_INPUT when nav name is not the same', () => {
        const expectedState = {
            inputData: [{
                text: 'Category 1',
                content: [{
                    test: { value: '', question: 'Why?', empty: true, },
                    test2: { value: '', question: 'Why?', optional: true, },
                }],
                nav: 'Nav',
            },
                {
                    text: 'Category 2',
                    content: [{
                        test: { value: '', question: 'Why?', },
                    }],
                    nav: 'Nav1',
                },
            ],
            navsObj: {
                'Nav': {
                    'Category 1': [['test']],
                    silent: false,
                    warn: true,
                },
                'Nav1': {
                    'Category 2': [[]],
                    silent: true,
                    warn: false,
                },

            },
        };
        expect(wizardReducer({
            inputData: [{
                text: 'Category 1',
                content: [{
                    test: { value: '', question: 'Why?' },
                    test2: { value: '', question: 'Why?', optional: true, },
                }],
                nav: 'Nav',
            },
                {
                    text: 'Category 2',
                    content: [{
                        test: { value: '', question: 'Why?', },
                    }],
                    nav: 'Nav1',
                },],
            navsObj: {
                'Nav': {
                    'Category 1': [[]],
                    silent: true,
                    warn: false,
                },
                'Nav1': {
                    'Category 2': [[]],
                    silent: true,
                    warn: false,
                },
            }
        }, {
            type: VALIDATE_INPUT,
            payload: { navName: 'Nav', silent: false },
        })).toEqual(expectedState);
    });

    it('should handle VALIDATE_INPUT when the passed navName does not exist', () => {
        const expectedState = {
            inputData: [{
                text: 'Category 1',
                content: [{
                    test: { value: '', question: 'Why?', },
                }],
                nav: 'Nav',
            },],
            navsObj: {
                'Nav': {
                    'Category 1': [[]],
                    silent: true,
                    warn: false,
                }
            },
        };
        expect(wizardReducer({
            inputData: [{
                text: 'Category 1',
                content: [{
                    test: { value: '', question: 'Why?', },
                }],
                nav: 'Nav',
            }],
            navsObj: {
                'Nav': {
                    'Category 1': [[]],
                    silent: true,
                    warn: false,
                }
            },
        }, {
            type: VALIDATE_INPUT,
            payload: { navName: 'Nav1', silent: false },
        })).toEqual(expectedState);
    });

    it('should update the service ID', () => {
        const expectedState = { serviceId: 'newId'};
        expect(wizardReducer({ serviceId: 'hey' }, {
            type: UPDATE_SERVICE_ID,
            payload: {
                value: 'newId',
            },
        })).toEqual(expectedState);
    })

    it('should override static definition', () => {
        const expectedState = { confirmDialog: true, wizardIsOpen: false};
        expect(wizardReducer({ confirmDialog: false, wizardIsOpen: true }, {
            type: OVERRIDE_DEF,
        })).toEqual(expectedState);
    })

    it('should add default values', () => {
        const content = {
            test: { value: '', question: 'Why?', },
        };
        const defaultsArr = [['test', 'val']];
        const newContent = addDefaultValues(content, defaultsArr);
        expect(newContent).toEqual({ test: { value: 'val', question: 'Why?', }, });
    });

    it('should not add default values if key does not match', () => {
        const content = {
            test: { value: '', question: 'Why?', },
        };
        const defaultsArr = [['someKey', 'val']];
        const newContent = addDefaultValues(content, defaultsArr);
        expect(newContent).toEqual(content);
    });

    it('should set the default value when content is an array', () => {
        const category = {
            text: 'Category 1',
            content: [{
                test: { value: '', question: 'Why?', },
                test2: { value: '', question: 'Why not?', },
            }]
        };
        const expectedCategory = {
            text: 'Category 1',
            content: [{
                test: { value: 'val1', question: 'Why?', },
                test2: { value: 'val2', question: 'Why not?', },
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
            content: [{
                test: { value: '', question: 'Why?', },
            }]
        };
        const expectedCategory = {
            text: 'Category 1',
            content: [{
                test: { value: 'val1', question: 'Why?', },
            }]
        };
        const defaults = {
            'Category 1': { test: 'val1', },
        };
        const newCategory = setDefault(category, defaults);
        expect(newCategory).toEqual(expectedCategory);
    });
});
