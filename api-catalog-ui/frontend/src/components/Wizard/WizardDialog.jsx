import React, { Component } from 'react';
import { Dialog, DialogBody, DialogHeader, DialogTitle, DialogFooter, DialogActions, Button, Text } from 'mineral-ui';
import './wizard.css';

export default class WizardDialog extends Component {
    render() {
        const { wizardIsOpen, wizardToggleDisplay } = this.props;
        return (
            <div className="dialog">
                <Dialog isOpen={wizardIsOpen} onClose={wizardToggleDisplay}>
                    <DialogHeader>
                        <DialogTitle>Onboard a New API</DialogTitle>
                    </DialogHeader>
                    <DialogBody>
                        <Text>This wizard will guide you through creating a correct YAML for your application.</Text>
                    </DialogBody>
                    <DialogFooter className="dialog-footer">
                        <DialogActions>
                            <Button size="medium">Cancel</Button>
                            <Button size="medium">Action</Button>
                        </DialogActions>
                    </DialogFooter>
                </Dialog>
            </div>
        );
    }
}
