import React, { Component } from 'react';
import { Redirect, Route, Router, Switch } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import BigShield from '../ErrorBoundary/BigShield/BigShield';
import ErrorContainer from '../Error/ErrorContainer';
import { AsyncDetailPageContainer, AsyncDashboardContainer, AsyncLoginContainer } from './AsyncModules';
import '../../webflow.css';
import './App.css';
import '../../assets/css/APIMReactToastify.css';
import PageNotFound from '../PageNotFound/PageNotFound';
import HeaderContainer from '../Header/HeaderContainer';

class App extends Component {
    render() {
        const { history, authentication } = this.props;
        return (
            <div className="App">
                <BigShield history={history}>
                    {authentication.showHeader !== undefined &&
                        authentication.showHeader === true && <HeaderContainer />}
                    <ToastContainer />
                    <ErrorContainer />
                    <Router history={history}>
                        <Switch>
                            <Route path="/" exact render={() => <Redirect replace to="/login" />} />
                            <Route
                                path="/login"
                                exact
                                render={(props, state) => <AsyncLoginContainer {...props} {...state} />}
                            />
                            <Route
                                exact
                                path="/dashboard"
                                render={(props, state) => (
                                    <BigShield>
                                        <AsyncDashboardContainer {...props} {...state} />
                                    </BigShield>
                                )}
                            />
                            <Route
                                path="/tile/:tileID"
                                render={(props, state) => (
                                    <BigShield history={history}>
                                        <AsyncDetailPageContainer {...props} {...state} />
                                    </BigShield>
                                )}
                            />
                            <Route
                                render={(props, state) => (
                                    <BigShield history={history}>
                                        <PageNotFound {...props} {...state} />
                                    </BigShield>
                                )}
                            />
                        </Switch>
                    </Router>
                </BigShield>
            </div>
        );
    }
}

export default App;
