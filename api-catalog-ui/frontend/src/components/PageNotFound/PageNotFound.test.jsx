/* eslint-disable no-undef */
import * as React from 'react';
// tslint:disable-next-line:no-implicit-dependencies
import { mount, shallow } from 'enzyme';
import PageNotFound from './PageNotFound';

describe('>>> Detailed Page component tests', () => {
    it('should handle a dashboard button click', () => {
        const historyMock = { push: jest.fn() };
        const wrapper = shallow(<PageNotFound history={historyMock} />);
        wrapper.find('Button').simulate('click');
        expect(historyMock.push.mock.calls[0]).toEqual(['/dashboard']);
    });
});
