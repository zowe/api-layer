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
import DialogDropdown from './DialogDropdown';
import React from 'react'
describe('>>> DialogDropdown tests', () => {
    it('should have "Onboard New API" button', () => {
        const wrapper = enzyme.shallow(
            <DialogDropdown
                visible={true}
                data={[{
                    text: 'Plain Java Enabler',
                },
                    {
                        text: 'Spring Enabler',
                    },]}
                toggleWizard={jest.fn()}
            />
        );
        let button = wrapper.find('#wizard-YAML-button');
        expect(button.length).toEqual(1);
    });
    it('should not have "Onboard New API" button', () => {
        const wrapper = enzyme.shallow(
            <DialogDropdown
                WIP={true}
                data={[{
                    text: 'Plain Java Enabler',
                },
                    {
                        text: 'Spring Enabler',
                    },]}
                toggleWizard={jest.fn()}
            />
        );
        let button = wrapper.find('#wizard-YAML-button');
        expect(button.length).toEqual(0);
    });
    it('should add onClick for every data item', () => {
        const testFunc = jest.fn();
        const dummyData = [{
            text: 'Plain Java Enabler',
        },
        ];
        const wrapper = enzyme.shallow(
            <DialogDropdown
                WIP={true}
                data={dummyData}
                toggleWizard={testFunc}
            />
        );
        let instance = wrapper.instance();
        instance.openOnClick();
        expect(typeof(instance.state.data[0].onClick)).toEqual("function");
    });
    it('should handle click on dropdown categories', () => {
        const toggleWizard = jest.fn();
        const selectEnabler = jest.fn();
        const dummyData = [{
            text: 'Plain Java Enabler',
        },
        ];
        const wrapper = enzyme.shallow(
            <DialogDropdown
                WIP={false}
                data={dummyData}
                toggleWizard={toggleWizard}
                selectEnabler={selectEnabler}
            />
        );
        wrapper.instance().handleClick({ target: { innerText: 'TestEnabler' } });
        expect(toggleWizard).toHaveBeenCalled();
        expect(selectEnabler).toHaveBeenCalledWith('TestEnabler');
    });
    it('should not render if data not an array', () => {
        const testFunc = jest.fn();
        const dummyData = 'notArray';
        const wrapper = enzyme.shallow(
            <DialogDropdown
                WIP={false}
                data={dummyData}
                toggleWizard={testFunc}
            />
        );
        let button = wrapper.find('#wizard-YAML-button');
        expect(button.length).toEqual(0);
    });
});
