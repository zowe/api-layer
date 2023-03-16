/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import * as fs from "fs";
import { Imperative } from "@zowe/imperative";
import {expect, describe, it} from '@jest/globals';

describe("map definition", () => {
    it("should match the snapshot", () => {
        // Attempt to read the full file contents. We could require the module here, however there is normally non-
        // deterministic data (filepaths, etc.) that are resolved when the module is loaded, so it is simpler to
        // check the contents for changes (sanity/protection agaisnt undesired changes to the definition)
        let contents: string = "";
        let error;
        try {
            contents = fs.readFileSync(__dirname + "/../../../src/cli/map/Map.definition.ts").toString();
        } catch (e) {
            error = e;
            Imperative.console.error(`Error reading Map.definition.ts Did you move the file? Details: ${e.message}`);
        }
        expect(error).toBeUndefined();
        expect(contents).toMatchSnapshot();
    });
});
