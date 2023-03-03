/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import { RacfCommands } from "../../src/api/RacfCommands";
import {CsvParser} from "../../lib/api/CsvParser";
import {ImperativeError} from "@zowe/imperative";

describe("Racf Commands", () => {
    it('should create the commands', () => {
        const csvParser = new CsvParser('__tests__/__resources__/csv/users.csv');
        const identities = csvParser.getIdentities();

        const racfCommands = new RacfCommands("ldap://host:1234", identities);
        const commands = racfCommands.getCommands();
        expect(commands).toMatchSnapshot();
    });

    it('should throw error when config is not valid', () => {
        const csvParser = new CsvParser('__tests__/__resources__/csv/invalid_identities.csv');
        const identities = csvParser.getIdentities();

        const racfCommands = new RacfCommands("ldap://host:1234", identities);
        expect(() => racfCommands.getCommands()).toThrow(ImperativeError);
    });
});
