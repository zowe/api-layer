import * as constants from '../constants/wizard-constants';
import * as actions from './wizard-fetch-actions';

describe('>>> Wizard actions tests', () => {
    it('should do nothing on YAML error', () => {
        const expectedAction = {
            type: 'IGNORE',
            payload: null,
        };
        expect(actions.notifyError()).toEqual(expectedAction);
    });

    it('should close on YAML success', () => {
        const expectedAction = {
            type: constants.TOGGLE_DISPLAY,
        };
        expect(actions.notifySuccess()).toEqual(expectedAction);
    });
});
