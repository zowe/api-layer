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
import { IconButton, Menu, MenuItem } from '@material-ui/core';
import './wizard.css';

export default class DialogDropdown extends Component {
    constructor(props) {
        super(props);
        this.state = {
            data: this.props.data,
            isOpen: false,
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
            const arr = data.map(item => ({ ...item, onClick: this.handleClick }));
            this.setState({ data: arr });
        }
    }

    openMenu() {
        this.setState({ isOpen: true });
    }

    closeMenu() {
        this.setState({ isOpen: false });
    }

    renderDropdown() {
        if (!this.props.visible || !Array.isArray(this.state.data)) {
            return null;
        }
        return (
            <div>
                <IconButton
                    aria-controls="wizard-menu"
                    aria-haspopup="true"
                    onClick={this.openMenu}
                    size="medium"
                    variant="outlined"
                >
                    Onboard New API
                </IconButton>
                <Menu id="wizard-menu" keepMounted open={this.state.isOpen} onClose={this.closeMenu}>
                    {this.state.data.map(itemType => (
                        <MenuItem onClick={this.handleClick}>{itemType.text}</MenuItem>
                    ))}
                </Menu>
            </div>
        );
    }

    render() {
        return this.renderDropdown();
    }
}
