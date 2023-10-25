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
import ServiceVersionDiff from './ServiceVersionDiff';

describe('>>> ServiceVersionDiff component tests', () => {
    it('Should display all the compare items', () => {
        const serviceVersionDiff = shallow(<ServiceVersionDiff serviceId="service" versions={['v1', 'v2']} />);

        expect(serviceVersionDiff.find('.api-diff-container').exists()).toEqual(true);
        expect(serviceVersionDiff.find('.api-diff-form').exists()).toEqual(true);

        expect(serviceVersionDiff.find('[data-testid="compare-label"]').first().prop('children')).toEqual('Compare');

        expect(serviceVersionDiff.find('[data-testid="select-1"]').first().exists()).toEqual(true);

        expect(serviceVersionDiff.find('[data-testid="select-2"]').first().exists()).toEqual(true);

        expect(serviceVersionDiff.find('[data-testid="label-with"]').first().prop('children')).toEqual('with');

        expect(serviceVersionDiff.find('[data-testid="menu-items-1"]').first().exists()).toEqual(true);

        expect(serviceVersionDiff.find('[data-testid="menu-items-2"]').first().exists()).toEqual(true);

        expect(serviceVersionDiff.find('[data-testid="menu-items-1"]').first().prop('value')).toEqual('v1');

        expect(serviceVersionDiff.find('[data-testid="menu-items-2"]').first().prop('value')).toEqual('v1');

        expect(serviceVersionDiff.find('[data-testid="diff-button"]').first().prop('children')).toEqual('Show');
    });

    it('Should call getDiff when button pressed', () => {
        const getDiff = jest.fn();
        const serviceVersionDiff = shallow(
            <ServiceVersionDiff
                getDiff={getDiff}
                serviceId="service"
                versions={['v1', 'v2']}
                version1="v1"
                version2="v2"
            />
        );

        serviceVersionDiff.find('[data-testid="diff-button"]').first().simulate('click');
        expect(getDiff.mock.calls.length).toBe(1);
    });

    it('Should call getDiff when default version', () => {
        const getDiff = jest.fn();
        const serviceVersionDiff = shallow(
            <ServiceVersionDiff getDiff={getDiff} serviceId="service" versions={['v1', 'v2']} version2="v2" />
        );

        serviceVersionDiff.find('[data-testid="diff-button"]').first().simulate('click');
        expect(getDiff.mock.calls.length).toBe(1);
    });

    it('should set current tile id with default version', () => {
        const getDiff = jest.fn();
        const serviceVersionDiff = shallow(
            <ServiceVersionDiff getDiff={getDiff} serviceId="service" versions={['v1', 'v2']} version2="v2" />
        );
        serviceVersionDiff.setState({ defaultVersion: 'v1' });

        serviceVersionDiff.find('[data-testid="diff-button"]').first().simulate('click');
        expect(getDiff.mock.calls.length).toBe(1);
    });
});
