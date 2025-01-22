/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import React, { Component } from 'react';
import { InputAdornment, TextField } from '@material-ui/core';
import SearchIcon from '@material-ui/icons/Search';
import ClearIcon from '@material-ui/icons/Clear';

export default class SearchCriteria extends Component {

    constructor(props) {
        super(props);
        this.state = {
            criteria: '',
        };
        this.handleCriteriaChange = this.handleCriteriaChange.bind(this);
        this.clearSearch = this.clearSearch.bind(this);
    }

    doSearch() {
        const { doSearch } = this.props;
        const { criteria } = this.state;
        doSearch(criteria);
    }

    handleCriteriaChange(e) {
        this.setState({ criteria: e.currentTarget.value }, () => this.doSearch());
    }

    clearSearch() {
        this.setState({ criteria: '' }, () => this.doSearch());
    }

    render() {
        const { criteria } = this.state;
        const { placeholder } = this.props;
        const iconSearch = <SearchIcon id="search-icon" />;
        const iconClear = (
            <ClearIcon data-testid="clear-button" className="clear-text-search" onClick={this.clearSearch} />
        );
        const icon = criteria.length > 0 ? iconClear : '';
        return (
            <TextField
                className="search-bar"
                data-testid="search-bar"
                InputProps={{
                    disableUnderline: true,
                    endAdornment: (
                        <InputAdornment position="end" id="search-input">
                            {icon}
                        </InputAdornment>
                    ),
                    startAdornment: <InputAdornment position="end">{iconSearch}</InputAdornment>,
                }}
                placeholder={placeholder}
                value={criteria}
                onChange={this.handleCriteriaChange}
            />
        );
    }
}
