/* eslint-disable no-undef */
import React from 'react';
import { shallow } from 'enzyme';

import Error from './Error';

describe('>>> Error component tests', () => {
    it('should display a styled Typography', () => {
        const errorText = 'my error';
        const sample = shallow(<Error text={errorText} />);

        const ErrorTypography = sample.find('WithStyles(WithStyles(ForwardRef(Typography)))');
        expect(ErrorTypography).toExist();
        expect(ErrorTypography.text().trim()).toEqual(errorText);
    });

    it('should display an element with id erroricon', () => {
        const sample = shallow(<Error />);
        expect(sample.find('[id="erroricon"]')).toExist();
    });
});
