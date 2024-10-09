/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import loadable from '@react-loadable/revised';
import { lazy } from 'react';

export const AsyncAppContainer = loadable({
    loader: () => import('./AppContainer'),
    loading: () => null,
});

export const AsyncLoginContainer = lazy(() => import('../Login/LoginContainer'));

export const AsyncDashboardContainer = lazy(() => import('../Dashboard/DashboardContainer'));

export const AsyncDetailPageContainer = lazy(() => import('../DetailPage/DetailPageContainer'));
