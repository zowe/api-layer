import { CLEAR_SERVICE, SELECT_SERVICE } from '../constants/selected-service-constants';

export function selectService(service = {}, tileId = "") {
    return {
        type: SELECT_SERVICE,
        selectedService: service,
        selectedTile: tileId
    };
}

export function clearService() {
    return {
        type: CLEAR_SERVICE,
        selectedService: {},
        selectedTile: ""
    };
}
