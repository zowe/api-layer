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
import { Button, Menu, MenuItem } from '@material-ui/core';
import KeyboardArrowDownIcon from '@material-ui/icons/KeyboardArrowDown';
import PropTypes from 'prop-types';

export default class DialogDropdown extends Component {
    constructor(props) {
        super(props);
        this.state = {
            data: this.props.data,
            isOpen: false,
            anchorEl: null,
        };
        this.handleClick = this.handleClick.bind(this);
        this.openMenu = this.openMenu.bind(this);
        this.closeMenu = this.closeMenu.bind(this);
    }

    componentDidMount() {
        this.openOnClick();
    }

    handleClick(event) {
        this.props.selectEnabler(event.target.innerText);
        this.props.toggleWizard();
        this.closeMenu();
    }

    openOnClick() {
        const { data } = this.state;
        if (Array.isArray(data)) {
            const arr = data.map((item) => ({ ...item, onClick: this.handleClick }));
            this.setState({ data: arr });
        }
    }

    openMenu(event) {
        this.setState({ isOpen: true, anchorEl: event.target });
    }

    closeMenu() {
        this.setState({ isOpen: false });
    }

    renderDropdown() {
        if (!this.props.visible || !Array.isArray(this.state.data)) {
            return null;
        }
        return (
            <span>
                <Button
                    aria-controls="wizard-menu"
                    aria-haspopup="true"
                    onClick={this.openMenu}
                    size="medium"
                    variant="outlined"
                    id="onboard-wizard-button"
                    style={{ borderRadius: '0.1875em' }}
                    endIcon={<KeyboardArrowDownIcon />}
                >
                    Onboard New API
                </Button>
                <Menu
                    id="wizard-menu"
                    keepMounted
                    open={this.state.isOpen}
                    onClose={this.closeMenu}
                    anchorEl={this.state.anchorEl}
                    getContentAnchorEl={null}
                    anchorOrigin={{
                        vertical: 'bottom',
                        horizontal: 'center',
                    }}
                    transformOrigin={{
                        vertical: 'top',
                        horizontal: 'center',
                    }}
                >
                    {this.state.data.map((itemType) => (
                        <MenuItem key={itemType.text} onClick={this.handleClick}>
                            {itemType.text}
                        </MenuItem>
                    ))}
                </Menu>
            </span>
        );
    }

    render() {
        return this.renderDropdown();
    }
}

DialogDropdown.propTypes = {
    props: PropTypes.shape({
        visible: PropTypes.bool,
    }).isRequired,
};
