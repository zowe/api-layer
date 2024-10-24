/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { useEffect, Suspense } from 'react';
import { Redirect, Route, Router, Switch } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import BigShield from '../ErrorBoundary/BigShield/BigShield';
import ErrorContainer from '../Error/ErrorContainer';
import '../../assets/css/APIMReactToastify.css';
import PageNotFound from '../PageNotFound/PageNotFound';
import HeaderContainer from '../Header/HeaderContainer';
import Spinner from '../Spinner/Spinner';
import { AsyncDashboardContainer, AsyncDetailPageContainer, AsyncLoginContainer } from './AsyncModules';

function App({ history }) {
    const isLoading = true;
    const headerPath = '/(dashboard|service/.*)/';
    const dashboardPath = '/dashboard';

    useEffect(() => {
        window.process = { ...window.process };
    }, []);

    return (
        <div className="App">
            <BigShield history={history}>
                <ToastContainer />
                <ErrorContainer />
                <Suspense fallback={<Spinner isLoading={isLoading} />}>
                    <Router history={history}>
                        {/* eslint-disable-next-line react/jsx-no-useless-fragment */}
                        <>
                            <div className="content">
                                <Route path={headerPath} component={HeaderContainer} />

                                <Switch>
                                    <Route path="/" exact render={() => <Redirect replace to={dashboardPath} />} />
                                    <Route
                                        path="/login"
                                        exact
                                        render={(props, state) => <AsyncLoginContainer {...props} {...state} />}
                                    />
                                    <Route
                                        exact
                                        path={dashboardPath}
                                        render={(props, state) => (
                                            <BigShield history={history}>
                                                <AsyncDashboardContainer {...props} {...state} />
                                            </BigShield>
                                        )}
                                    />
                                    <Route
                                        path="/service"
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
                            </div>
                        </>
                    </Router>
                </Suspense>
            </BigShield>
        </div>
    );
}

export default App;
