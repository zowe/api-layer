/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import { CLEAR_SERVICE, SELECT_SERVICE, STORE_CONTENT_ANCHOR } from '../constants/selected-service-constants';
import { selectService, clearService, storeContentAnchor } from './selected-service-actions';

describe('>>> Selected Service actions tests', () => {
    it('should return selected service', () => {
        const selectedService = { id: 'service' };
        const selectedTile = 'Tile';
        const expectedAction = {
            type: SELECT_SERVICE,
            selectedService: { id: 'service' },
            selectedTile: 'Tile',
        };
        expect(selectService(selectedService, selectedTile)).toEqual(expectedAction);
    });

    it('should return clear service', () => {
        const expectedAction = {
            type: CLEAR_SERVICE,
            selectedService: {},
            selectedTile: '',
        };
        expect(clearService()).toEqual(expectedAction);
    });

    it('should return store content anchor', () => {
        const expectedAction = {
            type: STORE_CONTENT_ANCHOR,
            payload: '#id',
        };
        expect(storeContentAnchor('#id')).toEqual(expectedAction);
    });
});
