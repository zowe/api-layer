/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import {RacfCommands} from "../../src/api/RacfCommands";
import {CsvParser} from "../../lib/api/CsvParser";
import {ImperativeError} from "@zowe/imperative";
import {ResponseMock} from "../__src__/ResponseMock";
import {Constants} from "../../lib/api/Constants";

describe("Racf Commands unit test", () => {

    it('should create the commands without warning', () => {
        const racfCommands = getRacfCommands('__tests__/__resources__/csv/users.csv');

        expect(racfCommands.commands.getCommands()).toMatchSnapshot();
        expect(racfCommands.response.exitCode).toBe(Constants.okayCode);
    });

    it('should create the commands with warning', () => {
        const racfCommands = getRacfCommands('__tests__/__resources__/csv/users_with_warnings.csv');

        expect(racfCommands.commands.getCommands()).toMatchSnapshot();
        expect(racfCommands.response.exitCode).toBe(Constants.warnCode);
    });


    it('should throw error when config is not valid', () => {
        const racfCommands = getRacfCommands('__tests__/__resources__/csv/invalid_identities.csv');

        expect(() => racfCommands.commands.getCommands()).toThrow(ImperativeError);
        expect(racfCommands.response.exitCode).toBe(Constants.fatalCode);
    });

});

function getRacfCommands(file: string): { commands: RacfCommands, response: ResponseMock } {
    const response = new ResponseMock();
    const csvParser = new CsvParser(file, response);
    const commands = new RacfCommands('ldap://host:1234', csvParser.getIdentities(), response);

    return {commands, response};
}
