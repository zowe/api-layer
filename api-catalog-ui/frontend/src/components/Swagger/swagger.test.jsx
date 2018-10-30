/* eslint-disable no-undef */
import * as React from 'react';
import { shallow, mount } from 'enzyme';
import SwaggerUI from './swagger';

describe('>>> Swagger component tests', () => {
    it('should render wrapper div', () => {
        const wrapper = shallow(
            <div>
                <SwaggerUI />
            </div>
        );
        const swaggerDiv = wrapper.find('#swaggerContainer');

        expect(swaggerDiv).toBeDefined();
    });
});
