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
import { ReactComponent as FeedbackImage } from '../../assets/images/square-envelope.svg';

export default class FeedbackButton extends Component {
    render() {
        const { noFloat, rightPlacement = '8px', bottomPlacement = '8px' } = this.props;

        return (
            <div>
                <div className={noFloat ? '' : 'floating-button'}>
                    <Fab
                        variant="extended"
                        href="https://mainframe.broadcom.com/developer-site-feedback"
                        rel="noopener noreferrer"
                        target="_blank"
                        className="button-cta feedback-button"
                        style={
                            noFloat
                                ? {}
                                : {
                                      position: 'fixed',
                                      bottom: bottomPlacement,
                                      right: rightPlacement,
                                      whiteSpace: 'nowrap',
                                  }
                        }
                        onClick={() => {
                            document.body.classList.remove('mobile-menu-open');
                        }}
                    >
                        <FeedbackImage className="icon-img" alt="" />
                        Give us Feedback
                    </Fab>
                </div>
            </div>
        );
    }
}
