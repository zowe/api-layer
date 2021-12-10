import { Component } from 'react';
import { IconButton, Typography } from '@material-ui/core';
import ChevronLeftIcon from '@material-ui/icons/ChevronLeft';

import './PageNotFound.css';

export default class PageNotFound extends Component {
    handleGoToHome = () => {
        const { history } = this.props;
        history.push('/dashboard');
    };

    render() {
        const iconBack = <ChevronLeftIcon />;
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
