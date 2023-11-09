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
import { Dialog, DialogContent, DialogContentText, DialogTitle, DialogActions, IconButton } from '@material-ui/core';

export default class ErrorDialog extends Component {
    // eslint-disable-next-line react/sort-comp
    closeDialog = () => {
        const { clearError } = this.props;
        clearError();
    };

    getCorrectRefreshMessage = (error) => {
        let messageText;
        if (error && !error.status && !error.messageNumber) {
            messageText = error.toString();
            messageText = `(ZWEAD702E) A problem occurred while parsing a static API definition file ${messageText}`;
            return messageText;
        }
        // eslint-disable-next-line global-require
        const errorMessages = require('../../error-messages.json');
        if (error?.messageNumber && error?.messageType && error?.messageContent) {
            messageText = `(${error.messageNumber} ${error.messageContent})`;
            const filter = errorMessages.messages.filter(
                (x) => x.messageKey != null && x.messageKey === error.messageNumber
            );
            if (filter.length !== 0) {
                messageText = `(${error.messageNumber}) ${filter[0].messageText}`;
            }
        }
        return messageText;
    };

    render() {
        const { refreshedStaticApisError } = this.props;
        const refreshError = this.getCorrectRefreshMessage(refreshedStaticApisError);
        return (
            <div>
                {refreshedStaticApisError &&
                    (refreshedStaticApisError.status || typeof refreshedStaticApisError === 'object') && (
                        <Dialog variant="danger" open={refreshedStaticApisError !== null}>
                            <DialogTitle style={{ color: '#de1b1b' }}>Error</DialogTitle>
                            <DialogContent data-testid="dialog-content">
                                <DialogContentText style={{ color: 'black' }}>{refreshError}</DialogContentText>
                            </DialogContent>
                            <DialogActions>
                                <IconButton
                                    variant="outlined"
                                    style={{
                                        border: '1px solid #de1b1b',
                                        backgroundColor: '#de1b1b',
                                        borderRadius: '0.1875em',
                                        fontSize: '15px',
                                        color: 'white',
                                    }}
                                    onClick={this.closeDialog}
                                >
                                    Close
                                </IconButton>
                            </DialogActions>
                        </Dialog>
                    )}
            </div>
        );
    }
}
