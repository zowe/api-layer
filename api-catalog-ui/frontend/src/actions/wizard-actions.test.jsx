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
import * as constants from '../constants/wizard-constants';
import * as actions from './wizard-actions';

describe('>>> Wizard actions tests', () => {
    it('should change enablers', () => {
        const expectedAction = {
            type: constants.ENABLER_CHANGED,
            payload: null,
        };
        expect(actions.changedEnablers()).toEqual(expectedAction);
    });
    it('should toggle the wizard', () => {
        const expectedAction = {
            type: constants.TOGGLE_DISPLAY,
            payload: null,
        };
        expect(actions.wizardToggleDisplay()).toEqual(expectedAction);
    });
    it('should select enabler', () => {
        const expectedAction = {
            type: constants.SELECT_ENABLER,
            payload: { enablerName: 'Test' },
        };
        expect(actions.selectEnabler("Test")).toEqual(expectedAction);
    });
});
