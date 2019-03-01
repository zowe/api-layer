/* eslint-disable no-undef */

import {SELECT_SERVICE} from '../constants/selected-service-constants';
import selectedServiceReducer from './selected-service-reducer';

describe('>>> Selected Service reducer tests', () => {
    it('should return selected service', () => {
        const state = {selectedService: {id: 'one'}, selectedTile: 'aaaa'};
        const expectedState = {selectedService: {id: 'one'}, selectedTile: 'aaaa'};
        const action = {type: 'SELECT_SERVICE', selectedService: {id: 'one'}, selectedTile: 'aaaa'};
        expect(selectedServiceReducer(state, action)).toEqual(expectedState);
    });

    it('should return clear service', () => {
        const state = {selectedService: {id: 'one'}, selectedTile: 'aaaa'};
        const expectedState = {selectedService: {}, selectedTile: null};
        const action = {type: 'CLEAR_SERVICE'};
        expect(selectedServiceReducer(state, action)).toEqual(expectedState);
    });

    it('should return default state', () => {
        const state = {selectedService: {id: 'one'}, selectedTile: 'aaaa'};
        const action = {type: 'OOPS'};
        expect(selectedServiceReducer(state, action)).toEqual(state);
    });
});
