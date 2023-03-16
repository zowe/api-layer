/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import { hasValidLength } from "../../src/api/ValidateUtil";
import {expect, describe, it} from '@jest/globals';

describe("ValidateUtil unit tests", () => {

    it('should return true if string length is valid', () => {
        expect(hasValidLength("valid", 8)).toBeTruthy();
    });

    it('should return true if string is too long', () => {
        expect(hasValidLength("toooooooooooolong", 8)).toBeFalsy();
    });

});
