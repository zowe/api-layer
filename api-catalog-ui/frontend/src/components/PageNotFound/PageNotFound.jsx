import { Component } from 'react';
import { IconButton, Typography } from '@material-ui/core';
import IconChevronLeft from 'mineral-ui-icons/IconChevronLeft';

import './PageNotFound.css';

export default class PageNotFound extends Component {
    handleGoToHome = () => {
        const { history } = this.props;
        history.push('/dashboard');
    };

    render() {
        const iconBack = <IconChevronLeft />;
        return (
            <div>
                <br />
                <Typography id="label" variant="h5">
                    Page Not Found
                </Typography>
                <div>
                    <IconButton
                        id="go-back-button"
                        data-testid="go-home-button"
                        onClick={this.handleGoToHome}
                        size="medium"
                    >
                        {iconBack}
                        Go to Dashboard
                    </IconButton>
                </div>
            </div>
        );
    }
}
