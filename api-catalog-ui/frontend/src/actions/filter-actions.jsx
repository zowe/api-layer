/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import { CLEAR_FILTER, FILTER_TEXT } from '../constants/filter-constants';

export function filterText(text = '') {
    return {
        type: FILTER_TEXT,
        text,
    };
}

export function clear() {
    return {
        type: CLEAR_FILTER,
        defaultFilter: '',
    };
}
