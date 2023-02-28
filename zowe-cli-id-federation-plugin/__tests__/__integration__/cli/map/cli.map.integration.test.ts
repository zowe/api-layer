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
const wrongTestCsv = "invalid_identities.csv";
const wrongFormatTestCsv = "invalid_format.csv";

describe("id-federation map command", () => {

    let csv: string;
    let wrongCsv: string;
    let wrongFormatCsv: string;

    // Create the unique test environment
    beforeAll(async () => {
        TEST_ENVIRONMENT = await TestEnvironment.setUp({
            installPlugin: true,
            testName: "map_command",
            skipProperties: true
        });

        csv = path.join(TEST_ENVIRONMENT.workingDir, testCsv);
        wrongCsv = path.join(TEST_ENVIRONMENT.workingDir, wrongTestCsv);
        wrongFormatCsv = path.join(TEST_ENVIRONMENT.workingDir, wrongFormatTestCsv);
        fs.copyFileSync(path.join(__dirname, "__resources__", testCsv), csv);
        fs.copyFileSync(path.join(__dirname, "__resources__", wrongTestCsv), wrongCsv);
        fs.copyFileSync(path.join(__dirname, "__resources__", wrongFormatTestCsv), wrongFormatCsv);
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

    it("should fail when arguments are not valid", () => {
        const response = runCliScript(__dirname + "/__scripts__/map_error_handler_invalid_argument.sh", TEST_ENVIRONMENT, [csv]);
        expect(response.stderr.toString()).toMatchSnapshot();
        expect(response.status).toBe(1);
        expect(response.stdout.toString()).toMatchSnapshot();
    });

    it("should print the successful creation message from old school profile and other sources", () => {
        const response = runCliScript(__dirname + "/__scripts__/map_old_profiles.sh", TEST_ENVIRONMENT, [csv]);
        expect(stripProfileDeprecationMessages(response.stderr)).toBe("");
        expect(response.status).toBe(0);
        const output = response.stdout.toString();
        expect(output).toContain("idf_ACF2_TST1.jcl' was created. Review and submit this JCL on the system TST1.");
        expect(output).toMatchSnapshot();
    });

    it("should print the successful creation message with passed args", () => {
        const response = runCliScript(__dirname + "/__scripts__/map_with_passed_args.sh", TEST_ENVIRONMENT,
            [`${TEST_ENVIRONMENT.workingDir}/users.csv`, "TSS", "TST1", "ldap://12.34.56.78:910"]);

        expect(response.stderr.toString()).toBe("");
        expect(response.status).toBe(0);
        expect(response.stdout.toString()).toMatchSnapshot();
    });

    it("should return command error in case of missing arguments", () => {
        const response = runCliScript(__dirname + "/__scripts__/map_with_passed_args.sh", TEST_ENVIRONMENT,
            [csv, "TSS", "", "ldap://12.34.56.78:910"]);
        const expectedError = "\n" +
            "Syntax Error:\n" +
            "No value specified for option:\n" +
            "--system\n" +
            "\n" +
            "This option requires a value of type:\n" +
            "string\n" +
            "\n" +
            "Option Description:\n" +
            "The target JES system on which the command will be executed\n" +
            "\n" +
            "Use \"zowe idf map --help\" to view command description, usage, and options.\n"
        expect(response.stderr.toString()).toBe(expectedError);
        expect(response.status).toBe(1);
        expect(response.stdout.toString()).toMatchSnapshot();
    });

    it("should print the successful creation message from team config profile and other sources", () => {
        fs.copyFileSync(path.join(__dirname, "__resources__", configJson), path.join(TEST_ENVIRONMENT.workingDir, configJson));

        const response = runCliScript(__dirname + "/__scripts__/map_team_config.sh", TEST_ENVIRONMENT, [csv]);
        expect(response.stderr.toString()).toBe("");
        expect(response.status).toBe(0);
        const output = response.stdout.toString();
        expect(output).toContain("'idf_RACF_TST2.jcl' was created. Review and submit this JCL on the system TST2");
        expect(output).toMatchSnapshot();
    });

    it("should return command error in case of csv config with invalid identities", () => {
        const response = runCliScript(__dirname + "/__scripts__/map_team_config.sh", TEST_ENVIRONMENT, [wrongCsv]);
        const expectedMessage = "The user name 'Too looooooooooooooooooooooooooooong name' has exceeded maximum length of 32 characters. Identity mapping for the user 'Too looooooooooooooooooooooooooooong name' has not been created.\n" +
            "The distributed user ID ' Dist naaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaame' has exceeded maximum length of 246 characters. Identity mapping for the user 'Name' has not been created.\n" +
            "The mainframe user ID ' mf_too_long' has exceeded maximum length of 8 characters. Identity mapping for the user 'Name' has not been created.\n" +
            "Command Error:\n" +
            "Error when trying to create the identity mapping.\n";
        expect(response.stderr.toString()).toBe(expectedMessage);
        expect(response.status).toBe(1);
        const output = response.stdout.toString();
        expect(output).toMatchSnapshot();
    });

    it("should return command error in case of invalid format csv config", () => {
        const response = runCliScript(__dirname + "/__scripts__/map_team_config.sh", TEST_ENVIRONMENT, [wrongFormatCsv]);
        const expectedMessage = "Command Error:\n" +
            "Invalid CSV format: Invalid Record Length: columns length is 3, got 1 on line 1\n";
        expect(response.stderr.toString()).toBe(expectedMessage);
        expect(response.status).toBe(1);
        const output = response.stdout.toString();
        expect(output).toMatchSnapshot();
    });

    it("should return command error in case of csv config not found", () => {
        const response = runCliScript(__dirname + "/__scripts__/map_team_config.sh", TEST_ENVIRONMENT, ["/wrong/path"]);
        const expectedMessage = "Command Error:\n" +
            "The input CSV file does not exist.\n";
        expect(response.stderr.toString()).toBe(expectedMessage);
        expect(response.status).toBe(1);
        const output = response.stdout.toString();
        expect(output).toMatchSnapshot();
    });
});
