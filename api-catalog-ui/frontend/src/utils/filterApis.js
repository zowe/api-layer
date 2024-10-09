/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

/**
 * Extend the filter by allowing to search not only by tags, but also by description and summary.
 * The filter is also case-insensitive.
 * @param phrase the search input
 * @param taggedOps the API doc
 * @param system the system
 * @returns {*} the filtered API operation
 */
export function extendFilter(phrase, taggedOps, system) {
    // eslint-disable-next-line no-param-reassign
    phrase = phrase.toLowerCase();
    const normalTaggedOps = JSON.parse(JSON.stringify(taggedOps));
    Object.keys(normalTaggedOps).forEach((tagObj) => {
        const { operations } = normalTaggedOps[tagObj];
        let i = operations.length;
        // eslint-disable-next-line no-plusplus
        while (i--) {
            const { operation } = operations[i];
            if (
                operations[i].path.toLowerCase().indexOf(phrase) === -1 &&
                operation.summary !== undefined &&
                operation.description !== undefined &&
                operation.summary.toLowerCase().indexOf(phrase) === -1 &&
                operation.description.toLowerCase().indexOf(phrase) === -1
            ) {
                operations.splice(i, 1);
            }
        }
        if (operations.length === 0) {
            delete normalTaggedOps[tagObj];
        } else {
            normalTaggedOps[tagObj].operations = operations;
        }
    });
    return system.Im.fromJS(normalTaggedOps);
}

/**
 * Custom Plugin which extends the SwaggerUI filter functionality to filter APIs by tag, summary and description
 */
export const AdvancedFilterPlugin = function (system) {
    return {
        fn: {
            opsFilter: (taggedOps, phrase) => extendFilter(phrase, taggedOps, system),
        },
    };
};
