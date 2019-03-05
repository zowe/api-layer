import {CLEAR_SERVICE, SELECT_SERVICE} from '../constants/selected-service-constants';

const defaultState = {
    selectedService: {},
    selectedTile: null,
};

const selectedServiceReducer = (state = defaultState, action) => {
    switch (action.type) {
        case SELECT_SERVICE:
            return {
                ...state,
                selectedService: action.selectedService,
                selectedTile: action.selectedTile,
            };
        case CLEAR_SERVICE:
            return {
                ...state,
                selectedService: {},
                selectedTile: null,
            };
        default:
            return state;
    }
};

export default selectedServiceReducer;
