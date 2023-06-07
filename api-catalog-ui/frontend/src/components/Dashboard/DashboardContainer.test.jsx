/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { render } from '@testing-library/react';
import '@testing-library/jest-dom/extend-expect';
import { Provider } from 'react-redux';
import configureStore from 'redux-mock-store';
import DashboardContainer from './DashboardContainer';

const mockStore = configureStore();
jest.mock(
    '../Wizard/ConfirmDialogContainer.jsx',
    () =>
        // eslint-disable-next-line react/display-name
        function () {
            const ConfirmDialogContainer = 'ConfirmDialogContainerMock';
            return <ConfirmDialogContainer />;
        }
);

jest.mock(
    '../Wizard/WizardContainer.jsx',
    () =>
        // eslint-disable-next-line react/display-name
        function () {
            const WizardContainer = 'WizardContainerMock';
            return <WizardContainer />;
        }
);

describe('Dashboard Container', () => {
    let store;
    let container;
    beforeEach(() => {
        const tiles = [{ title: 'test', id: '2', description: 'test', services: [{ title: 'test' }] }];
        store = mockStore({
            filtersReducer: {
                text: 'test',
            },
            tilesReducer: {
                tiles,
                services: tiles,
                error: null,
            },
            refreshStaticApisReducer: {
                error: {},
                refreshTimestamp: '',
            },
            authenticationReducer: {
                error: null,
            },
        });
        const history = {
            push: jest.fn(),
        };
        container = render(
            <Provider store={store}>
                <DashboardContainer history={history} />
            </Provider>
        );
    });

    it('should render the container', () => {
        expect(container).not.toBeNull();
    });
});
