/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import React, { Suspense } from 'react';
import { Navigate, Routes, Route } from 'react-router';
import { ThemeProvider } from '@material-ui/core/styles';

import AuthRoute from '../AuthRoute/AuthRouteContainer';
import { AsyncLoginContainer } from './AsyncModules'; // eslint-disable-line import/no-cycle
import Spinner from '../Spinner/Spinner';
import HeaderContainer from '../Header/HeaderContainer';
import DashboardContainer from '../Dashboard/DashboardContainer';
import theme from '../../helpers/theme';

function App() {
    const isLoading = true;
    return (
        <div className="App">
            <ThemeProvider theme={theme}>
                <Suspense fallback={<Spinner isLoading={isLoading} />}>
                    <div className="content">
                        <Routes>
                            <Route path="/login" element={null} />
                            <Route path="*" element={<HeaderContainer />} />
                        </Routes>

                        <Routes>
                            <Route path="/" element={<Navigate replace to="/dashboard" />} />
                            <Route path="/login" element={<AsyncLoginContainer />} />
                            <Route path="/dashboard" element={<AuthRoute><DashboardContainer /></AuthRoute>} />
                        </Routes>
                    </div>
                </Suspense>
            </ThemeProvider>
        </div>
    );
}

export default App;
