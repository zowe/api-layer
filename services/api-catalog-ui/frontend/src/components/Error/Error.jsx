import React, {Component} from 'react';
import {Button, Dialog, DialogActions, DialogBody, DialogFooter, DialogHeader, DialogTitle, Text} from 'mineral-ui';
import formatError from './ErrorFormatter';

export default class Error extends Component {
    closeDialog = () => {
        const {clearAllErrors} = this.props;
        clearAllErrors();
    };

    render() {
        const {errors} = this.props;
        const isTrue = true;
        const isFalse = false;
        return (
            <div>
                {errors &&
                errors.length > 0 && (
                    <Dialog
                        variant="danger"
                        appSelector="#App"
                        closeOnClickOutside={isFalse}
                        hideOverlay={isTrue}
                        modeless={isFalse}
                        isOpen={errors.length > 0}
                    >
                        <DialogHeader>
                            <DialogTitle>Error</DialogTitle>
                        </DialogHeader>
                        <DialogBody>
                            {errors !== null && errors !== undefined && errors.length > 0 ? (
                                errors.map(error => formatError(error))
                            ) : (
                                <Text>No Errors Found</Text>
                            )}
                        </DialogBody>
                        <DialogFooter>
                            <DialogActions>
                                <Button size="medium" variant="danger" onClick={this.closeDialog}>
                                    Close
                                </Button>
                            </DialogActions>
                        </DialogFooter>
                    </Dialog>
                )}
            </div>
        );
    }
}
