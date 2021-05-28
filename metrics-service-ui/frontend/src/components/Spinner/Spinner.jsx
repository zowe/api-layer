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
import './Spinner.css';

export default class Spinner extends Component {
    render() {
        const { isLoading, css } = this.props;
        const divStyle = {
            display: isLoading === true ? 'block' : 'none',
            ...css,
        };
        return (
            <div id="spinner" className="lds-ring" style={divStyle}>
                <div />
                <div />
                <div />
                <div />
            </div>
        );
    }
}
