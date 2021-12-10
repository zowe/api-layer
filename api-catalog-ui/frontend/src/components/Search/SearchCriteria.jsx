import React, { Component } from 'react';
import { debounce } from 'lodash';
import { InputAdornment, TextField } from '@material-ui/core';
import SearchIcon from '@material-ui/icons/Search';
import ClearIcon from '@material-ui/icons/Clear';
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
        const iconSearch = <SearchIcon />;
        const iconClear = (
            <ClearIcon data-testid="clear-button" className="clear-text-search" onClick={this.clearSearch} />
        );
        const icon = criteria.length === 0 ? iconSearch : iconClear;
        return (
            <TextField
                className="search-bar"
                data-testid="search-bar"
                InputProps={{
                    endAdornment: <InputAdornment position="end">{icon}</InputAdornment>,
                }}
                placeholder={placeholder}
                value={criteria}
                onChange={this.handleCriteriaChange}
            />
        );
    }
}
