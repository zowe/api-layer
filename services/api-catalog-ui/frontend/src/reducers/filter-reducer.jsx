import {CLEAR_FILTER, FILTER_TEXT} from '../constants/filter-constants';

const filtersReducerDefaultState = {
    text: '',
};

const filtersReducer = (state = filtersReducerDefaultState, action) => {
    switch (action.type) {
        case FILTER_TEXT:
            return {
                ...state,
                text: action.text,
            };
        case CLEAR_FILTER:
            return {
                ...state,
                text: '',
            };
        default:
            return state;
    }
};

export default filtersReducer;
