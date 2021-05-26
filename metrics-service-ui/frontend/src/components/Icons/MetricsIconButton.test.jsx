/* eslint-disable no-undef */
import React from 'react';
import { shallow } from 'enzyme';

import MetricsIconButton from './MetricsIconButton';

describe('>>> ErrorIcon component tests', () => {
    const styledIconButton = 'WithStyles(WithStyles(ForwardRef(IconButton)))';

    it('should display a styled IconButton', () => {
        const sample = shallow(<MetricsIconButton />);
        expect(sample.find(styledIconButton)).toExist();
    });

    it('IconButton should have a href to metrics service dashboard', () => {
        const sample = shallow(<MetricsIconButton />);
        expect(sample.find(styledIconButton).prop('href')).toEqual('/metrics-service/ui/v1/#/dashboard');
    });

    it('should contain a Metrics Service icon image', () => {
        const sample = shallow(<MetricsIconButton />);
        expect(sample.find('img')).toExist();
        expect(sample.find('img').prop('alt')).toEqual('Metrics Service icon');
    });
});
