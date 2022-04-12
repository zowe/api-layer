/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { toast } from 'react-toastify';
import { OVERRIDE_DEF, TOGGLE_DISPLAY, WIZARD_VISIBILITY_TOGGLE } from '../constants/wizard-constants';
import getBaseUrl from '../helpers/urls';

export function notifySuccess() {
    toast.success('Automatic onboarding successful!', {
        closeOnClick: true,
        autoClose: 2000,
    });
    setTimeout(() => {
        toast.dismiss();
    }, 2000);
    return {
        type: TOGGLE_DISPLAY,
    };
}

export function notifyError(error) {
    toast.error(error, {
        closeOnClick: true,
        autoClose: 2000,
    });
    setTimeout(() => {
        toast.dismiss();
    }, 2000);
    return {
        type: 'IGNORE',
        payload: null,
    };
}

/**
 * Toggle confirmation dialog
 */
export function confirmStaticDefOverride() {
    return {
        type: OVERRIDE_DEF,
        payload: null,
    };
}

/**
 * Communicates with the backend, to either save or override a YAML static definition.
 * @param yamlText valid YAML config
 * @param serviceId serviceId of the API
 * @param endpoint specifies whether to override or save the definition
 */
function yamlEndpointConnect(yamlText, serviceId, endpoint) {
    const url = `${getBaseUrl()}/static-api/${endpoint}`;
    return (dispatch) => {
        fetch(url, {
            method: 'POST',
            headers: {
                'Service-Id': serviceId,
            },
            body: yamlText,
        })
            .then((res) => {
                const { status } = res;
                if (status === 201) {
                    dispatch(notifySuccess());
                } else if (status === 409) {
                    dispatch(confirmStaticDefOverride());
                } else {
                    dispatch(notifyError('The automatic onboarding was unsuccessful..'));
                }
            })
            .catch(() => dispatch(notifyError('The automatic onboarding was unsuccessful..')));
    };
}

export function toggleWizardVisibility(state) {
    return {
        type: WIZARD_VISIBILITY_TOGGLE,
        payload: { state },
    };
}

/**
 * Assert logged user has authorization to save static definitions
 * @returns {(function(*): void)|*}
 */
export function assertAuthorization() {
    const url = `${process.env.REACT_APP_GATEWAY_URL}/gateway/auth/check`;
    const body = {
        resourceClass: 'ZOWE',
        resourceName: 'APIML.SERVICES',
        accessLevel: 'READ',
    };
    return (dispatch) => {
        fetch(url, {
            headers: {
                'Content-Type': 'application/json',
            },
            method: 'POST',
            body: JSON.stringify(body),
        })
            .then((res) => {
                const { status } = res;
                if (status === 204) {
                    dispatch(toggleWizardVisibility(true));
                } else {
                    dispatch(toggleWizardVisibility(false));
                }
            })
            .catch(() => dispatch(notifyError('Error while trying to establish authorization level..')));
    };
}

/**
 * Send request to override a static def with a certain serviceId
 * @param yamlText valid YAML config
 * @param serviceId serviceId of the API
 */
export function overrideStaticDef(yamlText, serviceId) {
    return yamlEndpointConnect(yamlText, serviceId, 'override');
}

/**
 *  Send request to save a static def with a certain serviceId
 * @param yamlText valid YAML config
 * @param serviceId serviceId of the API
 */
export function sendYAML(yamlText, serviceId) {
    return yamlEndpointConnect(yamlText, serviceId, 'generate');
}

/**
 * Notify a user that the file they have uploaded is invalid yaml
 */
export function notifyInvalidYamlUpload(message) {
    toast.warning(message, {
        closeOnClick: true,
        autoClose: 4000,
    });
    setTimeout(() => {
        toast.dismiss();
    }, 4000);
    return {
        type: 'IGNORE',
        payload: null,
    };
}
