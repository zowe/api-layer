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
    it('should change value in component\'s state on keystroke', () => {
        const updateWizardData = jest.fn();
        const dummyData = {
            text: 'Basic info',
            content: { testInput: { value: 'input', question: '' } },
            multiple: false,
        };
        const wrapper = enzyme.shallow(
            <WizardInputs updateWizardData={updateWizardData} data={dummyData} />
        );
        const instance = wrapper.instance();
        instance.handleInputChange({ target: { value: 'test1', name: 'testInput' } });
        expect(updateWizardData).toHaveBeenCalled();
    });
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
            <WizardInputs updateWizardData={updateWizardData} data={dummyData} />
        );
        const instance = wrapper.instance();
        instance.handleInputChange({ target: { value: 'test1', name: 'testInput', getAttribute: () => 1 } });
        expect(updateWizardData).toHaveBeenCalled();
    });
    it('should handle booleans', () => {
        const updateWizardData = jest.fn();
        const dummyData = {
            text: 'Basic info',
            content: { testInput: { value: false, question: '' } },
        };
        const wrapper = enzyme.shallow(
            <WizardInputs updateWizardData={updateWizardData} data={dummyData} />
        );
        const instance = wrapper.instance();
        instance.handleInputChange({ target: { value: 'irrelevantValue', checked: true, name: 'testInput' } });
        expect(updateWizardData).toHaveBeenCalledWith({
            content: { testInput: { value: true, question: '', show: true } },
            text: 'Basic info',
        });
    });
    it('should create events on select use', () => {
        const updateWizardData = jest.fn();
        const dummyData = {
            text: 'Basic info',
            content: {
                testInput: { value: '', question: '', options: ['test'] },
            },
        };
        const wrapper = enzyme.shallow(
            <WizardInputs updateWizardData={updateWizardData} data={dummyData} />
        );
        const instance = wrapper.instance();
        instance.handleSelect({ name: 'testInput', index: 1, value: 'test' });
        expect(updateWizardData).toHaveBeenCalledWith({
            content: { testInput: { value: 'test', question: '', options: ['test'], show: true  } },
            text: 'Basic info',
        });
    });
    it('should create 4 inputs based on data', () => {
        const dummyData = {
            text: 'Dummy Data',
            content: {
                test: { value: '', question: '', },
                test2: { value: '', question: '', },
                test3: { value: '', question: '', },
                test4: { value: '', question: '', },
            },
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
                test: { value: '', question: '' , show: true },
                test2: { value: '', question: '', show: true  },
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
        const dummyData = {
            text: 'Dummy Data',
            content: [{
                test: { value: '', question: '', },
                test2: { value: '', question: '', },
            }],
            multiple: true,
        };
        const wrapper = enzyme.shallow(
            <WizardInputs deleteCategoryConfig={deleteCategoryConfig} data={dummyData} />
        );
        const instance = wrapper.instance();
        instance.handleDelete({ target: { name: 'Dummy Data' } });
        expect(deleteCategoryConfig).toHaveBeenCalled();
    });
    it('should restrict values to lowercase and the allowed length', () => {
        const wrapper = enzyme.shallow(
            <WizardInputs data={[]} />
        )
        const instance = wrapper.instance();
        const result = instance.applyRestrictions(5, 'SoMEthinG', true);
        expect(result).toEqual('somet');
    })
    it('should handle dependencies', () => {
        const dummyData = [{
            text: 'Category',
            content: {
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
            },
        }];
        const wrapper = enzyme.shallow(
            <WizardInputs data={dummyData} />
        )
        const instance = wrapper.instance();
        const result = instance.dependenciesSatisfied({scheme: 'someScheme'}, dummyData[0].content);
        expect(result).toEqual(false);
    })
});
