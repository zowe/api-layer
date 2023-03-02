/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import { ProfileConstants } from "../../src/api/ProfileConstants";

describe("Profile Constants", () => {
    it('should return correct values for all the constants', () => {
        expect(ProfileConstants.IDF_OPTION_ESM).toMatchSnapshot();
        expect(ProfileConstants.IDF_OPTION_SYSTEM).toMatchSnapshot();
        expect(ProfileConstants.IDF_OPTION_REGISTRY).toMatchSnapshot();
        expect(ProfileConstants.IDF_CONNECTION_OPTION_GROUP).toMatchSnapshot();
        expect(ProfileConstants.IDF_CONNECTION_OPTIONS).toMatchSnapshot();
        expect(ProfileConstants.IDF_CONNECTION_OPTION_GROUP).toBe("IDF Connection Options");
    });
});
