/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import * as log from 'loglevel';
import { of, throwError, timer } from 'rxjs';
import { ofType } from 'redux-observable';
import { catchError, debounceTime, exhaustMap, map, mergeMap, retryWhen, takeUntil } from 'rxjs/operators';
import { FETCH_TILES_REQUEST, FETCH_NEW_TILES_REQUEST, FETCH_TILES_STOP } from '../constants/catalog-tile-constants';
import {
    fetchTilesFailed,
    fetchTilesRetry,
    fetchTilesSuccess,
    fetchNewTilesSuccess,
} from '../actions/catalog-tile-actions';
import { userActions } from '../actions/user-actions';
import getBaseUrl from '../helpers/urls';

const updatePeriod = Number(process.env.REACT_APP_STATUS_UPDATE_PERIOD);
const debounce = Number(process.env.REACT_APP_STATUS_UPDATE_DEBOUNCE);
const scalingDuration = process.env.REACT_APP_STATUS_UPDATE_SCALING_DURATION;

// terminate the epic if you get any of the following Ajax error codes
const terminatingStatusCodes = [500, 401, 403];
// override the termination if any of these APIM message codes are in the response
const excludedMessageCodes = ['ZWEAM104'];

function checkOrigin() {
    // only allow the gateway url to authenticate the user
    let allowOrigin = process.env.REACT_APP_GATEWAY_URL;
    if (
        process.env.REACT_APP_GATEWAY_URL === null ||
        process.env.REACT_APP_GATEWAY_URL === undefined ||
        process.env.REACT_APP_GATEWAY_URL === ''
    ) {
        allowOrigin = window.location.origin;
    }
    if (allowOrigin === null || allowOrigin === undefined) {
        throw new Error('Allow Origin is not set for Login/Logout process');
    }
    return allowOrigin;
}

/**
 * Construct the URL to fetch container data from the service
 * @param action the payload will contain a container Id
 * @returns the URL to call
 */
function getUrl(action) {
    let url = `${getBaseUrl()}${process.env.REACT_APP_CATALOG_UPDATE}`;
    if (action.payload !== undefined) {
        url += `/${action.payload}`;
    }
    return url;
}

/**
 * Check the error to see if we should terminate or retry the fetch
 * @param error the error thrown
 * @returns {boolean} true if terminate
 */
function shouldTerminate(error) {
    let terminate = terminatingStatusCodes.find((e) => e === error.status);
    if (terminate) {
        if (error.response.messages !== undefined) {
            const apiError = error.response.messages[0];
            if (apiError !== null && apiError !== undefined && apiError.messageNumber !== undefined) {
                // if this APIM message number is in the response, then override the terminate flag
                if (excludedMessageCodes.find((e) => e === apiError.messageNumber)) {
                    terminate = false;
                }
            }
        }
    }
    return terminate;
}

export const retryMechanism =
    (scheduler) =>
    ({ maxRetries = Number(process.env.REACT_APP_STATUS_UPDATE_MAX_RETRIES) } = {}) =>
    (attempts) =>
        attempts.pipe(
            mergeMap((error, i) => {
                const retryAttempt = i + 1;
                if (shouldTerminate(error)) {
                    return throwError(() => error);
                }
                // used for display Toast retry notification
                fetchTilesRetry(retryAttempt, maxRetries);
                if (retryAttempt > maxRetries) {
                    const message = `Could not retrieve tile info after ${maxRetries} attempts. Stopping fetch process.`;
                    log.error(message);
                    const err = new Error(message);
                    return throwError(() => err);
                }
                const msg = `Attempt ${retryAttempt}: retrying in ${scalingDuration * retryAttempt}s`;
                log.warn(msg);
                return timer(scalingDuration * retryAttempt, scheduler);
            })
        );

const createFetchTilesEpic =
    (fetchActionType, fetchSuccessAction, fetchFailedAction, fetchStopAction, url, origin, retry) =>
    (action$, _store, { ajax, scheduler }) =>
        action$.pipe(
            ofType(fetchActionType),
            debounceTime(debounce, scheduler),
            mergeMap((action) =>
                timer(0, updatePeriod, scheduler).pipe(
                    exhaustMap(() =>
                        ajax({
                            url: url(action),
                            method: 'GET',
                            credentials: 'include',
                            headers: {
                                'Content-Type': 'application/json',
                                'Access-Control-Allow-Origin': origin(),
                                'X-Requested-With': 'XMLHttpRequest',
                            },
                        }).pipe(
                            map((ajaxResponse) => {
                                const { response } = ajaxResponse;
                                if (response === null || response.length === 0) {
                                    return fetchFailedAction(
                                        action.payload.length > 0
                                            ? new Error(
                                                  `Could not retrieve details for Tile with ID: ${action.payload}`
                                              )
                                            : new Error(`Could not retrieve any Tiles`)
                                    );
                                }
                                return fetchSuccessAction(response);
                            }),
                            retryWhen(retry(scheduler)())
                        )
                    ),
                    takeUntil(action$.pipe(ofType(fetchStopAction))),
                    catchError((error) => {
                        if (error.status === 401 || error.status === 403) {
                            return of(userActions.authenticationFailure(error));
                        }
                        return of(fetchFailedAction(error));
                    })
                )
            )
        );

/**
 * Epic used to schedule call to fetch tiles from the containers endpoint
 * @param action$ redux action
 * @param _store redux store
 * @param ajax ajax call
 * @param scheduler scheduler
 */
export const fetchTilesPollingEpic = createFetchTilesEpic(
    FETCH_TILES_REQUEST,
    fetchTilesSuccess,
    fetchTilesFailed,
    FETCH_TILES_STOP,
    getUrl,
    checkOrigin,
    retryMechanism
);

/**
 * Epic used to schedule call to fetch tiles from the containers endpoint. This is required to dynamically
 * populate the navigation side bar in the detail page and refresh it based on the scheduled period
 * @param action$ redux action
 * @param _store redux store
 * @param ajax ajax call
 * @param scheduler scheduler
 */
export const fetchTilesPollingEpic2 = createFetchTilesEpic(
    FETCH_NEW_TILES_REQUEST,
    fetchNewTilesSuccess,
    fetchTilesFailed,
    FETCH_TILES_STOP,
    getUrl,
    checkOrigin,
    retryMechanism
);
