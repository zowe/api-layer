import React, { Component } from 'react';
import { Dialog, DialogBody, DialogHeader, DialogTitle, DialogFooter, DialogActions, Button, Text } from 'mineral-ui';

export default class ErrorDialog extends Component {

    closeDialog = () => {
        const {clearError} = this.props;
        clearError();
    };

    getCorrectRefreshMessage = error => {
        let messageText;
        if (error && !error.status && !error.messageNumber) {
            messageText = error.toString();
            messageText = "(ZWEAD702E) A problem occurred while parsing a static API definition file " + messageText;
            return messageText;
        }
        const errorMessages = require("../../error-messages.json");
        if (error && error.messageNumber && error.messageType) {
            messageText = "Unexpected error, please try again later";
            const filter = errorMessages.messages.filter(x => x.messageKey != null && x.messageKey === error.messageNumber);
            if (filter.length !== 0) {
                messageText = `(${error.messageNumber}) ${filter[0].messageText}`;
            }
        }
        return messageText;
    };

    render() {
        const {refreshedStaticApisError} = this.props;
        let refreshError = this.getCorrectRefreshMessage(refreshedStaticApisError);
        const isTrue = true;
        const isFalse = false;
        return (
            <div>
            {(refreshedStaticApisError && (refreshedStaticApisError.status || typeof refreshedStaticApisError === 'object'))
            && (
                <Dialog
                    variant="danger"
                    appSelector="#App"
                    closeOnClickOutside={isFalse}
                    hideOverlay={isTrue}
                    modeless={isFalse}
                    isOpen={refreshedStaticApisError !== null}
                >
                    <DialogHeader>
                        <DialogTitle>Error</DialogTitle>
                    </DialogHeader>
                    <DialogBody>
                        <Text>{refreshError}</Text>
                    </DialogBody>
                    <DialogFooter>
                        <DialogActions>
                            <Button size="medium" variant="danger" onClick={this.closeDialog}>
                                Close
                            </Button>
                        </DialogActions>
                    </DialogFooter>
                </Dialog>
            )
    }
            </div>
    );

    }
}
