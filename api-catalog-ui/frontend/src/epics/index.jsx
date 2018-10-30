import { combineEpics } from 'redux-observable';
import { fetchTilesPollingEpic } from './fetch-tiles';

// eslint-disable-next-line import/prefer-default-export
export const rootEpic = combineEpics(fetchTilesPollingEpic);
