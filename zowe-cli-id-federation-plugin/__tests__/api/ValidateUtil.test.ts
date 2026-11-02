/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import {hasValidLength, isValidDistributedId, isValidMainframeId, isValidUserName} from "../../src/api/ValidateUtil";
import {expect, describe, it} from '@jest/globals';

describe("ValidateUtil unit tests", () => {

    it('should return true if string length is valid', () => {
        expect(hasValidLength("valid", 8)).toBeTruthy();
    });

    it('should return true if string is too long', () => {
        expect(hasValidLength("toooooooooooolong", 8)).toBeFalsy();
    });

    it('provided MF userId with enter returns false', () => {
        expect(isValidMainframeId("\ninv")).toBeFalsy();
    });

    it('provided invalid MF userId with exclamation mark returns false', () => {
        expect(isValidMainframeId("user !")).toBeFalsy();
    });

    it('provided valid MF userId returns true', () => {
        expect(isValidMainframeId("MFUSER")).toBeTruthy();
    });

    it('provided valid MF userId with enter returns false', () => {
        expect(isValidMainframeId("MFUSER\n")).toBeFalsy();
    });

    it('provided distributed label with enter returns false', () => {
        expect(isValidDistributedId("\ninv")).toBeFalsy();
    });

    it('provided invalid distributed label with slash returns false', () => {
        expect(isValidDistributedId("dept01/user")).toBeFalsy();
    });

    it('provided valid distributed label as email returns false', () => {
        expect(isValidDistributedId("user@oidc.com")).toBeTruthy();
    });

    it('provided invalid username with enter returns false', () => {
        expect(isValidUserName("\ninv")).toBeFalsy();
    });

    it('provided invalid username userId with slash mark returns false', () => {
        expect(isValidUserName("user_name")).toBeFalsy();
    });

    it('provided valid username as email return true', () => {
        expect(isValidUserName("user@oidc.com")).toBeTruthy();
    });

});
