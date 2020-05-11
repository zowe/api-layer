import { REFRESH_STATIC_APIS_ERROR } from '../constants/refresh-static-apis-constants';

export function refreshStaticApisError(error) {
    return {
        type: REFRESH_STATIC_APIS_ERROR,
        error
    }
}

export function refreshedStaticApi() {
    const refreshEndpoint = '/discovery/api/v1/staticApi';
    const url =
        'https://localhost:10010/api/v1/apicatalog' + refreshEndpoint;
    return dispatch => {
        fetch(url, {
            method: 'POST'
        }).then(error => {
                dispatch(refreshStaticApisError(error));
            }
            )
            .catch(error => {
                dispatch(refreshStaticApisError(error));
            });
    }

}

