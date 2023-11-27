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
import WizardInputsContainer from './WizardInputsContainer';

const mockStore = configureStore();

describe('WizardInputs Container', () => {
    let store;
    let container;
    beforeEach(() => {
        const tiles = [{ title: 'test', id: '2', description: 'test' }];
        store = mockStore({
            tilesReducer: {
                tiles,
            },
            wizardReducer: {
                inputData: [],
            },
        });
        const data = {
            multiple: true,
            isMinion: false,
        };
        container = render(
            <Provider store={store}>
                <WizardInputsContainer data={data} />
            </Provider>
        );
    });

    it('should render the container', () => {
        expect(container).not.toBeNull();
    });
});
