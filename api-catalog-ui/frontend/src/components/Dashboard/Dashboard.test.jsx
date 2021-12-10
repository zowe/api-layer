/* eslint-disable no-undef */
import { shallow } from 'enzyme';
import Dashboard from './Dashboard';
import { categoryData } from '../Wizard/configs/wizard_categories';

const ajaxError = {
    message: 'ajax Error 404',
    name: 'AjaxError',
    request: '',
    response: { message: 'error' },
    responseType: 'json',
    status: 404,
};

describe('>>> Dashboard component tests', () => {

    it('should have "Refresh Static APIs" button', () => {
        const wrapper = shallow(
            <Dashboard
                tiles={null}
                fetchTilesStart={jest.fn()}
                fetchTilesStop={jest.fn()}
                clearService={jest.fn()}
                clear={jest.fn()}
                assertAuthorization={jest.fn()}
            />
        );
        let button = wrapper.find('#refresh-api-button');
        expect(button.length).toEqual(1);
    });

    it('should display no results if search fails', () => {
        const dashboard = shallow(
            <Dashboard
                tiles={[]}
                searchCriteria=" Supercalafragalisticexpialadoshus"
                fetchTilesStart={jest.fn()}
                clearService={jest.fn()}
                fetchTilesStop={jest.fn()}
                clear={jest.fn()}
                fetchTilesFailed={jest.fn()}
                assertAuthorization={jest.fn()}
            />
        );
        expect(
            dashboard
                .find('#search_no_results')
                .children()
                .text()
        ).toEqual('No tiles found matching search criteria');
    });

    it('should display error if error comms failure', () => {
        const dashboard = shallow(
            <Dashboard
                tiles={[]}
                fetchTilesError={ajaxError}
                clearService={jest.fn()}
                fetchTilesStart={jest.fn()}
                fetchTilesStop={jest.fn()}
                clear={jest.fn()}
                fetchTilesFailed={jest.fn()}
                assertAuthorization={jest.fn()}
            />
        );
        expect(
            dashboard
                .find('[data-testid="error"]')
                .first()
                .children()
                .text()
        ).toEqual('Tile details could not be retrieved, the following error was returned:');
    });

    it('should stop epic on unmount', () => {
        const fetchTilesStop = jest.fn();
        const clear = jest.fn();
        const wrapper = shallow(
            <Dashboard
                tiles={null}
                fetchTilesStart={jest.fn()}
                fetchTilesStop={fetchTilesStop}
                clearService={jest.fn()}
                clear={clear}
                assertAuthorization={jest.fn()}
            />
        );
        const instance = wrapper.instance();
        instance.componentWillUnmount();
        expect(fetchTilesStop).toHaveBeenCalled();
        expect(clear).toHaveBeenCalled();
    });

    it('should trigger filterText on handleSearch', () => {
        const filterText = jest.fn();
        const wrapper = shallow(
            <Dashboard
                tiles={null}
                fetchTilesStart={jest.fn()}
                filterText={filterText}
                fetchTilesStop={jest.fn()}
                clearService={jest.fn()}
                clear={jest.fn()}
                assertAuthorization={jest.fn()}
            />
        );
        const instance = wrapper.instance();
        instance.handleSearch();
        expect(filterText).toHaveBeenCalled();
    });

    it('should refresh static APIs on button click', () => {
        const refreshedStaticApi = jest.fn();
        const wrapper = shallow(
            <Dashboard
                tiles={null}
                fetchTilesStart={jest.fn()}
                refreshedStaticApi={refreshedStaticApi}
                fetchTilesStop={jest.fn()}
                clearService={jest.fn()}
                clear={jest.fn()}
                inputData={categoryData}
                assertAuthorization={jest.fn()}
            />
        );
        const instance = wrapper.instance();
        instance.refreshStaticApis();
        expect(refreshedStaticApi).toHaveBeenCalled();
    });

    it('should toggle display on button click', () => {
        const wizardToggleDisplay = jest.fn();
        const wrapper = shallow(
            <Dashboard
                tiles={null}
                fetchTilesStart={jest.fn()}
                wizardToggleDisplay={wizardToggleDisplay}
                fetchTilesStop={jest.fn()}
                clearService={jest.fn()}
                clear={jest.fn()}
                assertAuthorization={jest.fn()}
            />
        );
        const instance = wrapper.instance();
        instance.toggleWizard();
        expect(wizardToggleDisplay).toHaveBeenCalled();
    });

    it('should create tile', () => {
        const dashboardTile = {
            version: '1.0.0',
            id: 'apicatalog',
            title: 'API Mediation Layer API',
            status: 'UP',
            description: 'lkajsdlkjaldskj',
            services: [
                {
                    serviceId: 'apicatalog',
                    title: 'API Catalog',
                    description:
                        'API ML Microservice to locate and display API documentation for API ML discovered microservices',
                    status: 'UP',
                    secured: false,
                    homePageUrl: '/ui/v1/apicatalog',
                },
            ],
            totalServices: 1,
            activeServices: 1,
            lastUpdatedTimestamp: '2018-08-22T08:32:03.110+0000',
            createdTimestamp: '2018-08-22T08:31:22.948+0000',
        };

        const dashboard = shallow(
            <Dashboard
                tiles={[dashboardTile]}
                fetchTilesStart={jest.fn()}
                fetchTilesStop={jest.fn()}
                clearService={jest.fn()}
                clear={jest.fn()}
                assertAuthorization={jest.fn()}
            />
        );
        const tile = dashboard.find('Tile');
        expect(tile.length).toEqual(1);
    });
});
