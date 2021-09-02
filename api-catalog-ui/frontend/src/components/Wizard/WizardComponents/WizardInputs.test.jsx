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
        instance.handleSelect({ name: 'testInput', index: 0, value: 'test' });
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
        });
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
        instance.handleSelect({ name: 'testInput', index: 0, value: '' });
        expect(updateWizardData).toHaveBeenCalledWith({
            content: [{ testInput: { value: '', question: '', options: ['test'], show: true, interactedWith: true, problem: false, } }],
            text: 'Basic info',
        });
    });
    it('should create 4 inputs based on data', () => {
        const dummyData = {
            text: 'Dummy Data',
            content: [{
                test: { value: '', question: '', },
                test2: { value: '', question: '', },
                test3: { value: '', question: '', },
                test4: { value: '', question: '', },
            }],
            multiple: false,
        };
        const wrapper = enzyme.shallow(
            <WizardInputs updateWizardData={jest.fn()} data={dummyData} />
        );
        expect(wrapper.find('FormField').length).toEqual(4);
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
        instance.addFields();
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
        instance.addFields();
        expect(updateWizardData).toHaveBeenCalled();
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
            <WizardInputs deleteCategoryConfig={deleteCategoryConfig} validateInput={validateInput}
                          data={dummyData} />
        );
        const instance = wrapper.instance();
        instance.setState({ [`delBtn1`]: true });
        instance.handleDelete({ target: { name: 0 } });
        instance.handleDelete({ target: { name: 1 } });
        expect(validateInput).toHaveBeenCalledTimes(2);
        expect(deleteCategoryConfig).toHaveBeenCalledTimes(1);
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
                test: { value: '', question: 'Why?', optional: true, maxLength: 40, lowercase: true, empty: true, problem: false },
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
        a.props.data[1].onClick();
        expect(updateWizardData).toHaveBeenCalledWith(expectedData);
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
    })
    it('should handle tooltip', () => {
        const dummyData = {
            text: 'Category',
            content: [{
                test: { value: '', question: 'Why?', optional: true, maxLength: 40, lowercase: true, empty: true, problem: false, tooltip: 'hey' },
            }],
        };
        const wrapper = enzyme.shallow(
            <WizardInputs data={dummyData} />
        );
        const instance = wrapper.instance();
        instance.renderInputElement('test', 0, dummyData.content[0].test);
        expect(wrapper.find('Tooltip').props().content).toEqual('hey');
    })
});
