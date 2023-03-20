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
import {Constants} from "../../../../src/api/Constants";
import {expect, describe, it, beforeAll, afterAll} from '@jest/globals';

// Test environment will be populated in the "beforeAll"
let TEST_ENVIRONMENT: ITestEnvironment<ITestPropertiesSchema>;

const testCsv = "users.csv";

describe("id-federation map command integration tests", () => {

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

        expect(response.status).toBe(Constants.ZOWE_ERROR_CODE);
        expect(response.stderr.toString()).toMatchSnapshot();
        expect(response.stdout.toString()).toMatchSnapshot();
    });

    it("should fail when arguments are not valid", () => {
        const response = runCliScript(__dirname + "/__scripts__/map_error_handler_invalid_argument.sh", TEST_ENVIRONMENT, [csv]);

        expect(response.status).toBe(Constants.ZOWE_ERROR_CODE);
        expect(response.stderr.toString()).toMatchSnapshot();
        expect(response.stdout.toString()).toMatchSnapshot();
    });

    it("should print the successful creation message from old school profile and other sources", () => {
        const response = runCliScript(__dirname + "/__scripts__/map_old_profiles.sh", TEST_ENVIRONMENT, [csv]);

        expect(stripProfileDeprecationMessages(response.stderr)).toBe("");
        expect(response.status).toBe(Constants.OKAY_CODE);
        expect(response.stdout.toString()).toContain("idf_ACF2_TST1.jcl' was created. Review and submit this JCL on the system TST1.");
        expect(response.stdout.toString()).toMatchSnapshot();
    });

    it("should print the successful creation message with passed args", () => {
        const response = runCliScript(__dirname + "/__scripts__/map_with_passed_args.sh", TEST_ENVIRONMENT,
            [`${TEST_ENVIRONMENT.workingDir}/users.csv`, "TSS", "TST1", "ldap://12.34.56.78:910"]);

        expect(response.status).toBe(Constants.OKAY_CODE);
        expect(response.stderr.toString()).toBe("");
        expect(response.stdout.toString()).toMatchSnapshot();
    });

    it("should return command error in case of missing arguments", () => {
        const response = runCliScript(__dirname + "/__scripts__/map_with_passed_args.sh", TEST_ENVIRONMENT,
            [csv, "TSS", "", "ldap://12.34.56.78:910"]);

        expect(response.status).toBe(Constants.ZOWE_ERROR_CODE);
        expect(response.stderr.toString()).toMatchSnapshot();
        expect(response.stdout.toString()).toMatchSnapshot();
    });

});
