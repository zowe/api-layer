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
import FeedbackForm from './FeedbackForm';
import configureStore from 'redux-mock-store';
import { Provider } from 'react-redux';

const mockStore = configureStore();

describe('Feedback Button Container', () => {
    let store;
    let container;
    beforeEach(() => {
        store = mockStore();
        const history = {
            push: jest.fn(),
        };
        container = render(
            <Provider store={store}>
                <FeedbackForm history={history} />
            </Provider>
        );
    });

    it('should render the container', () => {
        expect(container).not.toBeNull();
    });
});
