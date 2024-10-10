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
import { Typography } from '@material-ui/core';
import '../BigShield/BigShield.css';

export default class Shield extends Component {
    constructor(props) {
        super(props);
        this.state = {
            error: false,
            // eslint-disable-next-line react/no-unused-state
            info: null,
        };
    }

    componentDidCatch(error, info) {
        this.setState({
            error,
            // eslint-disable-next-line react/no-unused-state
            info,
        });
    }

    render() {
        const { title } = this.props;
        if (this.state.error) {
            return (
                <div style={{ width: '40%' }}>
                    <h4 style={{ color: '#de1b1b' }}>{title}</h4>
                    <input id="collapsible" className="toggle" type="checkbox" />
                    <label htmlFor="collapsible" className="lbl-toggle">
                        Display the error stack
                    </label>
                    <div className="collapsible-content">
                        <div className="content-inner">
                            <Typography variant="h5">
                                <pre style={{ textAlign: 'left' }}>
                                    <code>{this.state.error.stack}</code>
                                </pre>
                            </Typography>
                        </div>
                    </div>
                </div>
            );
        }
        return this.props.children;
    }
}
