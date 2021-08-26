import { toast } from 'react-toastify';
import { TOGGLE_DISPLAY } from '../constants/wizard-constants';

export function sendYAMLSuccess() {
    toast.success('Automatic onboarding successful!', {
        closeOnClick: true,
        autoClose: 2000,
    });
    return {
        type: TOGGLE_DISPLAY,
    };
}

export function sendYAMLError(error) {
    toast.error(error, {
        closeOnClick: true,
        autoClose: 2000,
    });
    return {
        type: 'IGNORE',
    };
}

// eslint-disable-next-line import/prefer-default-export
export function sendYAML(yamlText) {
    const url = `${process.env.REACT_APP_GATEWAY_URL}${process.env.REACT_APP_CATALOG_HOME}/static-api/autoOnboard`;
    return dispatch => {
        fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ yaml: yamlText }),
        })
            .then(res => {
                const { status } = res;
                if (status === 201) {
                    dispatch(sendYAMLSuccess());
                } else {
                    dispatch(sendYAMLError('The automatic onboarding was unsuccessful..'));
                }
            })
            .catch(() => dispatch(sendYAMLError('The automatic onboarding was unsuccessful..')));
    };
}
