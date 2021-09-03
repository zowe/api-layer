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
import * as constants from '../constants/wizard-constants';
import * as actions from './wizard-actions';
import { addCategoryToYamlObject, handleIndentationDependency, insert } from './wizard-actions';

describe('>>> Wizard actions tests', () => {
    it('should get next category', () => {
        const expectedAction = {
            type: constants.NEXT_CATEGORY,
            payload: null,
        };
        expect(actions.nextWizardCategory()).toEqual(expectedAction);
    });
    it('should update the input', () => {
        const expectedAction = {
            type: constants.INPUT_UPDATED,
            payload: { category: 'Test category' },
        };
        expect(actions.updateWizardData('Test category')).toEqual(expectedAction);
    });
    it('should toggle the wizard', () => {
        const expectedAction = {
            type: constants.TOGGLE_DISPLAY,
            payload: null,
        };
        expect(actions.wizardToggleDisplay()).toEqual(expectedAction);
    });
    it('should select enabler', () => {
        const expectedAction = {
            type: constants.SELECT_ENABLER,
            payload: { enablerName: 'Test' },
        };
        expect(actions.selectEnabler('Test')).toEqual(expectedAction);
    });
    it('should insert if parent is empty', () => {
        const parent = {};
        const content = {
            test: 'yaml',
        };
        insert(parent, content);
        expect(parent).toEqual({ test: 'yaml' });
    });
    it('should insert', () => {
        const parent = {
            test: { text1: 'text 1', },
        };
        const content = {
            test: { text2: 'text 2', },
        };
        insert(parent, content);
        expect(parent).toEqual({ test: { text1: 'text 1', text2: 'text 2' } });
    });
    it('should add categories to the YAML object when content is not an array', () => {
        const category = {
            text: 'Category 1',
            content: [{ test: { value: 'yaml' } }],
            multiple: false,
            indentation: false,
        };
        let result = { test2: 'test 2' };
        result = addCategoryToYamlObject(category, result);
        expect(result).toEqual({ test: 'yaml', test2: 'test 2' });

    });
    it('should add categories to the YAML object when content is an array', () => {
        const category = {
            text: 'Category 1',
            content: [
                { test: { value: 'value 1' } },
                { test: { value: 'value 2' } },
            ],
            multiple: true,
            indentation: false,
        };
        let result = {};
        result = addCategoryToYamlObject(category, result);
        expect(result).toEqual({ '0': { 'test': 'value 1' }, '1': { 'test': 'value 2' } });

    });
    it('should add categories to the YAML object when content is an array without a key', () => {
        const category = {
            text: 'Category 1',
            content: [
                { test: { value: 'value 1' } },
                { test: { value: 'value 2' } },
            ],
            multiple: true,
            indentation: false,
            noKey: true,
        };
        let result = {};
        result = addCategoryToYamlObject(category, result);
        expect(result).toEqual({ '0': 'value 1' , '1': 'value 2' });

    });
    it('should handle indentation dependencies', () => {
        const inputData = [
            {
                text: 'Category 1',
                content: { test: { value: '', question: 'Why', }, },
            },
            {
                text: 'Category 2',
                content: [{
                    test3: { value: 'smth', question: 'Why not?', },
                    test2: { value: 'val', question: 'Why not?', },
                }],
            },
        ];
        const indentation = 'indent';
        const indentationDepenedency = 'test2';
        const result = handleIndentationDependency(inputData, indentationDepenedency, indentation);
        expect(result).toEqual('indent/val');
    })
    it('should handle indentation dependencies when content is not an array', () => {
        const inputData = [
            {
                text: 'Category 1',
                content: { test: { value: '', question: 'Why', }, },
            },
            {
                text: 'Category 2',
                content: {
                    test2: { value: 'val', question: 'Why not?', },
                },
            },
        ];
        const indentation = 'indent';
        const indentationDepenedency = 'test2';
        const result = handleIndentationDependency(inputData, indentationDepenedency, indentation);
        expect(result).toEqual('indent/val');
    })
    it('should add categories to the YAML object and handle indentation', () => {
        const category = {
            text: 'Category 1',
            content: [{ test: { value: 'yaml' } }],
            multiple: false,
            indentation: 'category1',
            indentationDependency: 'test2',
        };
        const inputData = [
            {
                text: 'Category 2',
                content: { test2: { value: 'val' } },
            },
        ];
        let result = { test3: 'test 3' };
        result = addCategoryToYamlObject(category, result, inputData);
        expect(result).toEqual({ category1: { val: { test: 'yaml' } }, test3: 'test 3' });
    });
    it('should add categories to the YAML object and handle empty indentation', () => {
        const category = {
            text: 'Category 1',
            content: [{
                test: { value: 'yaml' }
            }],
            multiple: false,
            indentation: '/',
        };
        let result = { test2: 'test 2' };
        result = addCategoryToYamlObject(category, result);
        expect(result).toEqual({  test: 'yaml' , test2: 'test 2' });
    });
    it('should not add categories to the YAML object if they are not shown', () => {
        const category = {
            text: 'Category 1',
            content: [{
                test: { value: 'yaml', show: false}
            }],
            multiple: false,
            indentation: '/',
        };
        let result = { test2: 'test 2' };
        result = addCategoryToYamlObject(category, result);
        expect(result).toEqual({ test2: 'test 2' });
    });
    it('should change the category', () => {
        const expectedAction = {
            type: constants.CHANGE_CATEGORY,
            payload: { category: 3 },
        };
        expect(actions.changeWizardCategory(3)).toEqual(expectedAction);
    });
    it('should remove element by index', () => {
        const expectedAction = {
            type: constants.REMOVE_INDEX,
            payload: { index: 1, text: "Basic info" },
        };
        expect(actions.deleteCategoryConfig(1, "Basic info")).toEqual(expectedAction);
    });
    it('should create an YAML object', () => {
        const expectedAction = {
            type: constants.READY_YAML_OBJECT,
            payload: { yaml: { test: 'yaml' } },
        };
        expect(actions.createYamlObject([{
            text: 'Category 1',
            content: [{
                test: { value: 'yaml' }
            }],
            multiple: false,
            indentation: false,
        }])).toEqual(expectedAction);
    });
    it('should create an YAML object with an array when needed', () => {
        const expectedAction = {
            type: constants.READY_YAML_OBJECT,
            payload: { yaml: { services: [{ test: 'yaml'}] } },
        };
        expect(actions.createYamlObject([{
            text: 'Category 1',
            content: [{ test: { value: 'yaml' } }],
            multiple: false,
            indentation: false,
            inArr: true,
        }])).toEqual(expectedAction);
    });
    it('should check for filled input', () => {
        const expectedAction = {
            type: constants.VALIDATE_INPUT,
            payload: { navName: 'Nav', silent: true },
        };
        expect(actions.validateInput('Nav', true)).toEqual(expectedAction);
    });
    it('should update service ID', () => {
        const expectedAction = {
            type: constants.UPDATE_SERVICE_ID,
            payload: { value: 'hey' },
        };
        expect(actions.updateServiceId('hey')).toEqual(expectedAction);
    });
});
