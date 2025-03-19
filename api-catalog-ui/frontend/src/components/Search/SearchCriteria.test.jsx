/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import * as enzyme from 'enzyme';
import SearchCriteria from './SearchCriteria';

describe('>>> Search bar component tests', () => {
    it('should render all fields with initial state', () => {
        const wrapper = enzyme.shallow(<SearchCriteria />);
        expect(wrapper.find('.header')).toBeDefined();
        expect(wrapper.find('.search-bar')).toBeDefined();
        expect(wrapper.find('.clear-text-search')).toBeDefined();
        expect(wrapper.find('IconSearch')).toBeDefined();
        expect(wrapper.find('IconClear')).toBeDefined();
        expect(wrapper.find('TextInput')).toBeDefined();
    });

    it('should clear search criteria', () => {
        let searchedText = 'somethingBefore';
        const wrapper = enzyme.shallow(<SearchCriteria doSearch={(criteria) => {searchedText = criteria}} />);
        wrapper.setState({ criteria: 'foo' });
        const instance = wrapper.instance();
        instance.clearSearch();
        expect(wrapper.state().criteria).toEqual('');
        expect(searchedText).toEqual('');
    });

    it('should handle search criteria', () => {
        let searchedText = '';
        const wrapper = enzyme.shallow(<SearchCriteria doSearch={(criteria) => {searchedText = criteria}} />);
        wrapper.setState({ criteria: '' });
        const instance = wrapper.instance();
        const e = {
            currentTarget: {
                value: 'foo',
            },
        };
        instance.handleCriteriaChange(e);
        expect(wrapper.state().criteria).toEqual('foo');
        expect(searchedText).toEqual('foo');
    });

});
