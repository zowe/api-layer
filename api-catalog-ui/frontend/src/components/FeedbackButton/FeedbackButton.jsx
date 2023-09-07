/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { Fab } from '@material-ui/core';
import { Component } from 'react';
import FeedbackImage from '../../assets/images/square-envelope.svg';
import formatError from '../Error/ErrorFormatter';
import { customUIStyle, isAPIPortal } from '../../utils/utilFunctions';

export default class FeedbackButton extends Component {
    // componentDidMount() {
    // }

    // componentWillUnmount() {
    // }

    // handleSearch = (value) => {
    // };

    // refreshStaticApis = () => {
    // };

    render() {
        const { 
            isLoading,
            noFloat
        } = this.props;

        return (
            <div className={noFloat ? "" : "floating-button"}>
                <Fab
                    variant="extended"
                    style={noFloat ? {} : {
                        position: 'absolute',
                        top: '85vh',
                        left: '70vw',
                        whiteSpace: 'nowrap',
                    }}
                    onClick={() => {
                        // eslint-disable-next-line no-console
                        document.body.classList.remove("mobile-menu-open");
                        console.log('feedback clicked');
                    }}
                >
                    { noFloat && <img alt="" src={FeedbackImage} className="feedback-img" style={{marginRight: "8px"}} /> }
                    Give us Feedback
                </Fab>
            </div>
        );
    }
}
