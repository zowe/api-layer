/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import { Mapper } from "../../src";
describe("Mapper", () => {
    it("should arguments passed correctly", () => {
        const INPUT_FILE = "fake-file.csv";
        const ESM = "fakeESM";
        const SYSTEM = "fakeLPAR";
        const REGISTRY = "fake://host:1234";

        const maper = new Mapper(INPUT_FILE, ESM, SYSTEM, REGISTRY);

        expect(maper.file).toBe(INPUT_FILE);
        expect(maper.esm).toBe(ESM);
        expect(maper.system).toBe(SYSTEM);
        expect(maper.registry).toBe(REGISTRY);
    });
});
