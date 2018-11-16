/* eslint-disable no-undef */

import { SELECT_SERVICE } from '../constants/selected-service-constants';
import selectedServiceReducer from './selected-service-reducer';

describe('>>> Selected Service reducer tests', () => {
    it('should return selected service', () => {
        const state = { selectedService: {id: 'one'} };
        const action = { type: 'SELECT_SERVICE' };
        const expectedState = { selectedService: {id: 'one'} };
        expect(selectedServiceReducer(state, action)).toEqual(expectedState);
    });
});
