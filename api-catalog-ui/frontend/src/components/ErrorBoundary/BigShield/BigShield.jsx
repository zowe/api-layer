/* eslint-disable react/destructuring-assignment */
import React, {Component} from 'react';
import Text from 'mineral-ui/Text';
import '../../Dashboard/Dashboard.css';
import {Button} from 'mineral-ui';
import IconChevronLeft from 'mineral-ui-icons/IconChevronLeft';
import './BigShield.css';

export default class BigShield extends Component {
    constructor(props) {
        super(props);
        this.state = {
            error: false,
            info: null,
        };
    }

    handleGoToHome = () => {
        const {history} = this.props;
        this.setState({error: null});
        history.push('/dashboard');
    };

    componentDidCatch(error, info) {
        this.setState({
            error,
            info,
        });
    }

    render() {
        const iconBack = <IconChevronLeft/>;
        const {history} = this.props;
        let disableButton = true;
        if (history !== undefined && history !== null) {
            if (
                history.location === undefined ||
                (history.location !== undefined &&
                    history.location.pathname !== undefined &&
                    history.location.pathname !== '/dashboard')
            ) {
                disableButton = false;
            }
        }
        if (this.state.error) {
            const {
                error: {stack},
                info: {componentStack},
            } = this.state;
            return (
                <div>
                    <div style={{marginLeft: '100px', marginRight: '100px'}}>
                        <br/>
                        <br/>
                        {!disableButton && (
                            <div>
                                <Button primary onClick={this.handleGoToHome} size="medium" iconStart={iconBack}>
                                    Go to Dashboard
                                </Button>
                            </div>
                        )}
                        <br/>
                        <div className="local-dev-debug">
                            <Text element="h2" style={{color: '#de1b1b'}}>
                                An unexpected browser error occurred
                            </Text>
                            <br/>
                            <Text element="h3" fontWeight="semiBold" color="black">
                                You are seeing this page because an unexpected error occurred while rendering your page.
                                <br/>
                                <br/>
                                {disableButton && (
                                    <b>The Dashboard is broken, you cannot navigate away from this page.</b>
                                )}
                                {!disableButton &&
                                <b>You can return to the Dashboard by clicking on the button above.</b>}
                            </Text>
                            <Text element="h4" color="#de1b1b">
                                <pre>
                                    <code>{this.state.error.message}</code>
                                </pre>
                            </Text>

                            <div className="wrap-collabsible">
                                <input id="collapsible" className="toggle" type="checkbox"/>
                                <label htmlFor="collapsible" className="lbl-toggle">
                                    Display the error stack
                                </label>
                                <div className="collapsible-content">
                                    <div className="content-inner">
                                        <Text element="h5">
                                            <pre>
                                                <code>{stack}</code>
                                            </pre>
                                        </Text>
                                    </div>
                                </div>
                            </div>
                            <br/>
                            <br/>
                            <div className="wrap-collabsible2">
                                <input id="collapsible2" className="toggle2" type="checkbox"/>
                                <label htmlFor="collapsible2" className="lbl-toggle2">
                                    Display the component stack
                                </label>
                                <div className="collapsible-content2">
                                    <div className="content-inner2">
                                        <Text element="h5">
                                            <pre>
                                                <code>{componentStack}</code>
                                            </pre>
                                        </Text>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            );
        }
        return this.props.children;
    }
}
