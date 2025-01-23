/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import {fireEvent, screen} from '@testing-library/react';
import {describe, expect, it} from '@jest/globals';
import DetailPage from './DetailPage';
import {BrowserRouter, Route, Routes} from "react-router";
import {renderWithProviders} from "../../helpers/test-utils";
import '@testing-library/jest-dom';

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

        renderWithProviders(<BrowserRouter>
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
            </BrowserRouter>
        );
        expect(fetchTilesStart).toHaveBeenCalled();
    });

    it('should stop epic on unmount', () => {
        const fetchTilesStop = jest.fn();
        const {unmount} = renderWithProviders(
            <BrowserRouter>
                <Routes>
                    <Route path="*" element={<DetailPage
                        tiles={[tile]}
                        fetchNewTiles={jest.fn()}
                        fetchTilesStart={jest.fn()}
                        fetchTilesStop={fetchTilesStop}
                    />}/>
                </Routes>
            </BrowserRouter>
        );
        unmount();
        expect(fetchTilesStop).toHaveBeenCalled();
    });

    it('should handle a back button click', () => {
        renderWithProviders(
            <BrowserRouter>
                <Routes>
                    <Route path="*" element={<DetailPage
                        tiles={[tile]}
                        services={tile.services}
                        currentTileId="apicatalog"
                        fetchTilesStart={jest.fn()}
                        fetchNewTiles={jest.fn()}
                        fetchTilesStop={jest.fn()}
                    />}/>
                </Routes>
            </BrowserRouter>
        );
        fireEvent.click( screen.getByTestId('go-back-button'));

    });

    it('should load spinner when waiting for data', () => {
        const isLoading = true;
        renderWithProviders(
            <BrowserRouter>
                <Routes>
                    <Route path="*" element={<DetailPage
                        tiles={[tile]}
                        fetchTilesStart={jest.fn()}
                        fetchNewTiles={jest.fn()}
                        fetchTilesStop={jest.fn()}
                        isLoading={isLoading}
                    />}/>
                </Routes>
            </BrowserRouter>
        );
        const spinner  = screen.getByTestId('spinner');
        expect(spinner).toBeInTheDocument();
    });

    it('should display tile title and description', () => {
        const isLoading = false;
        renderWithProviders(
            <BrowserRouter>
                <Routes>
                    <Route path="*" element={<DetailPage
                        tiles={[tile]}
                        services={tile.services}
                        currentTileId="apicatalog"
                        fetchTilesStart={jest.fn()}
                        fetchNewTiles={jest.fn()}
                        fetchTilesStop={jest.fn()}
                        isLoading={isLoading}
                    />}/>
                </Routes>
            </BrowserRouter>
        );
        const catalogTile  = screen.getByText('API Catalog');
        expect(catalogTile).toBeInTheDocument();
        const catalogDescription  = screen.getByText('API ML Microservice to locate and display API documentation for API ML discovered microservices');
        expect(catalogDescription).toBeInTheDocument();
    });

    it('should stop fetch tiles for 404 response code', () => {
        const isLoading = false;
        const fetchTilesStop = jest.fn();
        const fetchTilesError = {
            status: 404,
        };
        renderWithProviders(
            <BrowserRouter>
                <Routes>
                    <Route path="*" element={<DetailPage
                        tiles={[tile]}
                        fetchTilesStart={jest.fn()}
                        fetchNewTiles={jest.fn()}
                        fetchTilesStop={fetchTilesStop}
                        fetchTilesError={fetchTilesError}
                        isLoading={isLoading}
                    />}/>
                </Routes>
            </BrowserRouter>
        );
        expect(fetchTilesStop).toHaveBeenCalled();
    });

    it('should stop fetch tiles for error message', () => {
        const isLoading = false;
        const fetchTilesStop = jest.fn();
        const fetchTilesError = {
            message: 'some message',
        };
        renderWithProviders(
            <BrowserRouter>
                <Routes>
                    <Route path="*" element={<DetailPage
                        tiles={[tile]}
                        fetchTilesStart={jest.fn()}
                        fetchNewTiles={jest.fn()}
                        fetchTilesStop={fetchTilesStop}
                        fetchTilesError={fetchTilesError}
                        isLoading={isLoading}
                    />}/>
                </Routes>
            </BrowserRouter>
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
        renderWithProviders(
            <BrowserRouter>
                <Routes>
                    <Route path="*" element={<DetailPage
                        tiles={[tile]}
                        clearService={clearService}
                        fetchTilesStart={fetchTilesStart}
                        fetchNewTiles={jest.fn()}
                        fetchTilesStop={fetchTilesStop}
                        fetchTilesError={fetchTilesError}
                        isLoading={isLoading}
                        selectedTile={selectedTile}
                    />}/>
                </Routes>
            </BrowserRouter>
        );
        expect(fetchTilesStop).toHaveBeenCalled();
        expect(clearService).toHaveBeenCalled();
        expect(fetchTilesStart).toHaveBeenCalled();
    });


});
