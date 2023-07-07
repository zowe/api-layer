/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import countAdditionalContents from './utilFunctions';

describe('>>> Util Functions tests', () => {
    it('should count medias', () => {
        const service = {
            id: 'service',
            useCases: ['usecase1', 'usecase2'],
            tutorials: [],
            videos: [],
        };
        expect(countAdditionalContents(service)).toEqual({ tutorialsCounter: 0, useCasesCounter: 2, videosCounter: 0 });
    });
});
