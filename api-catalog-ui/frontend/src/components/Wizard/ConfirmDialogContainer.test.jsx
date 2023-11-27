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
import ConfirmDialogContainer from './ConfirmDialogContainer';

const mockStore = configureStore();

describe('ConfirmDialog Container', () => {
    let store;
    let container;
    beforeEach(() => {
        store = mockStore({
            wizardReducer: {
                wizardIsOpen: true,
                yamlObject: {},
                serviceId: 'service',
                confirmDialog: false,
            },
        });
        container = render(
            <Provider store={store}>
                <ConfirmDialogContainer versions={['v1', 'v2']} />
            </Provider>
        );
    });

    it('should render the container', () => {
        expect(container).not.toBeNull();
    });
});
