import { toast } from 'react-toastify';
import { TOGGLE_DISPLAY } from '../constants/wizard-constants';

function sendYAMLSuccess() {
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
export function sendYAML(body) {
    const url = 'https://ece6cddf-5813-42b0-a436-26707651e5df.mock.pstmn.io/post-test';
    return dispatch => {
        fetch(url, { method: 'POST', body })
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
