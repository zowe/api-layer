/* eslint-disable no-undef */
import React from 'react';
import { shallow } from 'enzyme';

import ErrorIcon from './ErrorIcon';

describe('>>> ErrorIcon component tests', () => {
    it('should display a styled material UI ErrorIcon', () => {
        const sample = shallow(<ErrorIcon />);
        expect(sample.find('Memo(ForwardRef(ErrorIcon))')).toExist();
    });
});
