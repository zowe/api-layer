/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import _ from 'lodash';

// check the current action against any states which are requests
export const createLoadingSelector = (actions) => (state) =>
    _(actions).some((action) => _.get(state.loadingReducer, action));

// eslint-disable-next-line
/**
 * Filters the services in the dashboard page based on the search criteria
 * @param tiles
 * @param searchCriteria
 * @returns tiles the filtered tiles that matches the criteria
 */
export const getVisibleTiles = (tiles, searchCriteria) => {
    if (tiles === undefined || tiles === null || tiles.length <= 0) {
        return [];
    }
    return tiles
        .filter((tile) => {
            if (searchCriteria === undefined || searchCriteria === null || searchCriteria.length === 0) {
                return true;
            }
            return tile.title.toLowerCase().includes(searchCriteria.toLowerCase());
        })
        .sort((tile1, tile2) => tile1.title.localeCompare(tile2.title));
};

// eslint-disable-next-line
/**
 * Filters the services in the navigation bar based on the search criteria
 * @param tiles
 * @param searchCriteria
 * @returns services the filtered services that matches the criteria
 */
export const getFilteredServices = (tiles, searchCriteria) => {
    if (!tiles || tiles.length === 0) {
        return [];
    }

    const filteredTiles = JSON.parse(JSON.stringify(tiles));

    return filteredTiles.filter((tile) => {
        const filteredServices = tile.services.filter((service) => {
            if (!searchCriteria || searchCriteria.length === 0) {
                return true;
            }
            return service.title.toLowerCase().includes(searchCriteria.toLowerCase());
        });

        if (filteredServices.length === 0) {
            return false;
        }

        tile.services = filteredServices.sort((service1, service2) => service1.title.localeCompare(service2.title));

        return tile.title.toLowerCase().includes(searchCriteria.toLowerCase()) || filteredServices.length > 0;
    });
};
