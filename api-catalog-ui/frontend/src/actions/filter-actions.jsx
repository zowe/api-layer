import { CLEAR_FILTER, FILTER_TEXT } from '../constants/filter-constants';

export function filterText(text = '') {
    return {
        type: FILTER_TEXT,
        text
    };
}

export function clear() {
    return {
        type: CLEAR_FILTER,
        defaultFilter: ''
    };
}
