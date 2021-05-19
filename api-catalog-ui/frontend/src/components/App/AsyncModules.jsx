import Loadable from 'react-loadable';
// eslint-disable-next-line no-unused-vars
import React, { lazy } from 'react';

export const AsyncAppContainer = Loadable({
    loader: () => import('../App/AppContainer'),
    loading: () => null,
});

export const AsyncLoginContainer = lazy(() => import('../Login/LoginContainer'));

export const AsyncDashboardContainer = lazy(() => import('../Dashboard/DashboardContainer'));

export const AsyncDetailPageContainer = lazy(() => import('../DetailPage/DetailPageContainer'));
