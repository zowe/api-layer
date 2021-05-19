import React, { Component, Suspense } from 'react';
import { Redirect, Route, Router, Switch } from 'react-router-dom';

import AuthRoute from '../AuthRoute/AuthRoute';
import { AsyncLoginContainer } from './AsyncModules';
import Spinner from '../Spinner/Spinner';
import Dashboard from '../Dashboard/Dashboard';

class App extends Component {
    render() {
        const { history } = this.props;
        const isLoading = true;
        return (
            <div className="App">
                <Suspense fallback={<Spinner isLoading={isLoading} />}>
                    <Router history={history}>
                        <>
                            <div className="content">
                                <Switch>
                                    <AuthRoute path="/" exact render={() => <Redirect replace to="/dashboard" />} />
                                    <Route
                                        path="/login"
                                        exact
                                        render={(props, state) => <AsyncLoginContainer {...props} {...state} />}
                                    />
                                    <AuthRoute path="/dashboard" render={() => <Dashboard />} />
                                </Switch>
                            </div>
                        </>
                    </Router>
                </Suspense>
            </div>
        );
    }
}

export default App;
