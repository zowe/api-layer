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
 * Custom Plugin which extends the SwaggerUI filter functionality to filter APIs by tag, summary and description
 */
// eslint-disable-next-line import/prefer-default-export
export const AdvancedFilterPlugin = function (system) {
    return {
        fn: {
            opsFilter: (taggedOps, phrase) => {
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
            },
        },
    };
};
