/* eslint-disable no-undef */
import React from 'react';
import { shallow } from 'enzyme';

import Dashboard from './Dashboard';

describe('>>> Dashboard component tests', () => {
    it('should display the service name ', () => {
        const sample = shallow(<Dashboard />);
        expect(sample.find('[id="name"]').first().text()).toEqual('Metrics Service');
    });
});
