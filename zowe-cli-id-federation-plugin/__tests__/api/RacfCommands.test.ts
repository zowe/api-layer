/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import {Commands} from "../../src/api/Commands";
import {CsvParser} from "../../src/api/CsvParser";
import {ImperativeError} from "@zowe/imperative";
import {ResponseMock} from "../__src__/ResponseMock";
import {Constants} from "../../src/api/Constants";
import {expect, describe, it} from '@jest/globals';
import * as fs from "fs";

describe("Racf Commands unit test", () => {

    it('should create the commands without warning', () => {
        const racfCommands = getRacfCommands('__tests__/__resources__/csv/users.csv');

        expect(racfCommands.commands.getCommands()).toMatchSnapshot();
        expect(racfCommands.response.exitCode).toBe(Constants.OKAY_CODE);
    });

    it('should create the commands with warning', () => {
        const racfCommands = getRacfCommands('__tests__/__resources__/csv/users_with_warnings.csv');

        expect(racfCommands.commands.getCommands()).toMatchSnapshot();
        expect(racfCommands.response.exitCode).toBe(Constants.WARN_CODE);
    });


    it('should throw error when config is not valid', () => {
        const racfCommands = getRacfCommands('__tests__/__resources__/csv/invalid_identities.csv');

        expect(() => racfCommands.commands.getCommands()).toThrow(ImperativeError);
        expect(racfCommands.response.exitCode).toBe(Constants.FATAL_CODE);
    });

});

function getRacfCommands(file: string): { commands: Commands, response: ResponseMock } {
    const response = new ResponseMock();
    const csvParser = new CsvParser(file, response);
    const racfTemplate = fs.readFileSync('src/api/templates/racf.jcl').toString();
    const racfRefreshCommand = fs.readFileSync('src/api/templates/racf_refresh.jcl').toString();
    const commands = new Commands('ldap://host:1234', csvParser.getIdentities(), racfTemplate, racfRefreshCommand, response);

    return {commands, response};
}
