/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import {Acf2Commands} from "../../src/api/Acf2Commands";
import {CsvParser} from "../../src/api/CsvParser";
import {ImperativeError} from "@zowe/imperative";
import {ResponseMock} from "../__src__/ResponseMock";
import {Constants} from "../../src/api/Constants";

describe("ACF2 Commands unit test", () => {

    it('should create the commands without warning', () => {
        const acf2Commands = getAcf2Commands('__tests__/__resources__/csv/users.csv');

        expect(acf2Commands.commands.getCommands()).toMatchSnapshot();
        expect(acf2Commands.response.exitCode).toBe(Constants.OKAY_CODE);
    });

    it('should create the commands with warning', () => {
        const acf2Commands = getAcf2Commands('__tests__/__resources__/csv/users_with_warnings.csv');

        expect(acf2Commands.commands.getCommands()).toMatchSnapshot();
        expect(acf2Commands.response.exitCode).toBe(Constants.WARN_CODE);
    });


    it('should throw error when config is not valid', () => {
        const acf2Commands = getAcf2Commands('__tests__/__resources__/csv/invalid_identities.csv');

        expect(() => acf2Commands.commands.getCommands()).toThrow(ImperativeError);
        expect(acf2Commands.response.exitCode).toBe(Constants.FATAL_CODE);
    });

});

function getAcf2Commands(file: string): { commands: Acf2Commands, response: ResponseMock } {
    const response = new ResponseMock();
    const csvParser = new CsvParser(file, response);
    const commands = new Acf2Commands('ldap://host:1234', csvParser.getIdentities(), response);

    return {commands, response};
}
