import Loadable from 'react-loadable';
import { lazy } from 'react';

export const AsyncAppContainer = Loadable({
    loader: () => import('./AppContainer'),
    loading: () => null,
});

export const AsyncLoginContainer = lazy(() => import('../Login/LoginContainer'));
