import { Component } from 'react';
import { Dialog, DialogContent, DialogContentText, DialogTitle, DialogActions, IconButton } from '@material-ui/core';

import formatError from './ErrorFormatter';

export default class Error extends Component {
    closeDialog = () => {
        const { clearAllErrors } = this.props;
        clearAllErrors();
    };

    render() {
        const { errors } = this.props;
        return (
            <div>
                {errors &&
                    errors.length > 0 && (
                        <Dialog variant="danger" open={errors.length > 0}>
                            <DialogTitle>Error</DialogTitle>
                            <DialogContent data-testid="dialog-content">
                                {errors !== null && errors !== undefined && errors.length > 0 ? (
                                    errors.map(error => formatError(error))
                                ) : (
                                    <DialogContentText style={{ color: 'black' }}>No Errors Found</DialogContentText>
                                )}
                            </DialogContent>
                            <DialogActions>
                                <IconButton
                                    size="medium"
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
