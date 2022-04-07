/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import * as constants from '../constants/wizard-constants';
import * as actions from './wizard-fetch-actions';
import { assertAuthorization, overrideStaticDef, sendYAML } from './wizard-fetch-actions';

describe('>>> Wizard actions tests', () => {
    it('should do nothing on YAML error', () => {
        const expectedAction = {
            type: 'IGNORE',
            payload: null,
        };
        expect(actions.notifyError()).toEqual(expectedAction);
    });

    it('should close on YAML success', () => {
        const expectedAction = {
            type: constants.TOGGLE_DISPLAY,
        };
        expect(actions.notifySuccess()).toEqual(expectedAction);
    });
    it('should confirm static definition override', () => {
        const expectedAction = {
            type: constants.OVERRIDE_DEF,
            payload: null,
        };
        expect(actions.confirmStaticDefOverride()).toEqual(expectedAction);
    });
    it('should toggle wizard visibility', () => {
        const expectedAction = {
            type: constants.WIZARD_VISIBILITY_TOGGLE,
            payload: { state: true },
        };
        expect(actions.toggleWizardVisibility(true)).toEqual(expectedAction);
    });
    it('should override static definition', () => {
        const yamlText = { test: 'hey' };
        const serviceId = 'Id';
        overrideStaticDef(yamlText, serviceId);
        const expectedAction = {
            type: constants.TOGGLE_DISPLAY,
        };
        expect(actions.notifySuccess()).toEqual(expectedAction);
    });
    it('should add static definition', () => {
        const yamlText = { test: 'hey' };
        const serviceId = 'Id';
        sendYAML(yamlText, serviceId);
        const expectedAction = {
            type: constants.TOGGLE_DISPLAY,
        };
        expect(actions.notifySuccess()).toEqual(expectedAction);
    });
    it('should assert authorization', () => {
        assertAuthorization();
        const expectedAction = {
            type: constants.WIZARD_VISIBILITY_TOGGLE,
            payload: { state: true },
        };
        expect(actions.toggleWizardVisibility(true)).toEqual(expectedAction);
    });
    it('should do nothing on YAML upload having invalid format', () => {
        const expectedAction = {
            type: 'IGNORE',
            payload: null,
        };
        expect(actions.notifyInvalidYamlUpload('test_message')).toEqual(expectedAction);
    });
});
