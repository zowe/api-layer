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
import {ResponseMock} from '../__src__/ResponseMock';
import {Constants} from '../../src/api/Constants';

describe("Tss Commands", () => {

    it('should create the commands', () => {
        const tssCommands = getTssCommands('__tests__/__resources__/csv/users.csv');
        const commands = tssCommands.commands.getCommands();
        expect(commands).toMatchSnapshot();
        expect(tssCommands.response.exitCode).toBe(Constants.okayCode);
    });

    it('should create the commands with warning', () => {
        const tssCommands = getTssCommands('__tests__/__resources__/csv/users_with_warnings.csv');
        const commands = tssCommands.commands.getCommands();
        expect(commands).toMatchSnapshot();
        expect(tssCommands.response.exitCode).toBe(Constants.warnCode);
    });

    it('should throw error when config is not valid', () => {
        const tssCommands = getTssCommands('__tests__/__resources__/csv/invalid_identities.csv');
        expect(() => tssCommands.commands.getCommands()).toThrow(ImperativeError);
        expect(tssCommands.response.exitCode).toBe(Constants.fatalCode);
    });
});

function getTssCommands(file: string): { commands: Commands, response: ResponseMock } {
    const response = new ResponseMock();
    const csvParser = new CsvParser(file, response);
    const commands = new Commands('ldap://host:1234', csvParser.getIdentities(), "tss", response);

    return {commands, response};
}
