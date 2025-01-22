import React from 'react'
import { render } from '@testing-library/react'
import { Provider } from 'react-redux'
import {setupStore} from "./detail-page-store";


export function renderWithProviders(
    ui,
    {
        preloadedState = {},
        // Automatically create a store instance if no store was passed in
        store = setupStore(preloadedState),
        ...renderOptions
    } = {}
) {
    function Wrapper({ children }) {
        return <Provider store={store}>{children}</Provider>
    }
    return { store, ...render(ui, { wrapper: Wrapper, ...renderOptions }) }
}


