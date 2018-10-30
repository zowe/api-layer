/* eslint-disable no-undef */
import React from 'react';
import { shallow } from 'enzyme/build';
import App from './App';

describe('>>> App component tests', () => {
    it('should call render', () => {
        const authentication = {
            showHeader: true,
        };
        const history = { push: jest.fn() };
        const wrapper = shallow(<App authentication={authentication} history={history} />);
        const instance = wrapper.instance();
        expect(instance).not.toBeNull();
    });
});
