/* eslint-disable no-undef */
import React from 'react';
import {shallow, mount} from 'enzyme/build';
import {MemoryRouter} from 'react-router-dom';
import App from './App';

describe('>>> App component tests', () = > {
    it('should call render',() =
>
{
    const history = {push: jest.fn()};
    const wrapper = shallow( < App
    history = {history}
    />);
    const instance = wrapper.instance();
    expect(instance).not.toBeNull();
}
)
;

it('should not show header on login route', () = > {
    const wrapper = mount(
        < MemoryRouter initialEntries = {['/login']} >
    < App / >
    < /MemoryRouter>
)
;
const header = wrapper.find('.header');

expect(header).toHaveLength(0);
})
;
})
;
