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
import { Button, Text } from 'mineral-ui';
import IconChevronLeft from 'mineral-ui-icons/IconChevronLeft';

export default class PageNotFound extends Component {
    handleGoToHome = () => {
        const { history } = this.props;
        history.push('/dashboard');
    };

    render() {
        const iconBack = <IconChevronLeft />;
        return (
            <div>
                <Text element="h1">Page Not Found</Text>
                <div>
                    <Button
                        primary
                        data-testid="go-home-button"
                        onClick={this.handleGoToHome}
                        size="medium"
                        iconStart={iconBack}
                    >
                        Go to Dashboard
                    </Button>
                </div>
            </div>
        );
    }
}
