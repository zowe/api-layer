/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
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
        selectedService.apis = {
            v1: { apiId: 'zowe.apiml.gateway' },
        };
        const tiles = [
            {
                activeServices: 1,
                hideServiceInfo: false,
            },
        ];
        const instanceInfo = shallow(
            <InstanceInfo
                service={selectedService}
                selectedVersion="v1"
                tiles={tiles}
            />
        );

        expect(instanceInfo.find('label').at(0).prop('children')).toEqual('Instance URL:');
        expect(instanceInfo.find('span').at(0).prop('children')).toEqual('https://localhost:6000');
        expect(instanceInfo.find('label').at(1).prop('children')).toEqual('API ID:');
        expect(instanceInfo.find('span').at(1).prop('children')).toEqual('zowe.apiml.gateway');
    });

    it('No selected version, use defaultApiVersion', () => {
        selectedService.apis = {
            v1: { apiId: 'zowe.apiml.gateway' },
        };
        selectedService.defaultApiVersion = ['v1'];
        const tiles = [{}];
        const instanceInfo = shallow(
            <InstanceInfo service={selectedService} tiles={tiles} />
        );

        expect(instanceInfo.find('span').at(1).prop('children')).toEqual('zowe.apiml.gateway');
    });

    it('No selected version and not set defaultApiVersion use key default', () => {
        selectedService.apis = {
            default: { apiId: 'zowe.apiml.gateway' },
        };
        selectedService.defaultApiVersion = null;
        const tiles = [{}];
        const instanceInfo = shallow(
            <InstanceInfo service={selectedService} tiles={tiles} />
        );

        expect(instanceInfo.find('span').at(1).prop('children')).toEqual('zowe.apiml.gateway');
    });
});
