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
import '@testing-library/jest-dom';
import { Provider } from 'react-redux';
import configureStore from 'redux-mock-store';
import HeaderContainer from './HeaderContainer';

const mockStore = configureStore();

jest.mock(
    '../Icons/MetricsIconButton.jsx',
    () =>
        // eslint-disable-next-line react/display-name
        function () {
            const MetricsIconButton = 'MetricsIconButtonMock';
            return <MetricsIconButton />;
        }
);

jest.mock(
    './Header.jsx',
    () =>
        // eslint-disable-next-line react/display-name
        function () {
            const Header = 'HeaderMock';
            return <Header />;
        }
);

describe('Header Container', () => {
    let store;
    let container;
    const theme = {
        palette: {
            background: {
                main: 'red',
            },
        },
    };
    beforeEach(() => {
        store = mockStore({});

        container = render(
            <Provider store={store}>
                <HeaderContainer theme={theme} />
            </Provider>
        );
    });

    it('should render the container', () => {
        expect(container).not.toBeNull();
    });
});
