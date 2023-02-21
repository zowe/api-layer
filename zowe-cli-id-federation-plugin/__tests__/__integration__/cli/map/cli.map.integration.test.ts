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
import * as path from "path";
import {ITestEnvironment, runCliScript, stripProfileDeprecationMessages, TestEnvironment} from "@zowe/cli-test-utils";
import {ITestPropertiesSchema} from "../../../__src__/environment/doc/ITestPropertiesSchema";

// Test environment will be populated in the "beforeAll"
let TEST_ENVIRONMENT: ITestEnvironment<ITestPropertiesSchema>;

const configJson = "zowe.config.json";
const testCsv = "users.csv";

describe("id-federation map command", () => {

    let csv: string;

    // Create the unique test environment
    beforeAll(async () => {
        TEST_ENVIRONMENT = await TestEnvironment.setUp({
            installPlugin: true,
            testName: "map_command",
            skipProperties: true
        });

        csv = path.join(TEST_ENVIRONMENT.workingDir, testCsv);
        fs.copyFileSync(path.join(__dirname, "__resources__", testCsv), csv);
    });

    afterAll(async () => {
        await TestEnvironment.cleanUp(TEST_ENVIRONMENT);
    });

    it("should fail when options are not passed", () => {
        const response = runCliScript(__dirname + "/__scripts__/map_error_handler.sh", TEST_ENVIRONMENT, [csv]);
        expect(response.stderr.toString()).toMatchSnapshot();
        expect(response.status).toBe(1);
        expect(response.stdout.toString()).toMatchSnapshot();
    });

    it("should print input args from team config profile and other sources", () => {
        fs.copyFileSync(path.join(__dirname, "__resources__", configJson), path.join(TEST_ENVIRONMENT.workingDir, configJson));

        const response = runCliScript(__dirname + "/__scripts__/map_team_config.sh", TEST_ENVIRONMENT, [csv]);
        expect(response.stderr.toString()).toBe("");
        expect(response.status).toBe(0);
        const output = response.stdout.toString();
        expect(output).toContain("ESM: RACF");
        expect(output).toContain("LPAR: TST2");
        expect(output).toContain("Registry: ldap://12.34.56.78:910");
        expect(output).toMatchSnapshot();
    });

    it("should print input args from old school profile and other sources", () => {
        const response = runCliScript(__dirname + "/__scripts__/map_old_profiles.sh", TEST_ENVIRONMENT, [csv]);
        expect(stripProfileDeprecationMessages(response.stderr)).toBe("");
        expect(response.status).toBe(0);
        const output = response.stdout.toString();
        expect(output).toContain("ESM: ACF2");
        expect(output).toContain("LPAR: TST1");
        expect(output).toContain("Registry: ldap://zowe.org");
        expect(output).toMatchSnapshot();
    });
});
