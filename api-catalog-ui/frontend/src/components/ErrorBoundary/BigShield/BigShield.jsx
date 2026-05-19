/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { useState, useCallback } from 'react';
import { Typography, Button } from '@material-ui/core';
import ArrowBackIosNewIcon from '@material-ui/icons/ArrowBackIosNew';
import PropTypes from 'prop-types';
import { useNavigate, useLocation } from 'react-router';
import './BigShield.css';

export default function BigShield(props) {
    const [error, setError] = useState(false);
    const [info, setInfo] = useState(null);
    const navigate = useNavigate();
    const location = useLocation();

    const handleGoToHome = useCallback(() => {
        setError(null);
        navigate('/dashboard');
    }, [navigate]);

    const path = '/dashboard';
    const disableButton = location.pathname === path;

    if (error) {
        return (
            <div>
                <div style={{ marginLeft: '100px', marginRight: '100px' }}>
                    <br />
                    <br />
                    {!disableButton && (
                        <div>
                            <Button
                                id="go-back-button"
                                data-testid="go-home-button"
                                primary
                                onClick={handleGoToHome}
                                size="medium"
                                iconStart={<ArrowBackIosNewIcon />}
                            >
                                Go to Dashboard
                            </Button>
                        </div>
                    )}
                    <br />
                    <div className="local-dev-debug">
                        <Typography variant="h4" style={{ color: '#de1b1b' }}>
                            An unexpected browser error occurred
                        </Typography>
                        <br />
                        <Typography variant="h6" style={{ color: 'black', fontWeight: 'semiBold' }}>
                            You are seeing this page because an unexpected error occurred while rendering your page.
                            <br />
                            <br />
                            {disableButton && (
                                <b>The Dashboard is broken, you cannot navigate away from this page.</b>
                            )}
                            {!disableButton && (
                                <b>You can return to the Dashboard by clicking on the button above.</b>
                            )}
                        </Typography>
                        <Typography variant="h6" color="#de1b1b">
                            <pre>
                                <code>{error.message}</code>
                            </pre>
                        </Typography>

                        <div className="wrap-collabsible">
                            <input id="collapsible" className="toggle" type="checkbox" />
                            {/* eslint-disable-next-line jsx-a11y/label-has-for */}
                            <label htmlFor="collapsible" className="lbl-toggle">
                                Display the error stack
                            </label>
                            <div className="collapsible-content">
                                <div className="content-inner">
                                    <Typography variant="h5">
                                        <pre>
                                            <code>{info?.componentStack}</code>
                                        </pre>
                                    </Typography>
                                </div>
                            </div>
                        </div>
                        <br />
                        <br />
                        <div className="wrap-collabsible2">
                            <input id="collapsible2" className="toggle2" type="checkbox" />
                            {/* eslint-disable-next-line jsx-a11y/label-has-for */}
                            <label htmlFor="collapsible2" className="lbl-toggle2">
                                Display the component stack
                            </label>
                            <div className="collapsible-content2">
                                <div className="content-inner2">
                                    <Typography variant="h5">
                                        <pre>
                                            <code>{info?.componentStack}</code>
                                        </pre>
                                    </Typography>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    }
    return props.children;
}

BigShield.propTypes = {
    children: PropTypes.node.isRequired,
};
