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
import ButtonGroup from 'mineral-ui/ButtonGroup';
import {
    Dialog,
    DialogBody,
    DialogHeader,
    DialogTitle,
    DialogFooter,
    DialogActions,
    Button,
    Text,
    Select,
} from 'mineral-ui';
import './wizard.css';
import WizardInputsContainer from './WizardInputsContainer';

export default class WizardDialog extends Component {
    constructor(props) {
        super(props);
        this.state = {
            selectedIndex: 0,
        };
        this.handleCategoryChange = this.handleCategoryChange.bind(this);
        this.getPrev = this.getPrev.bind(this);
        this.getNext = this.getNext.bind(this);
    }

    getPrev() {
        const index = this.state.selectedIndex;
        const len = this.props.inputData.length;
        this.setState({ selectedIndex: (index + len - 1) % len });
    }

    getNext() {
        const index = this.state.selectedIndex;
        const len = this.props.inputData.length;
        this.setState({ selectedIndex: (index + 1) % len });
    }

    handleCategoryChange(event) {
        for (let i = 0; i < this.props.inputData.length; i += 1) {
            if (this.props.inputData[i].text === event.text) {
                this.setState({ selectedIndex: i });
                break;
            }
        }
    }

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
        const { wizardIsOpen, enablerName, inputData, selectedIndex } = this.props;
        const selectedItem = inputData[selectedIndex];
        return (
            <div className="dialog">
                <Dialog id="wizard-dialog" isOpen={wizardIsOpen} closeOnClickOutside={false}>
                    <DialogHeader>
                        <DialogTitle>Onboard a New API Using {enablerName}</DialogTitle>
                    </DialogHeader>
                    <DialogBody>
                        <Text>This wizard will guide you through creating a correct YAML for your application.</Text>
                        <ButtonGroup aria-label="Optional compositions">
                            <Button onClick={this.getPrev}>Previous</Button>
                            <Button onClick={this.getNext}>Next</Button>
                            <Select
                                className="selector"
                                data={inputData}
                                selectedItem={selectedItem}
                                onChange={this.handleCategoryChange}
                            />
                        </ButtonGroup>
                        <WizardInputsContainer data={selectedItem} />
                    </DialogBody>
                    <DialogFooter className="dialog-footer">
                        <DialogActions>
                            <Button size="medium" onClick={this.closeWizard}>
                                Cancel
                            </Button>
                            <Button size="medium" onClick={this.doneWizard}>
                                Done
                            </Button>
                        </DialogActions>
                    </DialogFooter>
                </Dialog>
            </div>
        );
    }
}
