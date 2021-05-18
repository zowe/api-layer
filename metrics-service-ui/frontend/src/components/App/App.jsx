import React, { Component, Suspense } from 'react';
import { Redirect, Route, Router, Switch } from 'react-router-dom';
import { AsyncLoginContainer } from './AsyncModules';
import Spinner from '../Spinner/Spinner';

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
                                    <Route path="/" exact render={() => <Redirect replace to="/dashboard" />} />
                                    <Route
                                        path="/login"
                                        exact
                                        render={(props, state) => <AsyncLoginContainer {...props} {...state} />}
                                    />
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
