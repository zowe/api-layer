/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import {CsvParser} from "../../src/api/CsvParser";
import {ImperativeError} from "@zowe/imperative";
import {ResponseMock} from "../__src__/ResponseMock";
import {Constants} from "../../src/api/Constants";
import {expect, jest, describe, it} from '@jest/globals';

describe("CSV parser unit tests", () => {

    it("should successfully parse the file", () => {
        const response = new ResponseMock();
        const csvParser = new CsvParser('__tests__/__resources__/csv/users.csv', response);

        expect(csvParser.getIdentities()).toMatchSnapshot();
        expect(response.exitCode).toBe(Constants.okayCode);
    });

    it("throw error when cannot read file", () => {
        const response = new ResponseMock();
        const csvParser = new CsvParser('no.csv', response);

        expect(() => csvParser.getIdentities()).toThrow(ImperativeError);
        expect(response.exitCode).toBe(Constants.fatalCode);
    });

    it("throw error when invalid Csv file", () => {
        const response = new ResponseMock();
        const csvParser = new CsvParser('__tests__/__resources__/csv/invalid.csv', response);

        expect(() => csvParser.getIdentities()).toThrow(ImperativeError);
        expect(response.exitCode).toBe(Constants.fatalCode);
    });

});
