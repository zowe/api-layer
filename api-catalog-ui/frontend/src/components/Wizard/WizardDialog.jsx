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
                        <ButtonGroup aria-label="Optional compositions">
                            <Button>Previous</Button>
                            <Button>Next</Button>
                            <Dropdown data={data}>
                                <Button iconEnd={<IconArrowDropDown />}>Categories</Button>
                            </Dropdown>
                        </ButtonGroup>
                        <div className="wizardForm">
                            <TextInput size="large" placeholder="serviceId" />
                            <TextInput size="large" placeholder="title" />
                            <TextInput size="large" placeholder="description" />
                            <TextInput size="large" placeholder="baseURL" />
                        </div>
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
