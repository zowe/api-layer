import React, { Component, Suspense } from 'react';
import { Redirect, Route, Router, Switch } from 'react-router-dom';
import { ThemeProvider } from '@material-ui/core/styles';

import AuthRoute from '../AuthRoute/AuthRoute';
import { AsyncLoginContainer } from './AsyncModules';
import Spinner from '../Spinner/Spinner';
import HeaderContainer from '../Header/HeaderContainer';
import DashboardContainter from '../Dashboard/DashboardContainer';
import theme from '../../theme';

class App extends Component {
    render() {
        const { history } = this.props;
        const isLoading = true;
        return (
            <div className="App">
                <ThemeProvider theme={theme}>
                    <Suspense fallback={<Spinner isLoading={isLoading} />}>
                        <Router history={history}>
                            <div className="content">
                                {/* Switch is used to render header for every path except /login */}
                                <Switch>
                                    <Route path="/login" exact render={null} />
                                    <Route component={HeaderContainer} />
                                </Switch>

                                <Switch>
                                    <AuthRoute path="/" exact render={() => <Redirect replace to="/dashboard" />} />
                                    <Route
                                        path="/login"
                                        exact
                                        render={(props, state) => <AsyncLoginContainer {...props} {...state} />}
                                    />
                                    <AuthRoute path="/dashboard" component={DashboardContainter} />
                                </Switch>
                            </div>
                        </Router>
                    </Suspense>
                </ThemeProvider>
            </div>
        );
    }
}

export default App;
