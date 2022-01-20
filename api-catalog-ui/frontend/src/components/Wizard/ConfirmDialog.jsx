/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
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
