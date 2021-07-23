import * as enzyme from 'enzyme';
import React from 'react';
import WizardInputs from './WizardInputs';

describe('>>> WizardInputs tests', () => {
    it('should change value in component\'s state on keystroke', () => {
        const updateWizardData = jest.fn();
        const dummyData = {
                text: 'Basic info',
                content: {
                    testInput: {
                        value: 'input',
                        question: '',
                    },
                },
            };

        const wrapper = enzyme.shallow(
            <WizardInputs updateWizardData={updateWizardData} data={dummyData} />
        );
        const instance = wrapper.instance();
        instance.handleInputChange({target:{value:'test1', name:'testInput'}});
        expect(updateWizardData).toHaveBeenCalled();
    });

    it('should create 4 inputs based on data', () => {
        const dummyData = {
                text: 'Dummy Data',
                content: {
                    test: {
                        value: '',
                        question: '',
                    },
                    test2: {
                        value: '',
                        question: '',
                    },
                    test3: {
                        value: '',
                        question: '',
                    },
                    test4: {
                        value: '',
                        question: '',
                    },
                },
            };
        const wrapper = enzyme.shallow(
            <WizardInputs updateWizardData={jest.fn()} data={dummyData} />
        );
        expect(wrapper.find('FormField').length).toEqual(4);
    });

    it('should not load', () => {
        const updateWizardData = jest.fn();

        const wrapper = enzyme.shallow(
            <WizardInputs updateWizardData={updateWizardData} data={undefined} />
        );
        expect(wrapper.find('FormField').length).toEqual(0);
    });
});
