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
import * as enzyme from 'enzyme';
import React from 'react';
import WizardDialog from './WizardDialog';
import { data } from './wizard_config';

describe('>>> WizardDialog tests', () => {
    it('should render the dialog if store value is true', () => {
        const wrapper = enzyme.shallow(<WizardDialog wizardToggleDisplay={jest.fn()} inputData={data} wizardIsOpen />);
        expect(wrapper.find('DialogBody').exists()).toEqual(true);
    });

    it('should close dialog on cancel', () => {
        const wizardToggleDisplay = jest.fn();
        const wrapper = enzyme.shallow(
            <WizardDialog
                tiles={null}
                fetchTilesStart={jest.fn()}
                wizardToggleDisplay={wizardToggleDisplay}
                fetchTilesStop={jest.fn()}
                clearService={jest.fn()}
                clear={jest.fn()}
            />
        );
        const instance = wrapper.instance();
        instance.closeWizard();
        expect(wizardToggleDisplay).toHaveBeenCalled();
    });

});
