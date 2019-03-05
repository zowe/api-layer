import {CLEAR_SERVICE, SELECT_SERVICE} from '../constants/selected-service-constants';

export function selectService(selectedService = {}, selectedTile = "") {
    return {
        type: SELECT_SERVICE,
        selectedService: selectedService,
        selectedTile: selectedTile
    };
}

export function clearService() {
    return {
        type: CLEAR_SERVICE,
        selectedService: {},
        selectedTile: ""
    };
}
