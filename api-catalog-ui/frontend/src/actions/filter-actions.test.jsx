/* eslint-disable no-undef */

import {CLEAR_FILTER, FILTER_TEXT} from "../constants/filter-constants";
import {filterText, clear} from "./filter-actions";

describe('>>> Selected Service actions tests', () => {
    it('should create actions to clear text', () => {
        const expectedAction = {
            type: CLEAR_FILTER,
            defaultFilter: '',
        };

        expect(clear()).toEqual(expectedAction);
    });

    it('should create actions to filter text', () => {
        const expectedText = "sampleText";
        const expectedAction = {
            type: FILTER_TEXT,
            text: expectedText,
        };

        expect(filterText(expectedText)).toEqual(expectedAction);
    });
});
