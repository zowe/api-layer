/* eslint-disable no-undef */

import { CLEAR_SERVICE, SELECT_SERVICE } from '../constants/selected-service-constants';
import {selectService, clearService} from "./selected-service-actions";

describe('>>> Selected Service actions tests', () => {
    it('should return selected service', () => {
        const selectedService = {id: 'service'};
        const selectedTile = 'Tile';
        const expectedAction = {
            type: SELECT_SERVICE,
            selectedService: {id: 'service'},
            selectedTile: 'Tile'
        };
        expect(selectService(selectedService, selectedTile)).toEqual(expectedAction);
    });

    it('should return clear service', () => {
        const expectedAction = {
            type: CLEAR_SERVICE,
            selectedService: {},
            selectedTile: ""
        };
        expect(clearService()).toEqual(expectedAction);
    });
});
