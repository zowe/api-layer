/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import {CsvParser} from "../../lib/api/CsvParser";
import {ImperativeError} from "@zowe/imperative";

describe("Mapper", () => {

    it("should successfully parse the file", () => {
        const csvParser = new CsvParser('__tests__/__resources__/csv/users.csv');

        expect(csvParser.getIdentities()).toMatchSnapshot();
    });

    it("throw error when cannot read file", () => {
        const csvParser = new CsvParser('no.csv');

        expect(() => csvParser.getIdentities()).toThrow(ImperativeError);
    });

    it("throw error when invalid Csv file", () => {
        const csvParser = new CsvParser('__tests__/__resources__/csv/invalid.csv');

        expect(() => csvParser.getIdentities()).toThrow(ImperativeError);
    });

});
