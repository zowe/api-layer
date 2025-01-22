import { combineReducers, configureStore } from '@reduxjs/toolkit'

import tilesReducer from '../reducers/tiles-reducer'
import selectedServiceReducer from "../reducers/selected-service-reducer";

// Create the root reducer separately so we can extract the RootState type
const rootReducer = combineReducers({
    tilesReducer: tilesReducer,
    selectedServiceReducer: selectedServiceReducer
})

export const setupStore = preloadedState => {
    return configureStore({
        reducer: rootReducer,
        preloadedState
    })
}
