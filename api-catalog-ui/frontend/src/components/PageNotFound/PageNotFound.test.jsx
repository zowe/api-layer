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
import PageNotFound from './PageNotFound';

describe('>>> Detailed Page component tests', () => {
    it('should handle a dashboard button click', () => {
        const historyMock = { push: jest.fn() };
        const wrapper = shallow(<PageNotFound history={historyMock} />);
        wrapper.find('[data-testid="go-home-button"]').simulate('click');
        expect(historyMock.push.mock.calls[0]).toEqual(['/dashboard']);
    });
});
