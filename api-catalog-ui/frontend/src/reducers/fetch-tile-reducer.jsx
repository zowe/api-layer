import {
    FETCH_TILES_FAILED,
    FETCH_TILES_REQUEST,
    FETCH_TILES_RETRY,
    FETCH_TILES_STOP,
    FETCH_TILES_SUCCESS,
    FETCH_SERVICE_DOC,
} from '../constants/catalog-tile-constants';

const tilesReducerDefaultState = {
    tile: {},
    tiles: [],
    id: '',
    error: null,
};

const tilesReducer = (state = tilesReducerDefaultState, action) => {
    switch (action.type) {
        case FETCH_TILES_SUCCESS:
            return { ...state, tiles: [...action.payload], error: null };
        case FETCH_TILES_FAILED:
            return { tiles: state.tiles, id: '', error: action.payload };
        case FETCH_TILES_REQUEST:
            return { tiles: [], id: action.payload, error: null };
        case FETCH_TILES_RETRY:
            return state;
        case FETCH_TILES_STOP:
            return state;
        case FETCH_SERVICE_DOC:
            return { tiles: state.tiles, id: action.payload, error: null}
        default:
            return state;
    }
};

export default tilesReducer;
