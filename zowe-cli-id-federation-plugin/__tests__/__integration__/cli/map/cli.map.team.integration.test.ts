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
import {ITestEnvironment, runCliScript, TestEnvironment} from "@zowe/cli-test-utils";
import {ITestPropertiesSchema} from "../../../__src__/environment/doc/ITestPropertiesSchema";
import {Constants} from "../../../../src/api/Constants";
import {expect, describe, it, beforeAll, afterAll} from '@jest/globals';

// Test environment will be populated in the "beforeAll"
let TEST_ENVIRONMENT: ITestEnvironment<ITestPropertiesSchema>;

const configJson = "zowe.config.json";
const testCsv = "users.csv";
const invalidTestCsv = "invalid_identities.csv";
const allInvalidTestCsv = "all_invalid_identities.csv";
const wrongFormatTestCsv = "invalid_format.csv";

describe("id-federation map command integration tests", () => {

    let csv: string;
    let invalidCsv: string;
    let allInvalidCsv: string;
    let wrongFormatCsv: string;

    // Create the unique test environment
    beforeAll(async () => {
        TEST_ENVIRONMENT = await TestEnvironment.setUp({
            installPlugin: true,
            testName: "map_command",
            skipProperties: true
        });

        csv = path.join(TEST_ENVIRONMENT.workingDir, testCsv);
        invalidCsv = path.join(TEST_ENVIRONMENT.workingDir, invalidTestCsv);
        allInvalidCsv = path.join(TEST_ENVIRONMENT.workingDir, allInvalidTestCsv);
        wrongFormatCsv = path.join(TEST_ENVIRONMENT.workingDir, wrongFormatTestCsv);
        fs.copyFileSync(path.join(__dirname, "__resources__", testCsv), csv);
        fs.copyFileSync(path.join(__dirname, "__resources__", invalidTestCsv), invalidCsv);
        fs.copyFileSync(path.join(__dirname, "__resources__", allInvalidTestCsv), allInvalidCsv);
        fs.copyFileSync(path.join(__dirname, "__resources__", wrongFormatTestCsv), wrongFormatCsv);
        fs.copyFileSync(path.join(__dirname, "__resources__", configJson), path.join(TEST_ENVIRONMENT.workingDir, configJson));
    });

    afterAll(async () => {
        await TestEnvironment.cleanUp(TEST_ENVIRONMENT);
    });

    it("should print the successful creation message from team config profile and other sources", () => {
        const response = runCliScript(__dirname + "/__scripts__/map_team_config.sh", TEST_ENVIRONMENT, [csv]);

        expect(response.status).toBe(Constants.okayCode);
        expect(response.stderr.toString()).toBe("");
        expect(response.stdout.toString()).toMatchSnapshot();
    });

    it("should return command error in case of csv config with some invalid identities", () => {
        const response = runCliScript(__dirname + "/__scripts__/map_team_config.sh", TEST_ENVIRONMENT, [invalidCsv]);

        expect(response.status).toBe(Constants.warnCode);
        expect(response.stderr.toString()).toMatchSnapshot();
        expect(response.stdout.toString()).toMatchSnapshot();
    });

    it("should return command error in case of csv config with all invalid identities", () => {
        const response = runCliScript(__dirname + "/__scripts__/map_team_config.sh", TEST_ENVIRONMENT, [allInvalidCsv]);

        expect(response.status).toBe(Constants.fatalCode);
        expect(response.stderr.toString()).toMatchSnapshot();
        expect(response.stdout.toString()).toMatchSnapshot();
    });


    it("should return command error in case of invalid format csv config", () => {
        const response = runCliScript(__dirname + "/__scripts__/map_team_config.sh", TEST_ENVIRONMENT, [wrongFormatCsv]);

        expect(response.status).toBe(Constants.fatalCode);
        expect(response.stderr.toString()).toMatchSnapshot();
        expect(response.stdout.toString()).toMatchSnapshot();
    });

    it("should return command error in case of csv config not found", () => {
        const response = runCliScript(__dirname + "/__scripts__/map_team_config.sh", TEST_ENVIRONMENT, ["/wrong/path"]);

        expect(response.status).toBe(Constants.fatalCode);
        expect(response.stderr.toString()).toMatchSnapshot();
        expect(response.stdout.toString()).toMatchSnapshot();
    });

});
