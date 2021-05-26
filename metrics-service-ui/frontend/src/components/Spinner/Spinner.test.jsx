/* eslint-disable no-undef */
import * as React from 'react';
import * as enzyme from 'enzyme';
import Spinner from './Spinner';

describe('>>> Spinner component tests', () => {
    it('should display spinner if isLoading is true', () => {
        const isLoading = true;
        const spinner = enzyme.shallow(<Spinner isLoading={isLoading} />);
        expect(spinner.prop('style').display).toEqual('block');
    });

    it('should not display spinner if isLoading is false', () => {
        const isLoading = false;
        const spinner = enzyme.shallow(<Spinner isLoading={isLoading} />);
        expect(spinner.prop('style').display).toEqual('none');
    });
});
