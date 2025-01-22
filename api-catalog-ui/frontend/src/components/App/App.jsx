/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import React, { useEffect, Suspense } from 'react';
import {Navigate, Route, Routes} from 'react-router';
import { ToastContainer } from 'react-toastify';
import BigShield from '../ErrorBoundary/BigShield/BigShield';
import ErrorContainer from '../Error/ErrorContainer';
import '../../assets/css/APIMReactToastify.css';
import PageNotFound from '../PageNotFound/PageNotFound';
import HeaderContainer from '../Header/HeaderContainer';
import Spinner from '../Spinner/Spinner';
import { AsyncDashboardContainer, AsyncDetailPageContainer, AsyncLoginContainer } from './AsyncModules';

function App() {
    const isLoading = true;
    const dashboardPath = '/dashboard';

    useEffect(() => {
        window.process = { ...window.process };
    }, []);

    return (
        <div className="App">
            <BigShield>
                <ToastContainer />
                <ErrorContainer />
                <Suspense fallback={<Spinner isLoading={isLoading} />}>
                        {/* eslint-disable-next-line react/jsx-no-useless-fragment */}
                        <>
                            <div className="content">
                                <Routes>
                                    <Route path="/" exact element={<Navigate replace={true} to={dashboardPath} />} />
                                    <Route
                                        path="/login"
                                        exact
                                        element={<AsyncLoginContainer />}
                                    />
                                    <Route
                                        exact
                                        path={dashboardPath}
                                        element={

                                            <BigShield>
                                                    <HeaderContainer/>
                                                    <AsyncDashboardContainer/>
                                            </BigShield>
                                        }
                                    />
                                    <Route
                                        path="/service/*"
                                        element={
                                           <BigShield>
                                               <HeaderContainer/>
                                                <AsyncDetailPageContainer/>
                                            </BigShield>
                                        }
                                    />

                                    <Route
                                        element={
                                            <BigShield>
                                                <PageNotFound/>
                                            </BigShield>
                                        }
                                    />
                                </Routes>
                            </div>
                        </>
                </Suspense>
            </BigShield>
        </div>
    );
}

export default App;
