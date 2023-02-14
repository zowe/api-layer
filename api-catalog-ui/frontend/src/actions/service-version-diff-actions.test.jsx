/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import { getDiff } from './service-version-diff-actions';

describe('>>> Service version diff actions tests', () => {

    it('should get diff', async () => {
        const dispatch = jest.fn();
        const error = {

        }
        const expectedAction = {
            newVersion: '2.0.0',
            oldVersion: '1.0.0',
            serviceId: 'service',
            type: 'REQUEST_VERSION_DIFF',
        };
        await getDiff('service', '1.0.0', '2.0.0')(dispatch);
        expect(dispatch.mock.calls[0][0]).toStrictEqual(expectedAction);
    });
});
