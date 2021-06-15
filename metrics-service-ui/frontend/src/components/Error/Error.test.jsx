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
