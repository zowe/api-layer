import React, { Component } from 'react';
import { Dialog, DialogBody, DialogHeader, DialogTitle, DialogFooter, DialogActions, Button, Text } from 'mineral-ui';
import './wizard.css';

export default class WizardDialog extends Component {
    closeWizard = () => {
        const { wizardToggleDisplay } = this.props;
        wizardToggleDisplay();
    };
    render() {
        const { wizardIsOpen } = this.props;
        return (
            <div className="dialog">
                <Dialog isOpen={wizardIsOpen} closeOnClickOutside={false}>
                    <DialogHeader>
                        <DialogTitle>Onboard a New API</DialogTitle>
                    </DialogHeader>
                    <DialogBody>
                        <Text>This wizard will guide you through creating a correct YAML for your application.</Text>
                    </DialogBody>
                    <DialogFooter className="dialog-footer">
                        <DialogActions>
                            <Button size="medium" onClick={this.closeWizard}>
                                Cancel
                            </Button>
                            <Button size="medium">Save file</Button>
                        </DialogActions>
                    </DialogFooter>
                </Dialog>
            </div>
        );
    }
}
