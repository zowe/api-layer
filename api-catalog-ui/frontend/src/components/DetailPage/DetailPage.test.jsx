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
    description: 'Description of the tile',
    customStyleConfig: {},
    services: [
        {
            serviceId: 'apicatalog',
            title: 'API Catalog',
            description:
                'API ML Microservice to locate and display API documentation for API ML discovered microservices',
            tileDescription: 'Description of the tile',
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

jest.mock("../ServiceTab/ServiceTabContainer", () => ({
    __esModule: true,
   default: jest.fn(() => ({})),
}));

jest.mock('react-router', () => ({
    ...jest.requireActual('react-router'),
    useParams: () => ({ '*': 'mockServiceId' }), // Provide a valid serviceId
}));

describe('>>> Detailed Page component tests', () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    it('should start epic on mount', () => {
        const fetchNewService = jest.fn();

        renderWithProviders(<BrowserRouter>
                    <Routes>
                        <Route path="*" element={<DetailPage
                            tiles={[tile]}
                            services={tile.services}
                            currentTileId="apicatalog"
                            fetchNewService={fetchNewService}
                            fetchServiceStop={jest.fn()}
                        />}/>
                    </Routes>
            </BrowserRouter>
        );
        expect(fetchNewService).toHaveBeenCalled();
    });

    it('should stop epic on unmount', () => {
        const fetchServiceStop = jest.fn();
        const {unmount} = renderWithProviders(
            <BrowserRouter>
                <Routes>
                    <Route path="*" element={<DetailPage
                        tiles={[tile]}
                        fetchNewService={jest.fn()}
                        fetchServiceStop={fetchServiceStop}
                    />}/>
                </Routes>
            </BrowserRouter>
        );
        unmount();
        expect(fetchServiceStop).toHaveBeenCalled();
    });

    it('should handle a back button click', () => {
        renderWithProviders(
            <BrowserRouter>
                <Routes>
                    <Route path="*" element={<DetailPage
                        tiles={[tile]}
                        service={tile.services[0]}
                        currentTileId="apicatalog"
                        fetchNewService={jest.fn()}
                        fetchServiceStop={jest.fn()}
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
                        fetchNewService={jest.fn()}
                        fetchServiceStop={jest.fn()}
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
                        service={tile.services[0]}
                        currentTileId="apicatalog"
                        fetchTilesStart={jest.fn()}
                        fetchNewService={jest.fn()}
                        fetchServiceStop={jest.fn()}
                        isLoading={isLoading}
                    />}/>
                </Routes>
            </BrowserRouter>
        );
        const catalogDescription  = screen.getByText('Description of the tile');
        expect(catalogDescription).toBeInTheDocument();
    });

    it('should stop fetch service for 404 response code', () => {
        const isLoading = false;
        const fetchServiceStop = jest.fn();
        const fetchServiceError = {
            status: 404,
        };
        const {unmount} = renderWithProviders(
            <BrowserRouter>
                <Routes>
                    <Route path="*" element={<DetailPage
                        tiles={[tile]}
                        fetchTilesStart={jest.fn()}
                        fetchNewService={jest.fn()}
                        fetchServiceStop={fetchServiceStop}
                        fetchServiceError={fetchServiceError}
                        isLoading={isLoading}
                    />}/>
                </Routes>
            </BrowserRouter>
        );

        expect(fetchServiceStop).toHaveBeenCalled();
    });

    it('should stop fetch tiles for error message', () => {
        const isLoading = false;
        const fetchServiceStop = jest.fn();
        const fetchServiceError = {
            message: 'some message',
        };
        renderWithProviders(
            <BrowserRouter>
                <Routes>
                    <Route path="*" element={<DetailPage
                        tiles={[tile]}
                        fetchTilesStart={jest.fn()}
                        fetchNewService={jest.fn()}
                        fetchServiceStop={fetchServiceStop}
                        fetchServiceError={fetchServiceError}
                        isLoading={isLoading}
                    />}/>
                </Routes>
            </BrowserRouter>
        );
        expect(fetchServiceStop).toHaveBeenCalled();
    });


});
