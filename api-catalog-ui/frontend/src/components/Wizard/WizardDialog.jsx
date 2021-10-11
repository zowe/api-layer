/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import * as YAML from 'yaml';
import React, { Component } from 'react';
import { Dialog, DialogBody, DialogHeader, DialogTitle, DialogFooter, DialogActions, Button, Text } from 'mineral-ui';
import './wizard.css';
import WizardNavigationContainer from './WizardComponents/WizardNavigationContainer';

export default class WizardDialog extends Component {
    constructor(props) {
        super(props);
        this.nextSave = this.nextSave.bind(this);
        this.renderDoneButtonText = this.renderDoneButtonText.bind(this);
    }

    closeWizard = () => {
        const { wizardToggleDisplay } = this.props;
        wizardToggleDisplay();
    };

    doneWizard = () => {
        const { sendYAML, navsObj, notifyError, yamlObject, serviceId, enablerName } = this.props;

        /**
         * Check that all mandatory fields are filled.
         * @param navs navsObj contains information about all unfilled fields in each nav tab.
         * @returns {boolean} true if no mandatory fields are empty
         */
        const presenceIsSufficient = navs => {
            let sufficient = true;
            Object.keys(navs).forEach(nav => {
                Object.keys(navs[nav]).forEach(category => {
                    if (Array.isArray(navs[nav][category])) {
                        navs[nav][category].forEach(set => {
                            if (set.length > 0) {
                                sufficient = false;
                            }
                        });
                    }
                });
            });
            return sufficient;
        };
        if (enablerName !== 'Static Onboarding' || !this.props.userCanAutoOnboard) {
            this.closeWizard();
        } else if (presenceIsSufficient(navsObj)) {
            sendYAML(YAML.stringify(yamlObject), serviceId);
        } else {
            notifyError('Fill all mandatory fields first!');
        }
    };

    /**
     * Displays either Next or Save, depending whether the user is at the last stage or not.
     */
    nextSave = () => {
        const { selectedCategory, navsObj, nextWizardCategory, validateInput } = this.props;
        if (selectedCategory < Object.keys(navsObj).length) {
            validateInput(Object.keys(navsObj)[selectedCategory], false);
            nextWizardCategory();
            if (selectedCategory === Object.keys(navsObj).length - 1) {
                const navNamesArr = Object.keys(this.props.navsObj);
                navNamesArr.forEach(navName => {
                    this.props.validateInput(navName, false);
                });
            }
        } else {
            this.doneWizard();
        }
    };

    renderDoneButtonText() {
        if (this.props.enablerName === 'Static Onboarding' && this.props.userCanAutoOnboard) {
            return 'Save';
        }
        return 'Done';
    }

    render() {
        const { wizardIsOpen, enablerName, selectedCategory, navsObj } = this.props;
        const size = selectedCategory === Object.keys(navsObj).length ? 'large' : 'medium';
        const disable = selectedCategory !== Object.keys(navsObj).length;
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
                            <Button size="medium" onClick={this.nextSave} disabled={disable}>
                                {this.renderDoneButtonText()}
                            </Button>
                        </DialogActions>
                    </DialogFooter>
                </Dialog>
            </div>
        );
    }
}
