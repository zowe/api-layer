import * as React from 'react';
import { shallow } from 'enzyme';
import InstanceInfo from './InstanceInfo';

const selectedService = {
    serviceId: 'gateway',
    baseUrl: 'https://localhost:6000',
    apiId: {
        v1: 'zowe.apiml.gateway',
    },
};

describe('>>> InstanceInfo component tests', () => {
    it('Service with version v1', () => {
        selectedService.apiId = {
            v1: 'zowe.apiml.gateway',
        };
        const selectService = jest.fn();
        const instanceInfo = shallow(
            <InstanceInfo selectedService={selectedService} selectedVersion="v1" selectService={selectService} />
        );

        expect(
            instanceInfo
                .find('label')
                .at(0)
                .prop('children')
        ).toEqual('Instance URL:');
        expect(
            instanceInfo
                .find('span')
                .at(0)
                .prop('children')
        ).toEqual('https://localhost:6000');
        expect(
            instanceInfo
                .find('label')
                .at(1)
                .prop('children')
        ).toEqual('API ID:');
        expect(
            instanceInfo
                .find('span')
                .at(1)
                .prop('children')
        ).toEqual('zowe.apiml.gateway');
    });

    it('No selected version, use defaultApiVersion', () => {
        selectedService.apiId = {
            v1: 'zowe.apiml.gateway',
        };
        selectedService.defaultApiVersion = ['v1'];
        const selectService = jest.fn();
        const instanceInfo = shallow(<InstanceInfo selectedService={selectedService} selectService={selectService} />);

        expect(
            instanceInfo
                .find('span')
                .at(1)
                .prop('children')
        ).toEqual('zowe.apiml.gateway');
    });

    it('No selected version and not set defaultApiVersion use key default', () => {
        selectedService.apiId = {
            default: 'zowe.apiml.gateway',
        };
        selectedService.defaultApiVersion = null;
        const selectService = jest.fn();
        const instanceInfo = shallow(<InstanceInfo selectedService={selectedService} selectService={selectService} />);

        expect(
            instanceInfo
                .find('span')
                .at(1)
                .prop('children')
        ).toEqual('zowe.apiml.gateway');
    });
});
