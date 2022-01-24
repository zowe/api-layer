/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { Component } from 'react';
import { debounce } from 'lodash';
import { TextInput } from 'mineral-ui';
import IconSearch from 'mineral-ui-icons/IconSearch';
import { IconClear } from 'mineral-ui-icons';
import './search.css';

export default class SearchCriteria extends Component {
    // eslint-disable-next-line react/sort-comp
    raiseDoSearchWhenUserStoppedTyping = debounce(() => {
        const { criteria } = this.state;
        const { doSearch } = this.props;
        doSearch(criteria);
    }, 300);

    constructor(props) {
        super(props);
        this.state = {
            criteria: '',
        };
        this.handleCriteriaChange = this.handleCriteriaChange.bind(this);
        this.clearSearch = this.clearSearch.bind(this);
    }

    handleCriteriaChange(e) {
        this.setState({ criteria: e.currentTarget.value }, () => {
            this.raiseDoSearchWhenUserStoppedTyping();
        });
    }

    clearSearch() {
        this.setState({ criteria: '' }, () => {
            this.raiseDoSearchWhenUserStoppedTyping();
        });
    }

    render() {
        const { criteria } = this.state;
        const { placeholder } = this.props;
        const iconSearch = <IconSearch />;
        const iconClear = (
            <IconClear data-testid="clear-button" className="clear-text-search" onClick={this.clearSearch} />
        );
        const icon = criteria.length === 0 ? iconSearch : iconClear;
        return (
            <TextInput
                className="search-bar"
                data-testid="search-bar"
                iconEnd={icon}
                placeholder={placeholder}
                value={criteria}
                onChange={this.handleCriteriaChange}
            />
        );
    }
}
