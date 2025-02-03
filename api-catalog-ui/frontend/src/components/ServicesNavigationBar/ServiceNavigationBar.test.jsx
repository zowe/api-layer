/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import ServicesNavigationBar from './ServicesNavigationBar';
import {BrowserRouter, Route, Routes, useLocation} from "react-router";
import {render, fireEvent, screen} from '@testing-library/react';
import '@testing-library/jest-dom';

const tile = {
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

const match = {
    url: '/service',
};

const mockNavigate = jest.fn();
// const mockLocation = jest.fn();
jest.mock('react-router', () => {
    return {
        __esModule: true,
        ...jest.requireActual('react-router'),
        useNavigate: () => mockNavigate,
        useLocation: jest.fn(),
    };
});

describe('>>> ServiceNavigationBar component tests', () => {

    it('should display no results if search fails', () => {
        const clear = jest.fn();
        useLocation.mockReturnValue({
            pathname: '/mock/path/1234',
        });
        render(
            <ServicesNavigationBar
                searchCriteria=" Supercalafragalisticexpialadoshus"
                services={[]}
                currentTileId="apicatalog"
                clear={clear}
            />
        );
        expect(screen.getByTestId('search-bar')).toBeInTheDocument();

    });

    it('should clear when unmounting', async () => {
        const clear = jest.fn();
        useLocation.mockReturnValue({
            pathname: '/mock/path/1234', // your test path
            search: '',                  // or any other fields you need
            hash: '',
            state: null,
            key: 'testkey',
        });
        const {unmount} = render(<BrowserRouter>
            <Routes>
                <Route path="*"
                       element={<ServicesNavigationBar clear={clear} services={[tile]} currentTileId="apicatalog"/>}/>
            </Routes>
        </BrowserRouter>)
        unmount();
        expect(clear).toHaveBeenCalled();
    });


    it('should display label', () => {
        useLocation.mockReturnValue({
            pathname: '/mock/path/1234',
        });
        const clear = jest.fn();
        render(
            <ServicesNavigationBar services={[]} currentTileId="apicatalog" clear={clear}/>
        );
        expect(screen.getByText('Product APIs')).toBeInTheDocument();

    });


});
