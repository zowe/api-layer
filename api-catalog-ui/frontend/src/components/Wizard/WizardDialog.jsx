/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import React, { Component } from 'react';
import { Dialog, DialogBody, DialogHeader, DialogTitle, DialogFooter, DialogActions, Button, Text } from 'mineral-ui';
import './wizard.css';
import WizardNavigationContainer from './WizardNavigationContainer';

export default class WizardDialog extends Component {
    closeWizard = () => {
        const { wizardToggleDisplay } = this.props;
        wizardToggleDisplay();
    };

    doneWizard = () => {
        const { refreshedStaticApi, wizardToggleDisplay } = this.props;
        wizardToggleDisplay();
        refreshedStaticApi();
    };

    render() {
        const { wizardIsOpen, enablerName } = this.props;
        return (
            <div className="dialog">
                <Dialog id="wizard-dialog" isOpen={wizardIsOpen} closeOnClickOutside={false}>
                    <DialogHeader>
                        <DialogTitle>Onboard a New API Using {enablerName}</DialogTitle>
                    </DialogHeader>
                    <DialogBody>
                        <Text>This wizard will guide you through creating a correct YAML for your application.</Text>
                        <WizardNavigationContainer />
                    </DialogBody>
                    <DialogFooter className="dialog-footer">
                        <DialogActions>
                            <Button size="medium" onClick={this.closeWizard}>
                                Cancel
                            </Button>
                            <Button size="medium" onClick={this.doneWizard}>
                                Finish
                            </Button>
                        </DialogActions>
                    </DialogFooter>
                </Dialog>
            </div>
        );
    }
}
