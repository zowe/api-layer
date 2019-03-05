/* eslint-disable no-undef */

import React from 'react';
import {cleanup, fireEvent, render, wait, waitForElement, within} from 'react-testing-library';
import {applyMiddleware, compose, createStore} from 'redux';
import {Provider} from 'react-redux';

import {createEpicMiddleware} from 'redux-observable';
import {ajax} from 'rxjs/ajax';
import {HashRouter} from 'react-router-dom';
import {ThemeProvider} from 'mineral-ui';
import {rootReducer} from '../reducers';
import {rootEpic} from '../epics';
import AppContainer from '../components/App/AppContainer';
import DashboardContainer from '../components/Dashboard/DashboardContainer';

function renderWithRedux(ui, {initialState, store = createStore(reducer, initialState)} = {}) {
    return {
        ...render(
            <HashRouter>
                <Provider store={store}>
                    <ThemeProvider>{ui}</ThemeProvider>
                </Provider>
            </HashRouter>
        ),
        // adding `store` to the returned utilities to allow us
        // to reference it in our tests (just try to avoid using
        // this to test implementation details).
        store,
    };
}

const epicMiddleware = createEpicMiddleware({
    dependencies: {ajax},
});
const composeEnhancers = compose;

const mockStore = createStore(rootReducer, composeEnhancers(applyMiddleware(epicMiddleware)));
epicMiddleware.run(rootEpic);

describe('>>> Integration tests', () => {
    afterEach(cleanup);

    xit('should render login page', async () => {
        const authentication = {
            showHeader: false,
        };
        const history = {
            push: jest.fn(),
            pathname: '/',
        };
        const {getByTestId, queryAllByTestId} = renderWithRedux(
            <AppContainer authentication={authentication} history={history}/>,
            {
                store: mockStore,
            }
        );

        await wait(() => getByTestId('login-form'));
        const loginForm = queryAllByTestId('login-form');
        expect(loginForm.length).toEqual(1);

        await wait(() => getByTestId('username'));
        const username = queryAllByTestId('username');
        expect(username.length).toEqual(1);

        await wait(() => getByTestId('password'));
        const password = queryAllByTestId('password');
        expect(password.length).toEqual(1);
    });

    xit('should login user', async () => {
        const authentication = {
            showHeader: false,
        };
        const history = {
            push: jest.fn(),
            pathname: '/',
        };
        const {getByTestId, queryAllByTestId} = renderWithRedux(
            <AppContainer authentication={authentication} history={history}/>,
            {
                store: mockStore,
            }
        );

        await wait(() => getByTestId('login-form'));
        const loginForm = queryAllByTestId('login-form');
        expect(loginForm.length).toEqual(1);

        const username = getByTestId('username');
        fireEvent.change(username, {target: {value: 'user'}});
        const password = getByTestId('password');
        fireEvent.change(password, {target: {value: 'user'}});
        const submitButton = getByTestId('submit');
        fireEvent.click(submitButton);
    });

    xit('should render dashboard and show tiles', async () => {
        const authentication = {
            showHeader: true,
        };
        const history = {
            push: jest.fn(),
            pathname: '/dashboard',
        };
        const {getByText, container, getByTestId, queryAllByTestId} = renderWithRedux(
            <DashboardContainer authentication={authentication} history={history}/>,
            {
                store: mockStore,
            }
        );

        await wait(() => getByText('Available API services'));
        expect(container.querySelector('.api-heading').textContent).toEqual('Available API services');
        await wait(() => getByTestId('tile'));
        const tiles = queryAllByTestId('tile');

        expect(tiles.length).toEqual(7);
    });

    xit('should search tiles', async () => {
        const authentication = {
            showHeader: true,
        };
        const history = {
            push: jest.fn(),
            pathname: '/dashboard',
        };
        const {queryAllByTestId, getByTestId} = renderWithRedux(
            <DashboardContainer authentication={authentication} history={history}/>,
            {
                store: mockStore,
            }
        );

        await wait(() => getByTestId('search-bar'));
        const searchBar = getByTestId('search-bar');
        expect(searchBar.length).toEqual(1);
        const searchCriteria = 'API Mediation Layer API';
        fireEvent.change(searchBar, {target: {value: searchCriteria}});

        await wait(() => getByTestId('tile'));
        const tiles = queryAllByTestId('tile');
        expect(tiles.length).toEqual(1);
    });

    xit('should display util message on irrelevant search criteria', async () => {
        const authentication = {
            showHeader: true,
        };
        const history = {
            push: jest.fn(),
            pathname: '/dashboard',
        };
        const {getByPlaceholderText, queryAllByTestId, getByText, getByTestId} = renderWithRedux(
            <DashboardContainer authentication={authentication} history={history}/>,
            {
                store: mockStore,
            }
        );

        await wait(() => getByPlaceholderText('Search for APIs'));
        const searchBar = getByPlaceholderText('Search for APIs');
        const searchCriteria = "Don't panic!";
        fireEvent.change(searchBar, {target: {value: searchCriteria}});

        await wait(() => getByText('No tiles found matching search criteria'));
        let tiles = queryAllByTestId('tile');
        expect(tiles.length).toEqual(0);

        const clearButton = getByTestId('clear-button');
        fireEvent.click(clearButton);

        await wait(() => getByTestId('tile'));
        tiles = queryAllByTestId('tile');
        expect(tiles.length).toEqual(7);
    });

    xit('should show detail page with swagger-ui after clicking  tile', async () => {
        const authentication = {
            showHeader: true,
        };
        const history = {
            push: jest.fn(),
            pathname: '/dashboard',
        };
        const {queryAllByTestId, getByText, getByTestId} = renderWithRedux(
            <DashboardContainer authentication={authentication} history={history}/>,
            {
                store: mockStore,
            }
        );

        await wait(() => getByTestId('tile'));
        const tile = queryAllByTestId('tile')[0];
        const tileTitle = within(tile).getByTestId('tile-title').textContent;

        fireEvent.click(tile);

        await wait(() => getByText(tileTitle));
        const detailPageTitle = getByText(tileTitle);
        await wait(() => getByTestId('swagger'));
        const swaggerContainer = getByTestId('swagger');
        await wait(() => getByText('Back'));
        const backButton = getByText('Back');

        expect(detailPageTitle).toBeTruthy();
        expect(swaggerContainer).toBeTruthy();
        expect(backButton).toBeTruthy();

        fireEvent.click(backButton);

        await wait(() => getByText('Available API services'));
        const dashboard = getByText('Available API services');

        expect(dashboard).toBeTruthy();
    });

    xit('should change status of tiles', async () => {
        const authentication = {
            showHeader: true,
        };
        const history = {
            push: jest.fn(),
            pathname: '/dashboard',
        };
        const {getByTestId, queryAllByText} = renderWithRedux(
            <DashboardContainer authentication={authentication} history={history}/>,
            {
                store: mockStore,
            }
        );

        await wait(() => getByTestId('tile'));
        const runningTiles = queryAllByText('All services are running');

        expect(runningTiles.length).toEqual(5);
        ajax.getJSON('/ui/v1/apicatalog/containers/some-down').subscribe(() => {
            const allRunningTiles = queryAllByText('All services are running');

            expect(allRunningTiles.length).toEqual(7);
        });
    });

    xit('should handle util when no apidoc is available', async () => {
        const authentication = {
            showHeader: true,
        };
        const history = {
            push: jest.fn(),
            pathname: '/dashboard',
        };
        const {getByText, getByTestId} = renderWithRedux(
            <DashboardContainer authentication={authentication} history={history}/>,
            {
                store: mockStore,
            }
        );

        await wait(() => getByText('Test API'));
        const failingTile = getByText('Test API');

        fireEvent.click(failingTile);

        await waitForElement(() => getByTestId('detail-page-util'));
        const errorMsg1 = getByTestId('detail-page-util').textContent;

        expect(errorMsg1).toEqual(
            'Tile details for "tile_tile1" could not be retrieved, the following util was returned...'
        );
    });
});
