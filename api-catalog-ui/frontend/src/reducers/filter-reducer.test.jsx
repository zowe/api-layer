/* eslint-disable no-undef */
import { clear, filterText } from '../actions/filter-actions';
import { CLEAR_FILTER, FILTER_TEXT } from '../constants/filter-constants';
import filtersReducer from './filter-reducer';

describe('>>> Filter reducer tests', () => {
    it('should handle CLEAR_FILTER', () => {
        const expectedState = {
            text: '',
        };

        expect(filtersReducer({ text: 'Test' }, { type: CLEAR_FILTER, defaultFilter: '' })).toEqual(expectedState);
    });

    it('should handle FILTER_TEXT', () => {
        const expectedState = {
            text: 'Test',
        };

        expect(filtersReducer({ text: 'blem' }, { type: FILTER_TEXT, text: 'Test' })).toEqual(expectedState);
    });

    it('should handle DEFAULT', () => {
        const expectedState = {
            text: 'Test',
        };

        expect(filtersReducer({ text: 'Test' }, { type: 'UNKNOWN', text: 'Test' })).toEqual(expectedState);
    });
});
