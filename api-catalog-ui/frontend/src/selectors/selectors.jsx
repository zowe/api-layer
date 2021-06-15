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
export const createLoadingSelector = actions => state => _(actions).some(action => _.get(state.loadingReducer, action));

// eslint-disable-next-line
export const getVisibleTiles = (tiles, searchCriteria) => {
    if (tiles === undefined || tiles === null || tiles.length <= 0) {
        return [];
    }
    return tiles
        .filter(tile => {
            if (searchCriteria === undefined || searchCriteria === null || searchCriteria.length === 0) {
                return true;
            }
            return (
                tile.title.toLowerCase().includes(searchCriteria.toLowerCase()) ||
                tile.description.toLowerCase().includes(searchCriteria.toLowerCase())
            );
        })
        .sort((tile1, tile2) => tile1.title.localeCompare(tile2.title));
};
