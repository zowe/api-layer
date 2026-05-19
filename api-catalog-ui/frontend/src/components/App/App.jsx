/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { Suspense, useEffect } from 'react';
import { Navigate, Routes, Route } from 'react-router';
import { ToastContainer } from 'react-toastify';
import BigShield from '../ErrorBoundary/BigShield/BigShield';
import ErrorContainer from '../Error/ErrorContainer';
import '../../assets/css/APIMReactToastify.css';
import PageNotFound from '../PageNotFound/PageNotFound';
import HeaderContainer from '../Header/HeaderContainer';
import Spinner from '../Spinner/Spinner';
import { closeMobileMenu, isAPIPortal } from '../../utils/utilFunctions';
import { AsyncDashboardContainer, AsyncDetailPageContainer, AsyncLoginContainer } from './AsyncModules'; // eslint-disable-line import/no-cycle

function App() {
    useEffect(() => {
        // workaround for missing process polyfill in webpack 5
        window.process = { ...window.process };
        window.onresize = () => {
            if (document.body.offsetWidth > 767) {
                closeMobileMenu();
            }
        };
    }, []);

    const isLoading = true;
    const headerPath = '/(dashboard|service/.*)/';
    const dashboardPath = '/dashboard';

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
                                <Route path={headerPath} element={<HeaderContainer />} />

                                {isAPIPortal() && (
                                    <div className="dashboard-mobile-menu mobile-view">
                                        <HeaderContainer />
                                    </div>
                                )}

                                <Route path="/" element={<Navigate replace to={dashboardPath} />} />
                                <Route path="/login" element={<AsyncLoginContainer />} />
                                <Route
                                    path={dashboardPath}
                                    element={
                                        <BigShield>
                                            <AsyncDashboardContainer />
                                        </BigShield>
                                    }
                                />
                                <Route
                                    path="/service/*"
                                    element={
                                        <BigShield>
                                            <AsyncDetailPageContainer />
                                        </BigShield>
                                    }
                                />
                                <Route
                                    element={
                                        <BigShield>
                                            <PageNotFound />
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
