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

import Dashboard from './Dashboard';

describe('>>> Dashboard component tests', () => {
    it('should display the service name ', () => {
        const sample = shallow(<Dashboard />);
        expect(sample.find('[id="name"]').first().text()).toEqual('Metrics Service');
    });
});
