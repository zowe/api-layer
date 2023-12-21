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

export function selectService(selectedService = {}, selectedTile = '') {
    return {
        type: SELECT_SERVICE,
        selectedService,
        selectedTile,
    };
}

export function clearService() {
    return {
        type: CLEAR_SERVICE,
        selectedService: {},
        selectedTile: '',
    };
}

export function storeContentAnchor(id) {
    return {
        type: STORE_CONTENT_ANCHOR,
        payload: id,
    };
}
