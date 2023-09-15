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
import FeedbackForm from '../FeedbackForm/FeedbackForm';

export default class FeedbackButton extends Component {
    constructor(props) {
        super(props);
        this.state = {
            isDialogOpen: false,
        };
        this.handleDialogClose = this.handleDialogClose.bind(this);
        this.handleDialogOpen = this.handleDialogOpen.bind(this);
    }

    handleDialogOpen = () => {
        this.setState({
            isDialogOpen: true,
        });
    };

    handleDialogClose = () => {
        this.setState({ isDialogOpen: false });
    };
    // componentDidMount() {
    // }

    // componentWillUnmount() {
    // }

    // handleSearch = (value) => {
    // };

    // refreshStaticApis = () => {
    // };

    render() {
        const { noFloat, leftPlacement } = this.props;
        const { isDialogOpen } = this.state;

        return (
            <div>
                {isDialogOpen && <FeedbackForm handleDialog={this.handleDialogClose} isDialogOpen={isDialogOpen} />}
                <div className={noFloat ? '' : 'floating-button'}>
                    <Fab
                        variant="extended"
                        style={
                            noFloat
                                ? {}
                                : {
                                      position: 'fixed',
                                      top: '90vh',
                                      left: leftPlacement,
                                      whiteSpace: 'nowrap',
                                  }
                        }
                        onClick={() => {
                            document.body.classList.remove('mobile-menu-open');
                            this.handleDialogOpen();
                        }}
                    >
                        {noFloat && (
                            <img alt="" src={FeedbackImage} className="feedback-img" style={{ marginRight: '8px' }} />
                        )}
                        Give us Feedback
                    </Fab>
                </div>
            </div>
        );
    }
}
