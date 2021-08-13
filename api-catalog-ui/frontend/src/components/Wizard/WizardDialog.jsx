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
import WizardNavigationContainer from './WizardComponents/WizardNavigationContainer';

export default class WizardDialog extends Component {
    constructor(props) {
        super(props);
        this.nextSave = this.nextSave.bind(this);
    }

    closeWizard = () => {
        const { wizardToggleDisplay } = this.props;
        wizardToggleDisplay();
    };

    doneWizard = () => {
        const { refreshedStaticApi, wizardToggleDisplay, createYamlObject, inputData } = this.props;
        wizardToggleDisplay();
        refreshedStaticApi();
        createYamlObject(inputData);
    };

    /**
     * Displays either Next or Save, depending whether the user is at the last stage or not.
     */
    nextSave = () => {
        const { selectedCategory, inputData, nextWizardCategory, checkFilledInput } = this.props;
        if (selectedCategory < inputData.length) {
            checkFilledInput(inputData[selectedCategory].nav);
            nextWizardCategory();
        } else {
            this.doneWizard();
        }
    };

    render() {
        const { wizardIsOpen, enablerName, inputData, selectedCategory, navTabArray } = this.props;
        const size = selectedCategory === navTabArray.length ? 'large' : 'medium';
        return (
            <div className="dialog">
                <Dialog id="wizard-dialog" isOpen={wizardIsOpen} size={size} closeOnClickOutside={false}>
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
                            <Button size="medium" onClick={this.nextSave}>
                                {selectedCategory === inputData.length ? 'Save' : 'Next'}
                            </Button>
                        </DialogActions>
                    </DialogFooter>
                </Dialog>
            </div>
        );
    }
}
