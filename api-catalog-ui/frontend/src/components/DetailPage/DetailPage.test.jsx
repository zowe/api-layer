/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it} from '@jest/globals';
import DetailPage from './DetailPage';
import {BrowserRouter, HashRouter, Route, Routes} from "react-router";
import PageNotFound from "../PageNotFound/PageNotFound";
import {Provider} from "react-redux";

Object.defineProperty(global, 'performance', {
    writable: true,
});

const tile = {
    version: '1.0.0',
    id: 'apicatalog',
    title: 'API Mediation Layer for z/OS internal API services',
    status: 'UP',
    description: 'lkajsdlkjaldskj',
    customStyleConfig: {},
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

const mockNavigate = jest.fn();
jest.mock('react-router', () => {
    return {
        __esModule: true,
        ...jest.requireActual('react-router'),
        useNavigate: () => mockNavigate,
    };
});
describe('>>> Detailed Page component tests', () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    it('should start epic on mount', () => {
        const fetchTilesStart = jest.fn();
        const fetchNewTiles = jest.fn();

        render(<BrowserRouter>
                <Provider store={store}>
                    <Routes>
                        <Route path="*" element={<DetailPage
                            tiles={[tile]}
                            services={tile.services}
                            currentTileId="apicatalog"
                            fetchTilesStart={fetchTilesStart}
                            fetchNewTiles={fetchNewTiles}
                            fetchTilesStop={jest.fn()}
                        />}/>
                    </Routes>
                </Provider>
            </BrowserRouter>
        );
        expect(fetchTilesStart).toHaveBeenCalled();
    });

    it('should stop epic on unmount', () => {
        const fetchTilesStop = jest.fn();
        const wrapper = shallow(
            <DetailPage
                tiles={[tile]}
                fetchNewTiles={jest.fn()}
                fetchTilesStart={jest.fn()}
                fetchTilesStop={fetchTilesStop}
            />
        );
        const instance = wrapper.instance();
        instance.componentWillUnmount();
        expect(fetchTilesStop).toHaveBeenCalled();
    });

    it('should handle a back button click', () => {
        const wrapper = shallow(
            <DetailPage
                tiles={[tile]}
                services={tile.services}
                currentTileId="apicatalog"
                fetchTilesStart={jest.fn()}
                fetchNewTiles={jest.fn()}
                fetchTilesStop={jest.fn()}
            />
        );
        wrapper.find('[data-testid="go-back-button"]').simulate('click');
    });

    it('should load spinner when waiting for data', () => {
        const isLoading = true;
        const wrapper = shallow(
            <DetailPage
                tiles={[tile]}
                fetchTilesStart={jest.fn()}
                fetchNewTiles={jest.fn()}
                fetchTilesStop={jest.fn()}
            />
        );
        const spinner = wrapper.find('Spinner');
        expect(spinner.props().isLoading).toEqual(true);
    });

    it('should display tile title', () => {
        const isLoading = false;
        const wrapper = shallow(
            <DetailPage
                tiles={[tile]}
                services={tile.services}
                currentTileId="apicatalog"
                fetchTilesStart={jest.fn()}
                fetchNewTiles={jest.fn()}
                fetchTilesStop={jest.fn()}
                isLoading={isLoading}
            />
        );
        const title = wrapper.find('#title');
        expect(title.props().children).toEqual(tile.title);
    });

    it('should display tile description', () => {
        const isLoading = false;
        const wrapper = shallow(
            <DetailPage
                tiles={[tile]}
                services={tile.services}
                currentTileId="apicatalog"
                fetchTilesStart={jest.fn()}
                fetchNewTiles={jest.fn()}
                fetchTilesStop={jest.fn()}
                isLoading={isLoading}
            />
        );
        const title = wrapper.find('#description');
        expect(title.props().children).toEqual(tile.description);
    });

    it('should set comms failed message when there is a Tile fetch 404 or 500 error', () => {
        const isLoading = false;
        const fetchTilesStop = jest.fn();
        const fetchTilesError = {
            status: 404,
        };
        shallow(
            <DetailPage
                tiles={[tile]}
                fetchTilesStart={jest.fn()}
                fetchNewTiles={jest.fn()}
                fetchTilesStop={fetchTilesStop}
                fetchTilesError={fetchTilesError}
                isLoading={isLoading}
            />
        );
        expect(fetchTilesStop).toHaveBeenCalled();
    });

    it('should set comms failed message when there is a Tile fetch 404 or 500 error', () => {
        const isLoading = false;
        const fetchTilesStop = jest.fn();
        const fetchTilesError = {
            message: 'some message',
        };
        shallow(
            <DetailPage
                tiles={[tile]}
                fetchTilesStart={jest.fn()}
                fetchNewTiles={jest.fn()}
                fetchTilesStop={fetchTilesStop}
                fetchTilesError={fetchTilesError}
                isLoading={isLoading}
            />
        );
        expect(fetchTilesStop).toHaveBeenCalled();
    });

    it('should clear the selected service, stop and restart fetching if a different tile is selected ', () => {
        const isLoading = false;
        const fetchTilesError = null;
        const fetchTilesStop = jest.fn();
        const fetchTilesStart = jest.fn();
        const clearService = jest.fn();
        const selectedTile = 'apicatalog';
        shallow(
            <DetailPage
                tiles={[tile]}
                clearService={clearService}
                fetchTilesStart={fetchTilesStart}
                fetchNewTiles={jest.fn()}
                fetchTilesStop={fetchTilesStop}
                fetchTilesError={fetchTilesError}
                isLoading={isLoading}
                selectedTile={selectedTile}
            />
        );
        expect(fetchTilesStop).toHaveBeenCalled();
        expect(clearService).toHaveBeenCalled();
        expect(fetchTilesStart).toHaveBeenCalled();
    });

    it('should scroll into view when selectedContentAnchor prop is updated', () => {
        jest.useFakeTimers();

        const fetchTilesStart = jest.fn();
        const fetchNewTiles = jest.fn();

        const wrapper = shallow(
            <DetailPage
                tiles={[tile]}
                services={tile.services}
                currentTileId="apicatalog"
                fetchTilesStart={fetchTilesStart}
                fetchNewTiles={fetchNewTiles}
                fetchTilesStop={jest.fn()}
                selectedContentAnchor="#id"
            />
        );

        const scrollIntoViewMock = jest.fn();
        const elementMock = {scrollIntoView: scrollIntoViewMock};
        const spyQuerySelector = jest.spyOn(document, 'querySelector').mockReturnValue(elementMock);

        wrapper.setProps({selectedContentAnchor: '#new-selected-content-anchor'});

        // Run all timers to execute the setTimeout
        jest.runAllTimers();

        expect(spyQuerySelector).toHaveBeenCalledWith('#new-selected-content-anchor');
        expect(scrollIntoViewMock).toHaveBeenCalled();

        jest.useRealTimers(); // restore the real timers
    });
});
