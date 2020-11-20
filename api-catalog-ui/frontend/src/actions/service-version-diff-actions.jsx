
export const REQUEST_VERSION_DIFF = 'REQUEST_VERSION_DIFF';
export const RECEIVE_VERSION_DIFF = 'RECEIVE_VERSION_DIFF';

export function getDiff(serviceId, version1, version2) {
    function request(serviceId, version1, version2) {
        return {
            type: REQUEST_VERSION_DIFF,
            serviceId,
            version1,
            version2,
        }
    }

    function receive(diffText) {
        return {
            type: RECEIVE_VERSION_DIFF,
            diffText,
        }
    }

    return dispatch => {
        dispatch(request(serviceId, version1, version2));

        return fetch(process.env.REACT_APP_GATEWAY_URL +
            process.env.REACT_APP_CATALOG_HOME +
            process.env.REACT_APP_APIDOC_UPDATE + 
            `/${serviceId}/${version1}/${version2}`)
            .then(response => {
                return response.text();
            })
            .then(text => {
                console.log(text);
                return dispatch(receive(text))
            })
            .catch(e => {
                console.log(e);
            })
    }
}