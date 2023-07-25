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
import DetailPage from './DetailPage';

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

const match = {
    path: '/service',
};

describe('>>> Detailed Page component tests', () => {
    it('should start epic on mount', () => {
        const fetchTilesStart = jest.fn();
        const fetchNewTiles = jest.fn();
        const history = {
            push: jest.fn(),
            pathname: jest.fn(),
        };
        const wrapper = shallow(
            <DetailPage
                tiles={[tile]}
                services={tile.services}
                currentTileId="apicatalog"
                fetchTilesStart={fetchTilesStart}
                fetchNewTiles={fetchNewTiles}
                fetchTilesStop={jest.fn()}
                match={match}
                history={history}
            />
        );
        const instance = wrapper.instance();
        instance.componentDidMount();
        expect(fetchTilesStart).toHaveBeenCalled();
    });

    it('should stop epic on unmount', () => {
        const fetchTilesStop = jest.fn();
        const history = {
            push: jest.fn(),
            pathname: jest.fn(),
        };
        const wrapper = shallow(
            <DetailPage
                tiles={[tile]}
                fetchNewTiles={jest.fn()}
                fetchTilesStart={jest.fn()}
                fetchTilesStop={fetchTilesStop}
                match={match}
                history={history}
            />
        );
        const instance = wrapper.instance();
        instance.componentWillUnmount();
        expect(fetchTilesStop).toHaveBeenCalled();
    });

    it('should handle a back button click', () => {
        const historyMock = { push: jest.fn() };
        const wrapper = shallow(
            <DetailPage
                tiles={[tile]}
                services={tile.services}
                currentTileId="apicatalog"
                fetchTilesStart={jest.fn()}
                fetchNewTiles={jest.fn()}
                fetchTilesStop={jest.fn()}
                history={historyMock}
                match={match}
            />
        );
        wrapper.find('[data-testid="go-back-button"]').simulate('click');
        expect(historyMock.push.mock.calls[0]).toEqual(['/dashboard']);
    });

    it('should load spinner when waiting for data', () => {
        const isLoading = true;
        const wrapper = shallow(
            <DetailPage
                tiles={[tile]}
                fetchTilesStart={jest.fn()}
                fetchNewTiles={jest.fn()}
                fetchTilesStop={jest.fn()}
                match={match}
                isLoading={isLoading}
            />
        );
        const spinner = wrapper.find('Spinner');
        expect(spinner.props().isLoading).toEqual(true);
    });

    it('should display tile title', () => {
        const historyMock = { push: jest.fn() };
        const isLoading = false;
        const wrapper = shallow(
            <DetailPage
                tiles={[tile]}
                services={tile.services}
                currentTileId="apicatalog"
                fetchTilesStart={jest.fn()}
                fetchNewTiles={jest.fn()}
                fetchTilesStop={jest.fn()}
                history={historyMock}
                match={match}
                isLoading={isLoading}
            />
        );
        const title = wrapper.find('#title');
        expect(title.props().children).toEqual(tile.title);
    });

    it('should display tile description', () => {
        const historyMock = { push: jest.fn() };
        const isLoading = false;
        const wrapper = shallow(
            <DetailPage
                tiles={[tile]}
                services={tile.services}
                currentTileId="apicatalog"
                fetchTilesStart={jest.fn()}
                fetchNewTiles={jest.fn()}
                fetchTilesStop={jest.fn()}
                history={historyMock}
                match={match}
                isLoading={isLoading}
            />
        );
        const title = wrapper.find('#description');
        expect(title.props().children).toEqual(tile.description);
    });

    it('should set comms failed message when there is a Tile fetch 404 or 500 error', () => {
        const historyMock = { push: jest.fn() };
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
                history={historyMock}
                fetchTilesError={fetchTilesError}
                match={match}
                isLoading={isLoading}
            />
        );
        expect(fetchTilesStop).toHaveBeenCalled();
    });

    it('should set comms failed message when there is a Tile fetch 404 or 500 error', () => {
        const historyMock = { push: jest.fn() };
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
                history={historyMock}
                fetchTilesError={fetchTilesError}
                match={match}
                isLoading={isLoading}
            />
        );
        expect(fetchTilesStop).toHaveBeenCalled();
    });

    it('should clear the selected service, stop and restart fetching if a different tile is selected ', () => {
        const historyMock = { push: jest.fn() };
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
                history={historyMock}
                fetchTilesError={fetchTilesError}
                match={match}
                isLoading={isLoading}
                selectedTile={selectedTile}
            />
        );
        expect(fetchTilesStop).toHaveBeenCalled();
        expect(clearService).toHaveBeenCalled();
        expect(fetchTilesStart).toHaveBeenCalled();
    });

    it('should display nav right menu', () => {
        process.env.REACT_APP_API_PORTAL = true;
        const fetchTilesStart = jest.fn();
        const fetchNewTiles = jest.fn();
        const history = {
            push: jest.fn(),
            pathname: jest.fn(),
        };
        const wrapper = shallow(
            <DetailPage
                tiles={[tile]}
                services={tile.services}
                currentTileId="apicatalog"
                fetchTilesStart={fetchTilesStart}
                fetchNewTiles={fetchNewTiles}
                fetchTilesStop={jest.fn()}
                match={match}
                history={history}
            />
        );
        expect(wrapper.find('#right-resources-menu').exists()).toEqual(true);
    });

    it('should click', () => {
        process.env.REACT_APP_API_PORTAL = true;
        const fetchTilesStart = jest.fn();
        const fetchNewTiles = jest.fn();
        const history = {
            push: jest.fn(),
            pathname: jest.fn(),
        };
        const mockHandleLinkClick = jest.fn();
        const mockEvent = { preventDefault: jest.fn() };
        const mockElementToView = { scrollIntoView: jest.fn() };
        document.querySelector = jest.fn().mockReturnValue(mockElementToView);
        const wrapper = shallow(
            <DetailPage
                tiles={[tile]}
                services={tile.services}
                currentTileId="apicatalog"
                handleLinkClick={mockHandleLinkClick}
                fetchTilesStart={fetchTilesStart}
                fetchNewTiles={fetchNewTiles}
                fetchTilesStop={jest.fn()}
                match={match}
                history={history}
            />
        );
        // Simulate a click event on the Link component, providing the id as the second argument
        wrapper.instance().handleLinkClick(mockEvent, '#swagger-label');
        expect(mockEvent.preventDefault).toHaveBeenCalled();
        expect(document.querySelector).toHaveBeenCalledWith('#swagger-label');
        expect(mockElementToView.scrollIntoView).toHaveBeenCalled();

        wrapper.instance().handleLinkClick(mockEvent, '#use-cases-label');
        expect(mockEvent.preventDefault).toHaveBeenCalled();
        expect(document.querySelector).toHaveBeenCalledWith('#use-cases-label');
        expect(mockElementToView.scrollIntoView).toHaveBeenCalled();

        wrapper.instance().handleLinkClick(mockEvent, '#videos-label');
        expect(mockEvent.preventDefault).toHaveBeenCalled();
        expect(document.querySelector).toHaveBeenCalledWith('#videos-label');
        expect(mockElementToView.scrollIntoView).toHaveBeenCalled();

        wrapper.instance().handleLinkClick(mockEvent, '#tutorials-label');
        expect(mockEvent.preventDefault).toHaveBeenCalled();
        expect(document.querySelector).toHaveBeenCalledWith('#tutorials-label');
        expect(mockElementToView.scrollIntoView).toHaveBeenCalled();
    });
});
