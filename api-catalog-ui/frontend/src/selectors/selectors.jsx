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
