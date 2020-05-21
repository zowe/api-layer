/* eslint-disable no-undef */
import * as React from 'react';
// tslint:disable-next-line:no-implicit-dependencies
import ErrorDialog from "./ErrorDialog";
import { shallow } from 'enzyme';
import jest from 'jest-mock';

describe('>>> ErrorDialog component tests', () => {
    it('should render the ErrorDialog if there is an error while refreshing apis', () => {
        const messageText = {
            messageContent: "Authentication service is not available by URL",
            messageKey: 'org.zowe.apiml.apiCatalog.serviceNotFound',
            messageNumber: 'ZWEAC706E',
            messageType: 'ERROR',
        };
        const wrapper = shallow(
            <ErrorDialog
                refreshedStaticApisError={messageText}
                clearError={jest.fn()}
            />
        );
        expect(wrapper.find('DialogBody').exists()).toEqual(true);
    });
});
