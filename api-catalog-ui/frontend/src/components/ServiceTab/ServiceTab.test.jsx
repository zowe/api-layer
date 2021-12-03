import { shallow } from 'enzyme';
import ServiceTab from './ServiceTab';

const params = {
    path: '/tile/:tileID/:serviceId',
    url: '/tile/apimediationlayer/gateway',
    params: {
        tileID: 'apimediationlayer',
        serviceId: 'gateway',
    },
};
const selectedService = {
    serviceId: 'gateway',
    title: 'API Gateway',
    description:
        'API Gateway service to route requests to services registered in the API Mediation Layer and provides an API for mainframe security.',
    status: 'UP',
    baseUrl: 'https://localhost:6000',
    homePageUrl: 'https://localhost:10010/',
    basePath: '/gateway/api/v1',
    apiDoc: null,
    apiVersions: ['org.zowe v1', 'org.zowe v2'],
    defaultApiVersion: ['org.zowe v1'],
    ssoAllInstances: true,
    gatewayUrls: {
        'org.zowe v1': 'api/v1',
        'org.zowe v2': 'api/v2'
    }
};

const tiles = {
    version: '1.0.0',
    id: 'apimediationlayer',
    title: 'API Mediation Layer API',
    status: 'UP',
    description:
        'The API Mediation Layer for z/OS internal API services. The API Mediation Layer provides a single point of access to mainframe REST APIs and offers enterprise cloud-like features such as high-availability, scalability, dynamic API discovery, and documentation.',
    services: [selectedService],
};

describe('>>> ServiceTab component tests', () => {
    xit('should display service tab information', () => {
        const selectService = jest.fn();
        const serviceTab = shallow(
            <ServiceTab
                match={params}
                selectedService={selectedService}
                tiles={[tiles]}
                selectService={selectService}
            />
        );
        serviceTab.setState({ selectedVersion: 'org.zowe v1' });

        expect(serviceTab.find('[data-testid="tooltip"]').exists()).toEqual(true);
        expect(serviceTab.find('[data-testid="link"]').exists()).toEqual(true);
        expect(
            serviceTab
                .find('[data-testid="link"]')
                .props().href
        ).toEqual('https://localhost:10010/');
        expect(
            serviceTab
                .find('[data-testid="service"]')
                .prop('children')
        ).toEqual('API Gateway');

        const checkValueItem = function(serviceTab, order, title, value) {
            const row = serviceTab.find('selector').at(order);
            // expect(row).toExist();
            console.log(row.debug())
            expect(row.find('label').prop('children')).toEqual(title);
            expect(row.find('span').prop('children')).toEqual(value);
        };

        checkValueItem(serviceTab, 1, 'API Base Path:', '/gateway/api/v1');
        checkValueItem(serviceTab, 2, 'Service ID:', 'gateway');
        checkValueItem(serviceTab, 3, 'SSO:', 'supported');
        expect(
            serviceTab
                .find('Text')
                .at(4)
                .prop('children')
        ).toEqual(
            'API Gateway service to route requests to services registered in the API Mediation Layer and provides an API for mainframe security.'
        );
        expect(
            serviceTab
                .find('Text')
                .at(5)
                .prop('children')
        ).toEqual('org.zowe v1');
        expect(
            serviceTab
                .find('Text')
                .at(6)
                .prop('children')
        ).toEqual('org.zowe v2');
    });

    it('should change selected version when clicking v2 api version', () => {
        const selectService = jest.fn();
        const serviceTab = shallow(
            <ServiceTab
                match={params}
                selectedService={selectedService}
                tiles={[tiles]}
                selectService={selectService}
            />
        );
        serviceTab.setState({ selectedVersion: 'org.zowe v1' });

        let tabContainer = serviceTab.find('div').at(3);
        expect(
            tabContainer
                .find('span')
                .first()
                .prop('style').backgroundColor
        ).toEqual('#fff');
        expect(
            tabContainer
                .find('span')
                .at(1)
                .prop('style').backgroundColor
        ).toEqual(undefined);

        tabContainer
            .find('span')
            .at(1)
            .simulate('click');

        tabContainer = serviceTab.find('div').at(3);
        expect(
            tabContainer
                .find('span')
                .at(1)
                .prop('style').backgroundColor
        ).toEqual('#fff');
        expect(
            tabContainer
                .find('span')
                .first()
                .prop('style').backgroundColor
        ).toEqual(undefined);
    });
});
