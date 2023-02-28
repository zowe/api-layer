/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import * as path from "path";
import * as fs from "fs";
import { ITestEnvironment, TestEnvironment, runCliScript } from "@zowe/cli-test-utils";
import { ITestPropertiesSchema } from "../../../__src__/environment/doc/ITestPropertiesSchema";

// Test environment will be populated in the "beforeAll"
let TEST_ENVIRONMENT: ITestEnvironment<ITestPropertiesSchema>;
const testCsv = "users.csv";

describe("id-federation map", () => {
    let csv: string;
    // Create the unique test environment
    beforeAll(async () => {
        TEST_ENVIRONMENT = await TestEnvironment.setUp({
            installPlugin: true,
            testName: "map_command"
        });
        csv = path.join(TEST_ENVIRONMENT.workingDir, testCsv);
        fs.copyFileSync(path.join(__dirname, "__resources__", testCsv), csv);
    });

    afterAll(async () => {
        await TestEnvironment.cleanUp(TEST_ENVIRONMENT);
    });

    it("should print the successful creation message", () => {
        const response = runCliScript(__dirname + "/__scripts__/map.sh", TEST_ENVIRONMENT,
            [`${TEST_ENVIRONMENT.workingDir}/users.csv`, "TSS", "TST1", "ldap://12.34.56.78:910"]);

        expect(response.stderr.toString()).toBe("");
        expect(response.status).toBe(0);
        expect(response.stdout.toString()).toMatchSnapshot();
    });

    it("should display the help", () => {
        const response = runCliScript(__dirname + "/__scripts__/map_help.sh", TEST_ENVIRONMENT);

        expect(response.stderr.toString()).toBe("");
        expect(response.status).toBe(0);
        expect(response.stdout.toString()).toMatchSnapshot();
    });
});
