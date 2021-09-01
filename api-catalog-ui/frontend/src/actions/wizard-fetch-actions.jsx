import { toast } from 'react-toastify';
import { OVERRIDE_DEF, TOGGLE_DISPLAY, WIZARD_VISIBILITY_TOGGLE } from '../constants/wizard-constants';

export function notifySuccess() {
    toast.success('Automatic onboarding successful!', {
        closeOnClick: true,
        autoClose: 2000,
    });
    return {
        type: TOGGLE_DISPLAY,
    };
}

export function notifyError(error) {
    toast.error(error, {
        closeOnClick: true,
        autoClose: 2000,
    });
    return {
        type: 'IGNORE',
        payload: null,
    };
}

export function confirmStaticDefOverride() {
    return {
        type: OVERRIDE_DEF,
        payload: null,
    };
}

export function sendYAML(yamlText, serviceId) {
    const url = `${process.env.REACT_APP_GATEWAY_URL}${process.env.REACT_APP_CATALOG_HOME}/static-api/generate`;
    return dispatch => {
        fetch(url, {
            method: 'POST',
            headers: {
                'Service-Id': serviceId,
            },
            body: yamlText,
        })
            .then(res => {
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
    return dispatch => {
        fetch(url, {
            headers: {
                'Content-Type': 'application/json',
            },
            method: 'POST',
            body: JSON.stringify(body),
        })
            .then(res => {
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
