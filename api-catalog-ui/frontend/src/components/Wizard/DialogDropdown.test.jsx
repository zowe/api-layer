import * as enzyme from 'enzyme';
import DialogDropdown from './DialogDropdown';
import React from 'react';

describe('>>> DialogDropdown tests', () => {
    it('should have "Onboard New API" button', () => {
        const wrapper = enzyme.shallow(
            <DialogDropdown
                WIP={false}
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
        const expectedData = [{
            text: 'Plain Java Enabler',
            onClick: testFunc,
        },
            ]
        const wrapper = enzyme.shallow(
            <DialogDropdown
                WIP={true}
                data={dummyData}
                toggleWizard={testFunc}
            />
        );
        let instance = wrapper.instance();
        instance.openOnClick();
        expect(instance.state.data).toEqual(expectedData);
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
