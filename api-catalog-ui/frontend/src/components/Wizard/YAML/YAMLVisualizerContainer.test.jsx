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
import YAMLVisualizerContainer from './YAMLVisualizerContainer';

const mockStore = configureStore();

describe('YAMLVisualizer Container', () => {
    let store;
    let container;
    beforeEach(() => {
        store = mockStore({
            wizardReducer: {
                yamlObject: 'category',
                inputData: [],
            },
        });
        container = render(
            <Provider store={store}>
                <YAMLVisualizerContainer />
            </Provider>
        );
    });

    it('should render the container', () => {
        expect(container).not.toBeNull();
    });
});
