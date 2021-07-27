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
import * as enzyme from 'enzyme';
import WizardNavigation from './WizardNavigation';

xdescribe('>>> Wizard navigation tests', () => {
    it('should invoke next_category on click', () => {
        const next = jest.fn()
        const wrapper = enzyme.shallow(
            <WizardNavigation selectedCategory={0} nextWizardCategory={next} />
        );
        wrapper.find('#next').first().simulate('click');
        expect(next).toHaveBeenCalled();
    });
});
