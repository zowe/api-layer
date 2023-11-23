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
import { jest } from '@jest/globals';
import ErrorDialog from './ErrorDialog';

describe('>>> ErrorDialog component tests', () => {
    it('should render the ErrorDialog if there is an error while refreshing apis', () => {
        const messageText = {
            messageContent: 'Authentication service is not available by URL',
            messageKey: 'org.zowe.apiml.apiCatalog.serviceNotFound',
            messageNumber: 'ZWEAC706E',
            messageType: 'ERROR',
        };
        const wrapper = shallow(<ErrorDialog refreshedStaticApisError={messageText} clearError={jest.fn()} />);
        expect(wrapper.find('[data-testid="dialog-content"]').exists()).toEqual(true);
    });

    it('should not render the ErrorDialog if there is not an error while refreshing apis', () => {
        const wrapper = shallow(<ErrorDialog refreshedStaticApisError={null} clearError={jest.fn()} />);
        expect(wrapper.find('DialogBody').exists()).toEqual(false);
    });
});
