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
import { feedbackService } from '../../feedbackServices';

export default class FeedbackButton extends Component {
    constructor(props) {
        super(props);
        this.state = {
            isDialogOpen: false,
            formToken: '',
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

    componentDidMount() {
        feedbackService.getToken().then(
            (rsp) => {
                // eslint-disable-next-line no-console
                console.log('asdfadsfasdfasdf');
                // eslint-disable-next-line no-console
                console.log(rsp);
                this.setState({ formToken: rsp });
            },
            (error) => {
                // if (error.messageNumber === 'ZWEAT413E') {
                //     dispatch(invalidPassword(error));
                // } else if (error.messageNumber === 'ZWEAT412E') {
                //     dispatch(expiredPassword(error));
                // } else {
                //     dispatch(failure(error));
                // }
            }
        );
    }

    // componentWillUnmount() {
    // }

    // handleSearch = (value) => {
    // };

    // refreshStaticApis = () => {
    // };

    render() {
        const { noFloat, leftPlacement } = this.props;
        const { isDialogOpen, formToken } = this.state;

        const submit = (data) => {
            console.log('submitting data');
            console.log(data);
            feedbackService.submitFeedback().then(
                (rsp) => {
                    // eslint-disable-next-line no-console
                    console.log('asdfadsfasdfasdf');
                },
                (error) => {
                    // if (error.messageNumber === 'ZWEAT413E') {
                    //     dispatch(invalidPassword(error));
                    // } else if (error.messageNumber === 'ZWEAT412E') {
                    //     dispatch(expiredPassword(error));
                    // } else {
                    //     dispatch(failure(error));
                    // }
                }
            );
        };

        return (
            <div>
                {isDialogOpen && (
                    <FeedbackForm
                        handleDialog={this.handleDialogClose}
                        isDialogOpen={isDialogOpen}
                        formSubmission={submit}
                    />
                )}
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
