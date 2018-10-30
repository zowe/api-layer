import React, { Component } from 'react';
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
                    <Button primary onClick={this.handleGoToHome} size="medium" iconStart={iconBack}>
                        Go to Dashboard
                    </Button>
                </div>
            </div>
        );
    }
}
