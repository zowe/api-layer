/* eslint-disable no-undef */

import React from 'react';
import * as enzyme from 'enzyme';
import {AjaxError} from 'rxjs/ajax/index';
import Error from './Error';
import {ApiError, MessageType} from '../../constants/error-constants';

describe('>>> Error component tests', () => {
    it('Should display the dialog and the error message for Api error', () => {
        const errorResponse = 'Could not determine error';
        const apiError = new ApiError('ABC123', 123, new MessageType(40, 'ERROR', 'E'), 'Bad stuff happened');
        const wrapper = enzyme.shallow(<Error errors={[apiError]}/>);
        expect(wrapper.find('DialogBody').exists()).toEqual(true);
        expect(wrapper.find('Text').prop('children')).toBe(errorResponse);
    });

    it('Should display the dialog and the error message for Ajax error', () => {
        const errorResponse = '404 : Fetch Failure';
        const ajaxError = new AjaxError(
            'API Error',
            {
                status: 404,
                response: {message: 'Fetch Failure'},
                responseType: 'json',
            },
            null
        );
        const wrapper = enzyme.shallow(<Error errors={[ajaxError]}/>);
        expect(wrapper.find('DialogBody').exists()).toEqual(true);
        expect(wrapper.find('Text').prop('children')).toBe(errorResponse);
    });

    it('Should not display the dialog if there are no errors', () => {
        const wrapper = enzyme.shallow(<Error errors={null}/>);
        expect(wrapper.find('DialogBody').exists()).toEqual(false);
    });
});
