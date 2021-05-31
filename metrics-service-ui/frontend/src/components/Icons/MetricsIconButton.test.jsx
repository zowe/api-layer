/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

/* eslint-disable no-undef */

import React from 'react';
import { shallow } from 'enzyme';

import MetricsIconButton from './MetricsIconButton';

describe('>>> MetricsIconButton component tests', () => {
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
