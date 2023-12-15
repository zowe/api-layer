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

function filterService(searchCriteria, service) {
    if (!searchCriteria || searchCriteria.length === 0) {
        return true;
    }
    if (!service.title) {
        return false;
    }
    return service.title.toLowerCase().includes(searchCriteria.toLowerCase());
}

function compareResult(searchCriteria, tile, filteredServices) {
    if (!searchCriteria || searchCriteria.length === 0) {
        return true;
    }
    if (!tile.title) {
        return false;
    }
    return tile.title.toLowerCase().includes(searchCriteria.toLowerCase()) || filteredServices.length > 0;
}

// eslint-disable-next-line
/**
 * Filters the services in the dashboard and navigation bar based on the search criteria
 * @param tiles
 * @param searchCriteria
 * @returns services the filtered services that matches the criteria
 */
export const getFilteredServices = (tiles, searchCriteria) => {
    if (!tiles || tiles.length === 0) {
        return [];
    }

    const filteredTiles = JSON.parse(JSON.stringify(tiles));

    return filteredTiles
        .filter((tile) => tile?.title)
        .filter((tile) => {
            const filteredServices = tile.services.filter((service) => filterService(searchCriteria, service));

            if (filteredServices.length === 0) {
                return false;
            }

            tile.services = filteredServices.sort((service1, service2) => service1.title.localeCompare(service2.title));
            return compareResult(searchCriteria, tile, filteredServices);
        });
};

export function sortServices(services) {
    return services
        .flatMap((tile) => tile.services)
        .sort((service1, service2) => service1.title.localeCompare(service2.title));
}
