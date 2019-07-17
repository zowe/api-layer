const Loadable = React.lazy(() => import('react-loadable'));

export const AsyncAppContainer = Loadable({
    loader: () => import('../App/AppContainer'),
    loading: () => null,
});

export const AsyncLoginContainer = Loadable({
    loader: () => import('../Login/LoginContainer'),
    loading: () => null,
});

export const AsyncDashboardContainer = Loadable({
    loader: () => import('../Dashboard/DashboardContainer'),
    loading: () => null,
});

export const AsyncDetailPageContainer = Loadable({
    loader: () => import('../DetailPage/DetailPageContainer'),
    loading: () => null,
});
