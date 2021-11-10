import { Component } from 'react';
import { Button, Dialog, DialogActions, DialogBody, DialogFooter, DialogHeader, DialogTitle, Text } from 'mineral-ui';
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
            <Dialog isOpen={confirmDialog} closeOnClickOutside={false}>
                <DialogHeader>
                    <DialogTitle>Are you sure?</DialogTitle>
                </DialogHeader>
                <DialogBody>
                    <Text>
                        Static definition with serviceId <code>{serviceId}</code> already exists. Do you wish to
                        overwrite it?
                    </Text>
                </DialogBody>
                <DialogFooter>
                    <DialogActions>
                        <Button onClick={confirmStaticDefOverride} size="medium">
                            No, I'll change my serviceId
                        </Button>
                        <Button onClick={this.override} size="medium">
                            Yes, overwrite
                        </Button>
                    </DialogActions>
                </DialogFooter>
            </Dialog>
        );
    }
}

export default ConfirmDialog;
