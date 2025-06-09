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
import { Navigate, Route, Routes, useNavigate } from 'react-router';
import { ToastContainer } from 'react-toastify';
import BigShield from '../ErrorBoundary/BigShield/BigShield';
import ErrorContainer from '../Error/ErrorContainer';
import '../../assets/css/APIMReactToastify.css';
import PageNotFound from '../PageNotFound/PageNotFound';
import HeaderContainer from '../Header/HeaderContainer';
import Spinner from '../Spinner/Spinner';
import { AsyncDashboardContainer, AsyncDetailPageContainer, AsyncLoginContainer } from './AsyncModules';
import { userService } from "../../services";

function App(props) {
    const isLoading = true;
    const dashboardPath = '/dashboard';
    const navigate = useNavigate();
    useEffect(() => {
        window.process = { ...window.process };
    }, []);
    const { authentication, success, logout } = props;
    useEffect(() => {
        const checkAuth = () => {
            if (!authentication.user) {
                userService.query().then((result) => {
                    if (result.status === 200) {
                        success(result.userId, false);
                    } else {
                        navigate('/login');
                    }
                }).catch(() => {
                    navigate('/login');
                });
            }
        };

        // Run once on mount
        checkAuth();

        // Run again whenever the tab gains focus
        window.addEventListener('focus', checkAuth);

        const authChannel = new BroadcastChannel('auth_channel');
        authChannel.onmessage = (event) => {
            if (event.data === 'logout') {
                logout();
                navigate('/login');
            }

            if (event.data === 'login') {
                userService.query().then((result) => {
                    if (result.status === 200) {
                        success(result.userId, false);
                    }
                });
            }
        };

        return () => { // Remove immediately to avoid loop
            window.removeEventListener('focus', checkAuth);
            authChannel.close();
        };
    }, [authentication.user, success, navigate]);

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
