/* eslint-disable no-undef */
import * as React from 'react';
// tslint:disable-next-line:no-implicit-dependencies
import { shallow } from 'enzyme';
import Dashboard from './Dashboard';

const ajaxError = {
    message: 'ajax Error 404',
    name: 'AjaxError',
    request: '',
    response: {message: 'error'},
    responseType: 'json',
    status: 404,
};

describe('>>> Dashboard component tests', () => {
    it('should display no results if search fails', () => {
        const dashboard = shallow(
            <Dashboard
                tiles={[]}
                searchCriteria=" Supercalafragalisticexpialadoshus"
                fetchTilesStart={jest.fn()}
                fetchTilesStop={jest.fn()}
                fetchTilesFailed={jest.fn()}
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
                fetchTilesStart={jest.fn()}
                fetchTilesStop={jest.fn()}
                fetchTilesFailed={jest.fn()}
            />
        );
        expect(
            dashboard
                .find('Text')
                .first()
                .children()
                .text()
        ).toEqual('Tile details could not be retrieved, the following error was returned:');
    });

    it('should stop epic on unmount', () => {
        const fetchTilesStop = jest.fn();
        const wrapper = shallow(<Dashboard tiles={null} fetchTilesStart={jest.fn()} fetchTilesStop={fetchTilesStop} />);
        const instance = wrapper.instance();
        instance.componentWillUnmount();
        expect(fetchTilesStop).toHaveBeenCalled();
    });

    it('should trigger filterText on handleSearch', () => {
        const filterText = jest.fn();
        const wrapper = shallow(
            <Dashboard tiles={null} fetchTilesStart={jest.fn()} filterText={filterText} fetchTilesStop={jest.fn()} />
        );
        const instance = wrapper.instance();
        instance.handleSearch();
        expect(filterText).toHaveBeenCalled();
    });

    it('should create tile', () => {
        const dashboardTile = {
            version: '1.0.0',
            id: 'apicatalog',
            title: 'API Mediation Layer for z/OS internal API services',
            status: 'UP',
            description: 'lkajsdlkjaldskj',
            services: [
                {
                    serviceId: 'apicatalog',
                    title: 'API Catalog',
                    description:
                        'MFaaS Microservice to locate and display API documentation for MFaaS discovered microservices',
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
            <Dashboard tiles={[dashboardTile]} fetchTilesStart={jest.fn()} fetchTilesStop={jest.fn()} />
        );
        const tile = dashboard.find('Tile');
        expect(tile.length).toEqual(1);
    });
});
