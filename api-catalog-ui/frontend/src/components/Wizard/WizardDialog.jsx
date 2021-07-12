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
import TextInput from 'mineral-ui/TextInput';
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
    Dropdown,
} from 'mineral-ui';
import { IconArrowDropDown } from 'mineral-ui-icons';
import './wizard.css';
import { data } from '../../constants/wizard-constants';

export default class WizardDialog extends Component {
    constructor(props) {
        super(props);
        this.state = {
            selectedIndex: 0,
            inputData: [...data],
        };
        this.handleChange = this.handleChange.bind(this);
    }

    handleChange(event) {
        const { name } = event.target;
        const inputData = [...this.state.inputData];
        const objectToChange = inputData[this.state.selectedIndex];
        inputData[this.state.selectedIndex] = {
            ...objectToChange,
            content: { ...objectToChange.content, [name]: event.target.value },
        };
        this.setState({ inputData });
    }

    closeWizard = () => {
        const { wizardToggleDisplay } = this.props;
        wizardToggleDisplay();
    };

    loadInputs = () => {
        const dataAsObject = this.state.inputData[this.state.selectedIndex];
        if (dataAsObject.content === undefined || Object.entries(dataAsObject.content).length === 0) return '';
        const selectedData = Object.entries(dataAsObject.content);
        let key = 0;
        return selectedData.map(item => {
            key += 1;
            return (
                <TextInput
                    size="large"
                    name={item[0]}
                    onChange={this.handleChange}
                    key={key}
                    placeholder={item[0]}
                    value={dataAsObject.content[item[0]]}
                />
            );
        });
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
                        <ButtonGroup aria-label="Optional compositions">
                            <Button>Previous</Button>
                            <Button>Next</Button>
                            <Dropdown data={data}>
                                <Button iconEnd={<IconArrowDropDown />}>Categories</Button>
                            </Dropdown>
                        </ButtonGroup>
                        <div className="wizardForm"> {this.loadInputs()}</div>
                    </DialogBody>
                    <DialogFooter className="dialog-footer">
                        <DialogActions>
                            <Button size="medium" onClick={this.closeWizard}>
                                Cancel
                            </Button>
                            <Button size="medium"> Save file </Button>
                        </DialogActions>
                    </DialogFooter>
                </Dialog>
            </div>
        );
    }
}
