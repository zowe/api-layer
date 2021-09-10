/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import * as enzyme from 'enzyme';
import React from 'react';
import WizardInputs from './WizardInputs';

describe('>>> WizardInputs tests', () => {
    it('should change value in component\'s state on keystroke when content is an array', () => {
        const updateWizardData = jest.fn();
        const dummyData = {
            text: 'Basic info',
            content: [
                { testInput: { value: 'input', question: '' } },
                { testInput: { value: 'input', question: '' } },
            ],
            multiple: true,
        };
        const wrapper = enzyme.shallow(
            <WizardInputs validateInput={jest.fn()} updateWizardData={updateWizardData} data={dummyData} />
        );
        const instance = wrapper.instance();
        instance.handleInputChange({ target: { value: 'test1', name: 'testInput', getAttribute: () => 0 } });
        expect(updateWizardData).toHaveBeenCalled();
    });
    it('should handle booleans', () => {
        const updateWizardData = jest.fn();
        const dummyData = {
            text: 'Basic info',
            content: [{ testInput: { value: false, question: '' } }],
        };
        const wrapper = enzyme.shallow(
            <WizardInputs validateInput={jest.fn()} updateWizardData={updateWizardData} data={dummyData} />
        );
        const instance = wrapper.instance();
        instance.handleInputChange({
            target: {
                value: 'irrelevantValue',
                checked: true,
                name: 'testInput',
                getAttribute: () => 0
            }
        });
        expect(updateWizardData).toHaveBeenCalledWith({
            content: [{ testInput: { value: true, question: '', show: true, interactedWith: true } }],
            text: 'Basic info',
        });
    });
    it('should create events on select use', () => {
        const updateWizardData = jest.fn();
        const dummyData = {
            text: 'Basic info',
            content: [{ testInput: { value: '', question: '', options: ['test'] } }],
        };
        const wrapper = enzyme.shallow(
            <WizardInputs validateInput={jest.fn()} updateWizardData={updateWizardData} data={dummyData} />
        );
        const instance = wrapper.instance();
        instance.handleSelect({ name: 'testInput', index: 0, value: 'test' }).then(() =>
            expect(updateWizardData).toHaveBeenCalledWith({
                content: [{
                    testInput: {
                        value: 'test',
                        question: '',
                        options: ['test'],
                        interactedWith: true,
                        show: true,
                        empty: false,
                        problem: false,
                    }
                }],
                text: 'Basic info',
            })
        );
    });
    it('should create correct events on select use', () => {
        const updateWizardData = jest.fn();
        const dummyData = {
            text: 'Basic info',
            content: [{
                testInput: { value: '', question: '', options: ['test'] },
            }],
        };
        const wrapper = enzyme.shallow(
            <WizardInputs validateInput={jest.fn()} updateWizardData={updateWizardData} data={dummyData} />
        );
        const instance = wrapper.instance();
        instance.handleSelect({ name: 'testInput', index: 0, value: '' }).then(() =>
            expect(updateWizardData).toHaveBeenCalledWith({
                content: [{
                    testInput: {
                        value: '',
                        question: '',
                        options: ['test'],
                        show: true,
                        interactedWith: true,
                        problem: false,
                    }
                }],
                text: 'Basic info',
            }));
    });
    it('should create 3 inputs based on data and visibility', () => {
        const dummyData = {
            text: 'Dummy Data',
            content: [{
                test: { value: '', question: '', hide: true },
                test2: { value: '', question: '', },
                test3: { value: '', question: '', },
                test4: { value: '', question: '', },
            }],
            multiple: false,
        };
        const wrapper = enzyme.shallow(
            <WizardInputs updateWizardData={jest.fn()} data={dummyData} />
        );
        expect(wrapper.find('FormField').length).toEqual(3);
    });
    it('should not load', () => {
        const updateWizardData = jest.fn();
        const dummyData = {};
        const wrapper = enzyme.shallow(
            <WizardInputs updateWizardData={updateWizardData} data={dummyData} />
        );
        expect(wrapper.find('FormField').length).toEqual(0);
    });
    it('should add more fields', () => {
        const updateWizardData = jest.fn();
        const dummyData = {
            text: 'Dummy Data',
            content: [{
                test: { value: '', question: '', show: true },
                test2: { value: '', question: '', show: true },
            }],
            multiple: true,
        };
        const wrapper = enzyme.shallow(
            <WizardInputs updateWizardData={updateWizardData} data={dummyData} />
        );
        const instance = wrapper.instance();
        instance.addFields(dummyData);
        expect(updateWizardData).toHaveBeenCalled();
    });
    it('should add more fields when value is boolean', () => {
        const updateWizardData = jest.fn();
        const dummyData = {
            text: 'Dummy Data',
            content: [{
                test: { value: true, question: '', show: true },
                test2: { value: '', question: '', show: true },
            }],
            multiple: true,
        };
        const wrapper = enzyme.shallow(
            <WizardInputs updateWizardData={updateWizardData} data={dummyData} />
        );
        const instance = wrapper.instance();
        instance.addFields(dummyData);
        expect(updateWizardData).toHaveBeenCalled();
    });
    it('should add more fields to the current category (minion)', () => {
        const updateWizardData = jest.fn();
        const dummyCategory = {
            text: 'Dummy Category',
            content: [{
                field1: { value: true, question: '', show: true },
                field2: { value: 'hey', question: '', show: true },
            }],
            multiple: true,
            minions: { 'Test Category 2': ['test2'] }
        };
        const dummyData = [{
            text: 'Test Category',
            content: [{
                myCategory: { value: 'dummy value', question: 'This is a dummy question', }
            }],
            multiple: true,
        },
            {
                text: 'Test Category 2',
                content: [{ test2: { value: 'val', question: 'Why?' } }],
                multiple: true,
                isMinion: true,
            }
        ];
        const wrapper = enzyme.shallow(
            <WizardInputs updateWizardData={updateWizardData} inputData={dummyData} data={dummyCategory} />
        );
        const instance = wrapper.instance();
        instance.addFieldsToCurrentCategory();
        expect(updateWizardData).toHaveBeenCalled();
    });
    it('should add more fields to the current category without minions', () => {
        const updateWizardData = jest.fn();
        const dummyCategory = {
            text: 'Dummy Category',
            content: [{
                field1: { value: true, question: '', show: true },
                field2: { value: 'hey', question: '', show: true },
            }],
            multiple: true,
        };
        const dummyData = [{
            text: 'Test Category',
            content: [{
                myCategory: { value: 'dummy value', question: 'This is a dummy question', }
            }],
            multiple: true,
        },];
        const wrapper = enzyme.shallow(
            <WizardInputs updateWizardData={updateWizardData} inputData={dummyData} data={dummyCategory} />
        );
        const instance = wrapper.instance();
        instance.addFieldsToCurrentCategory();
        expect(updateWizardData).toHaveBeenCalled();
    });
    it('should update the value of the minions', () => {
        const updateWizardData = jest.fn();
        const dummyCategory = {
            text: 'Dummy Category',
            content: [{
                test2: { value: 'val', question: '', show: true },
            }],
            multiple: true,
            minions: { 'Test Category 2': ['test2'] }
        };
        const dummyData = [
            {
                text: 'Test Category 2',
                content: [{ test2: { value: '', question: 'Why?' } }],
                multiple: true,
                isMinion: true,
            }
        ];
        const wrapper = enzyme.shallow(
            <WizardInputs updateWizardData={updateWizardData} inputData={dummyData} data={dummyCategory} />
        );
        const instance = wrapper.instance();
        instance.propagateToMinions('test2', 'val', 0);
        expect(updateWizardData).toHaveBeenCalled();
    });
    it('should not update the value of the minion if no such minion input field exists', () => {
        const updateWizardData = jest.fn();
        const dummyCategory = {
            text: 'Dummy Category',
            content: [{
                test2: { value: 'val', question: '', show: true },
            }],
            multiple: true,
            minions: { 'Test Category 2': ['test'] }
        };
        const dummyData = [
            {
                text: 'Test Category 2',
                content: [{ test2: { value: '', question: 'Why?' } }],
                multiple: true,
                isMinion: true,
            }
        ];
        const wrapper = enzyme.shallow(
            <WizardInputs updateWizardData={updateWizardData} inputData={dummyData} data={dummyCategory} />
        );
        const instance = wrapper.instance();
        instance.propagateToMinions('test2', 'val', 0);
        expect(updateWizardData).toHaveBeenCalledTimes(0);
    });
    it('should not update the value of the minion if no such minion exists', () => {
        const updateWizardData = jest.fn();
        const dummyCategory = {
            text: 'Dummy Category',
            content: [{
                test2: { value: 'val', question: '', show: true },
            }],
            multiple: true,
            minions: { 'Test Category 2': ['test2'] }
        };
        const dummyData = [
            {
                text: 'Test Category',
                content: [{ test2: { value: '', question: 'Why?' } }],
                multiple: true,
                isMinion: true,
            }
        ];
        const wrapper = enzyme.shallow(
            <WizardInputs updateWizardData={updateWizardData} inputData={dummyData} data={dummyCategory} />
        );
        const instance = wrapper.instance();
        instance.propagateToMinions('test2', 'val', 0);
        expect(updateWizardData).toHaveBeenCalledTimes(0);
    });
    it('should delete added input fields', () => {
        const deleteCategoryConfig = jest.fn();
        const validateInput = jest.fn();
        const dummyData = {
            text: 'Dummy Data',
            content: [
                {
                    test: { value: '', question: 'Why?', empty: true, },
                    test2: { value: '', question: 'When?', empty: true, },
                },
                {
                    test: { value: 'val', question: 'Why?', empty: false, },
                    test2: { value: 'val2', question: 'When?', empty: false, },
                }
            ],
            multiple: true,
        };
        const wrapper = enzyme.shallow(
            <WizardInputs data={dummyData} deleteCategoryConfig={deleteCategoryConfig} validateInput={validateInput}
            />
        );
        const instance = wrapper.instance();
        instance.setState({ [`delBtn1`]: true });
        instance.handleDelete({ target: { name: 0 } });
        instance.handleDelete({ target: { name: 1 } });
        expect(validateInput).toHaveBeenCalledTimes(2);
        expect(deleteCategoryConfig).toHaveBeenCalledTimes(1);
    });
    it('should delete added minion input fields', () => {
        const deleteCategoryConfig = jest.fn();
        const validateInput = jest.fn();
        const dummyData = {
            text: 'Dummy Data',
            content: [
                {
                    test: { value: '', question: 'Why?', empty: true, },
                    test2: { value: '', question: 'When?', empty: true, },
                },
                {
                    test: { value: 'val', question: 'Why?', empty: false, },
                    test2: { value: 'val2', question: 'When?', empty: false, },
                }
            ],
            multiple: true,
            minions: { 'Test Cat': ['testCont'] },
        };
        const wrapper = enzyme.shallow(
            <WizardInputs deleteCategoryConfig={deleteCategoryConfig} validateInput={validateInput}
                          data={dummyData} />
        );
        const instance = wrapper.instance();
        instance.setState({ [`delBtn1`]: true });
        instance.handleDelete({ target: { name: 0 } });
        instance.handleDelete({ target: { name: 1 } });
        expect(validateInput).toHaveBeenCalledTimes(2);
        expect(deleteCategoryConfig).toHaveBeenCalledTimes(2);
    });
    it('should restrict values to lowercase and the allowed length', () => {
        const wrapper = enzyme.shallow(
            <WizardInputs data={[]} />
        );
        const instance = wrapper.instance();
        const result = instance.applyRestrictions(5, 'SoMEthinG', true);
        expect(result).toEqual('somet');
    });
    it('should not render if dependencies are not satisfied', () => {
        const dummyData = {
            text: 'Category',
            content: [{
                test: {
                    value: '',
                    question: 'Why?',
                    dependencies: { scheme: 'someScheme' },
                    show: true,
                },
                scheme: {
                    value: '',
                    question: 'Why not?',
                    options: ['someScheme', 'otherScheme'],
                },
            }],
        };
        const wrapper = enzyme.shallow(
            <WizardInputs data={dummyData} />
        );
        const instance = wrapper.instance();
        const result = instance.renderInputs(dummyData.content[0]);
        expect(result[0]).toEqual(null);
    });
    it('should render if dependency is satisfied', () => {
        const dummyData = {
            text: 'Category',
            content: [{
                test: { value: '', question: 'Why?', dependencies: { scheme: 'schemeValue' } },
                scheme: { value: 'schemeValue', question: 'Who?' },
            }],
        };
        const wrapper = enzyme.shallow(
            <WizardInputs data={dummyData} />
        );
        const instance = wrapper.instance();
        const result = instance.renderInputs(dummyData.content,);
        expect(result[0]).not.toEqual(null);
    });
    it('should correctly generate captions for all restrictions', () => {
        const dummyData = {
            text: 'Category',
            content: [{
                test: {
                    value: '',
                    question: 'Why?',
                    optional: true,
                    maxLength: 40,
                    lowercase: true,
                    empty: true,
                    problem: false
                },
            }],
        };
        const wrapper = enzyme.shallow(
            <WizardInputs data={dummyData} />
        );
        const instance = wrapper.instance();
        instance.renderInputElement('test', 0, dummyData.content[0].test);
        expect(wrapper.find('FormField').props().caption).toEqual('Optional field; Field must be lowercase; Max length is 40 characters');
    });
    it('should handle select\'s onCLick', () => {
        const updateWizardData = jest.fn();
        const dummyData = {
            text: 'Category',
            content: [{ test: { value: 'a', question: 'Why?', empty: true, options: ['a', 'b', 'c'] } }],
            multiple: false,
        };
        const expectedData = {
            text: 'Category',
            content: [{
                test: {
                    value: 'b',
                    question: 'Why?',
                    options: ['a', 'b', 'c'],
                    empty: false,
                    show: true,
                    interactedWith: true,
                    problem: false,
                }
            }],
            multiple: false,
        };
        const wrapper = enzyme.shallow(
            <WizardInputs validateInput={jest.fn()} data={dummyData} updateWizardData={updateWizardData} />
        );
        const instance = wrapper.instance();
        const a = instance.renderInputElement('test', 0, dummyData.content[0].test);
        a.props.data[1].onClick().then(() => expect(updateWizardData).toHaveBeenCalledWith(expectedData));
    });
    it('should check for regex restrictions', () => {
        const updateWizardData = jest.fn();
        const dummyData = {};
        const wrapper = enzyme.shallow(
            <WizardInputs validateInput={jest.fn()} updateWizardData={updateWizardData} data={dummyData} />
        );
        const instance = wrapper.instance();
        const result = instance.checkRestrictions('hey', ['^[a-z]+$'], false);
        expect(result).toEqual(false);
    });
    it('should check for URL restrictions', () => {
        const updateWizardData = jest.fn();
        const dummyData = {};
        const wrapper = enzyme.shallow(
            <WizardInputs validateInput={jest.fn()} updateWizardData={updateWizardData} data={dummyData} />
        );
        const instance = wrapper.instance();
        const result = instance.checkRestrictions('http://www.example.com/index.html', undefined, true);
        expect(result).toEqual(false);
    });
    it('should check for restrictions correctly', () => {
        const updateWizardData = jest.fn();
        const dummyData = {};
        const wrapper = enzyme.shallow(
            <WizardInputs validateInput={jest.fn()} updateWizardData={updateWizardData} data={dummyData} />
        );
        const instance = wrapper.instance();
        const result = instance.checkRestrictions('hEy9', ['^[a-z]+$'], true);
        expect(result).toEqual(true);
    });
    it('should handle tooltip', () => {
        const dummyData = {
            text: 'Category',
            content: [{
                test: {
                    value: '',
                    question: 'Why?',
                    optional: true,
                    maxLength: 40,
                    lowercase: true,
                    empty: true,
                    problem: false,
                    tooltip: 'hey'
                },
            }],
        };
        const wrapper = enzyme.shallow(
            <WizardInputs data={dummyData} />
        );
        const instance = wrapper.instance();
        instance.renderInputElement('test', 0, dummyData.content[0].test);
        expect(wrapper.find('Tooltip').props().content).toEqual('hey');
    });
    it('should alter fields if there\'s interference', () => {
        const updateWizardData = jest.fn();
        const payload = { title: 'Option 1' };
        const dummyData = {
            text: 'Test category',
            content: [{ type: { value: '', question: 'Why?', options: ['Custom'] }, }],
            interference: 'catalog',
        };
        const dummyTiles = [{ title: 'Option 1', version: '1.0.0', id: 'opt1', description: 'Description' }];
        const expectedArgument = {
            text: 'Test category',
            content: [
                {
                    title: { value: 'Option 1', disabled: true },
                    version: { value: '1.0.0', disabled: true },
                    id: { value: 'opt1', disabled: true },
                    description: { value: 'Description', disabled: true },
                    type: { value: '', question: 'Why?', show: true, options: ['Custom'] }
                }
            ],
            interference: 'catalog',
        };
        const wrapper = enzyme.shallow(
            <WizardInputs data={dummyData} tiles={dummyTiles} updateWizardData={updateWizardData} />
        );
        const instance = wrapper.instance();
        instance.interferenceInjection(payload);
        expect(updateWizardData).toHaveBeenCalledWith(expectedArgument);
    });
    it('should alter fields if there\'s interference correctly', () => {
        const updateWizardData = jest.fn();
        const payload = { title: 'Option 2' };
        const dummyData = {
            text: 'Test category',
            content: [{ type: { value: '', question: 'Why?', options: ['Custom'] }, }],
            interference: 'catalog',
        };
        const dummyTiles = [{ title: 'Option 1', version: '1.0.0', id: 'opt1', description: 'Description' }];
        const expectedArgument = {
            text: 'Test category',
            content: [
                {
                    title: { value: '', disabled: false },
                    version: { value: '', disabled: false },
                    id: { value: '', disabled: false },
                    description: { value: '', disabled: false },
                    type: { value: '', question: 'Why?', show: true, options: ['Custom'] }
                }
            ],
            interference: 'catalog',
        };
        const wrapper = enzyme.shallow(
            <WizardInputs data={dummyData} tiles={dummyTiles} updateWizardData={updateWizardData} />
        );
        const instance = wrapper.instance();
        instance.interferenceInjection(payload);
        expect(updateWizardData).toHaveBeenCalledWith(expectedArgument);
    });
});
