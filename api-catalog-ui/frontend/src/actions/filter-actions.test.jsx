/* eslint-disable no-undef */

import * as actions from './filter-actions'
import {CLEAR_FILTER, FILTER_TEXT} from "../constants/filter-constants";

describe('>>> Selected Service actions tests', () => {
    it('should create actions to clear text', () => {
        const expectedAction = {
            type: CLEAR_FILTER,
            defaultFilter: '',
        };

        expect(actions.clear()).toEqual(expectedAction);
    });

    it('should create actions to filter text', () => {
        const expectedText = "sampleText"
        const expectedAction = {
            type: FILTER_TEXT,
            text: expectedText,
        };

        expect(actions.filterText(expectedText)).toEqual(expectedAction);
    });
});
