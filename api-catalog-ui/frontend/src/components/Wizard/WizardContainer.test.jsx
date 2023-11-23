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
import WizardContainer from './WizardContainer';

const mockStore = configureStore();

describe('Wizard Container', () => {
    let store;
    let container;
    beforeEach(() => {
        store = mockStore({
            wizardReducer: {
                wizardIsOpen: true,
                enablerName: 'spring enabler',
                selectedCategory: 'category',
                yamlObject: {},
                navsObj: 'obj',
                serviceId: 'service',
                userCanAutoOnboard: true,
                inputData: [],
                uploadedYamlTitle: false,
            },
        });
        container = render(
            <Provider store={store}>
                <WizardContainer versions={['v1', 'v2']} />
            </Provider>
        );
    });

    it('should render the container', () => {
        expect(container).not.toBeNull();
    });
});
