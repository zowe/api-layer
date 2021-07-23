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
import * as enzyme from 'enzyme';
import React from 'react';
import WizardDialog from './WizardDialog';
import { data } from './wizard_config';

describe('>>> WizardDialog tests', () => {
    it('should render the dialog if store value is true', () => {
        const wrapper = enzyme.shallow(<WizardDialog wizardToggleDisplay={jest.fn()} inputData={data} wizardIsOpen />);
        expect(wrapper.find('DialogBody').exists()).toEqual(true);
    });

    it('should create 4 inputs based on data', () => {
        const dummyData = [
            {
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
            },
        ];
        const wrapper = enzyme.shallow(
            <WizardDialog wizardToggleDisplay={jest.fn()} inputData={dummyData} wizardIsOpen />
        );
        expect(wrapper.find('FormField').length).toEqual(4);
    });

    it('should create 0 inputs if content is an empty object', () => {
        const dummyData = [
            {
                text: 'Basic info',
                content: {},
            },
        ];
        const wrapper = enzyme.shallow(
            <WizardDialog wizardToggleDisplay={jest.fn()} inputData={dummyData} wizardIsOpen />
        );
        expect(wrapper.find('TextInput').length).toEqual(0);
    });

    it('should create 0 inputs if content does not exist', () => {
        const dummyData = [
            {
                text: 'Basic info',
            },
        ];
        const wrapper = enzyme.shallow(
            <WizardDialog wizardToggleDisplay={jest.fn()} inputData={dummyData} wizardIsOpen />
        );
        expect(wrapper.find('TextInput').length).toEqual(0);
    });

    it('should create 0 inputs if content is null', () => {
        const dummyData = [
            {
                text: 'Basic info',
                content: null,
            },
        ];
        const wrapper = enzyme.shallow(
            <WizardDialog wizardToggleDisplay={jest.fn()} inputData={dummyData} wizardIsOpen />
        );
        expect(wrapper.find('TextInput').length).toEqual(0);
    });

    it('should change value in component\'s state on keystroke', () => {
        const dummyData = [
            {
                text: 'Basic info',
                content: {
                    testInput: {
                        value: 'input',
                        question: '',
                    },
                },
            },
        ];
        const expectedData = [
            {
                text: 'Basic info',
                content: {
                    testInput: {
                        value: 'test',
                        question: '',
                    },
                },
            },
        ];
        const wrapper = enzyme.shallow(
            <WizardDialog wizardToggleDisplay={jest.fn()} inputData={dummyData} wizardIsOpen />
        );
        wrapper.find('FormField').first().simulate('change', { target: { name: 'testInput', value: 'test'} });
        expect(wrapper.state()['inputData']).toEqual(expectedData);
    });

    it('should change categories', () => {
        const dummyData = [
            {
                text: 'Category #1',
                content: {
                    testInput: 'test1',
                },
            },
            {
                text: 'Category #2',
                content: {
                    testInput2: 'test2',
                },
            },
        ];
        const wrapper = enzyme.shallow(
            <WizardDialog wizardToggleDisplay={jest.fn()} inputData={dummyData} wizardIsOpen />
        );
        wrapper.setState({selectedIndex: 0});
        wrapper.find('Select').first().simulate('change', { text: 'Category #2' } );
        expect(wrapper.state().selectedIndex).toEqual(1);
    });

    it('should close dialog on cancel', () => {
        const wizardToggleDisplay = jest.fn();
        const wrapper = enzyme.shallow(
            <WizardDialog
                tiles={null}
                fetchTilesStart={jest.fn()}
                wizardToggleDisplay={wizardToggleDisplay}
                fetchTilesStop={jest.fn()}
                clearService={jest.fn()}
                clear={jest.fn()}
                inputData={data}
            />
        );
        const instance = wrapper.instance();
        instance.closeWizard();
        expect(wizardToggleDisplay).toHaveBeenCalled();
    });

    it('should close dialog and refresh static APIs on Done', () => {
        const wizardToggleDisplay = jest.fn();
        const refreshedStaticApi = jest.fn();
        const wrapper = enzyme.shallow(
            <WizardDialog
                tiles={null}
                fetchTilesStart={jest.fn()}
                wizardToggleDisplay={wizardToggleDisplay}
                refreshedStaticApi={refreshedStaticApi}
                fetchTilesStop={jest.fn()}
                clearService={jest.fn()}
                clear={jest.fn()}
                inputData={data}
            />
        );
        const instance = wrapper.instance();
        instance.doneWizard();
        expect(wizardToggleDisplay).toHaveBeenCalled();
        expect(refreshedStaticApi).toHaveBeenCalled();
    });

    it('should get previous category', () => {
        const prevIndex = jest.fn();
        const wrapper = enzyme.shallow(
            <WizardDialog
                tiles={null}
                fetchTilesStart={jest.fn()}
                prevIndex={prevIndex}
                fetchTilesStop={jest.fn()}
                clearService={jest.fn()}
                clear={jest.fn()}
                inputData={data}
            />
        );
        const instance = wrapper.instance();
        instance.setState({ selectedIndex: 0 });
        const len = data.length;
        instance.getPrev();
        expect(wrapper.state().selectedIndex).toEqual(len - 1);
    });

    it('should get next category', () => {
        const nextIndex = jest.fn();
        const wrapper = enzyme.shallow(
            <WizardDialog
                tiles={null}
                fetchTilesStart={jest.fn()}
                nextIndex={nextIndex}
                fetchTilesStop={jest.fn()}
                clearService={jest.fn()}
                clear={jest.fn()}
                inputData={data}
            />
        );
        const instance = wrapper.instance();
        const len = data.length;
        instance.setState({ selectedIndex: len - 1 });
        instance.getNext();
        expect(wrapper.state().selectedIndex).toEqual(0);
    });

    it('should refresh input data', () => {
        const changedEnablers = jest.fn();
        const wrapper = enzyme.shallow(
            <WizardDialog
                tiles={null}
                fetchTilesStart={jest.fn()}
                fetchTilesStop={jest.fn()}
                clearService={jest.fn()}
                clear={jest.fn()}
                inputData={data}
                changedEnablers={changedEnablers}
            />
        );
        const instance = wrapper.instance();
        instance.refreshInputData();
        expect(changedEnablers).toHaveBeenCalled();
    })

    it('should refresh input data if enablerChanged is true', () => {
        const changedEnablers = jest.fn();
        enzyme.shallow(
            <WizardDialog
                tiles={null}
                fetchTilesStart={jest.fn()}
                fetchTilesStop={jest.fn()}
                clearService={jest.fn()}
                clear={jest.fn()}
                inputData={data}
                changedEnablers={changedEnablers}
                enablerChanged={true}
            />
        );
        expect(changedEnablers).toHaveBeenCalled();
    })

});
