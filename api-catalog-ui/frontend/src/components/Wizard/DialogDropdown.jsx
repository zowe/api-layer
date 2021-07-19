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
import { IconArrowDropDown } from 'mineral-ui-icons';
import { Dropdown } from 'mineral-ui';
import Button from 'mineral-ui/Button';
import './wizard.css';

export default class DialogDropdown extends Component {
    constructor(props) {
        super(props);
        this.state = {
            data: this.props.data,
        };
    }

    componentDidMount() {
        this.openOnClick();
    }

    openOnClick() {
        const { data } = this.state;
        if (Array.isArray(data)) {
            this.setState({ data: data.map(item => ({ ...item, onClick: this.props.toggleWizard })) });
        }
    }

    renderDropdown() {
        if (this.props.WIP || !Array.isArray(this.state.data)) {
            return null;
        }
        return (
            <Dropdown iconEnd={<IconArrowDropDown />} data={this.state.data}>
                <Button id="wizard-YAML-button" iconEnd={<IconArrowDropDown />}>
                    Onboard New API
                </Button>
            </Dropdown>
        );
    }

    render() {
        return this.renderDropdown();
    }
}
