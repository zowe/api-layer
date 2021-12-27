import { Component } from 'react';
import { Dialog, DialogContent, DialogContentText, DialogTitle, DialogActions, IconButton } from '@material-ui/core';
import * as YAML from 'yaml';

class ConfirmDialog extends Component {
    constructor(props) {
        super(props);
        this.override = this.override.bind(this);
    }

    override() {
        const { overrideStaticDef, yamlObject, serviceId, confirmStaticDefOverride } = this.props;
        overrideStaticDef(YAML.stringify(yamlObject), serviceId);
        confirmStaticDefOverride();
    }

    render() {
        const { confirmDialog, serviceId, confirmStaticDefOverride } = this.props;
        return (
            <Dialog open={confirmDialog}>
                <DialogTitle>Are you sure?</DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        Static definition with serviceId <code>{serviceId}</code> already exists. Do you wish to
                        overwrite it?
                    </DialogContentText>
                </DialogContent>
                <DialogActions>
                    <IconButton onClick={confirmStaticDefOverride} size="medium">
                        No, I'll change my serviceId
                    </IconButton>
                    <IconButton onClick={this.override} size="medium">
                        Yes, overwrite
                    </IconButton>
                </DialogActions>
            </Dialog>
        );
    }
}

export default ConfirmDialog;
