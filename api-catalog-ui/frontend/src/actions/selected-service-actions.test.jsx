/* eslint-disable no-undef */

import * as actions from './selected-service-actions'
import { CLEAR_SERVICE, SELECT_SERVICE } from '../constants/selected-service-constants';

describe('>>> Selected Service actions tests', () => {
    it('should return selected service', () => {
        const selectedService = {id: 'service'};
        const selectedTile = 'Tile';
        const expectedAction = {
            type: 'SELECT_SERVICE',
            selectedService: {id: 'service'},
            selectedTile: 'Tile'
        };
        expect(actions.selectService(selectedService, selectedTile)).toEqual(expectedAction);
    });

    it('should return clear service', () => {
        const expectedAction = {
            type: CLEAR_SERVICE,
            selectedService: {},
            selectedTile: ""
        };
        expect(actions.clearService()).toEqual(expectedAction);
    });
});
