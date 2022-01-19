/* eslint-disable no-undef */
import { shallow } from 'enzyme';
import ServiceVersionDiff from './ServiceVersionDiff';

describe('>>> ServiceVersionDiff component tests', () => {
    it('Should disable the compare button', () => {
        const serviceVersionDiff = shallow(<ServiceVersionDiff serviceId="service" versions={['v1', 'v2']} />);

        expect(serviceVersionDiff.find('.api-diff-container').exists()).toEqual(true);
        expect(serviceVersionDiff.find('.api-diff-form').exists()).toEqual(true);

        expect(
            serviceVersionDiff
                .find('[data-testid="compare-label"]')
                .first()
                .prop('children')
        ).toEqual('Compare');

        expect(
            serviceVersionDiff
                .find('[data-testid="select-1"]')
                .first()
                .exists()
        ).toEqual(true);

        expect(
            serviceVersionDiff
                .find('[data-testid="select-2"]')
                .first()
                .exists()
        ).toEqual(true);

        expect(
            serviceVersionDiff
                .find('[data-testid="label-with"]')
                .first()
                .prop('children')
        ).toEqual('with');

        expect(
            serviceVersionDiff
                .find('[data-testid="menu-items-1"]')
                .first()
                .exists()
        ).toEqual(true);

        expect(
            serviceVersionDiff
                .find('[data-testid="menu-items-2"]')
                .first()
                .exists()
        ).toEqual(true);

        expect(
            serviceVersionDiff
                .find('[data-testid="menu-items-1"]')
                .first()
                .prop('value')
        ).toEqual('v1');

        expect(
            serviceVersionDiff
                .find('[data-testid="menu-items-2"]')
                .first()
                .prop('value')
        ).toEqual('v1');

        expect(
            serviceVersionDiff
                .find('[data-testid="diff-button"]')
                .first()
                .prop('children')
        ).toEqual('Go');

        expect(
            serviceVersionDiff
                .find('[data-testid="diff-button"]')
                .first()
                .prop('disabled')
        ).toEqual(true);
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

        serviceVersionDiff
            .find('[data-testid="diff-button"]')
            .first()
            .simulate('click');
        expect(getDiff.mock.calls.length).toBe(1);
    });
});
