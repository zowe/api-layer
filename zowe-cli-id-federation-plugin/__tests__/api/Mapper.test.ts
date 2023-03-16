/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import { Mapper } from "../../src";
import {ImperativeError} from '@zowe/imperative';
import {ResponseMock} from '../__src__/ResponseMock';
import {expect, jest, describe, it} from '@jest/globals';

const mockGetCommands = jest.fn().mockReturnValue(['abc']);

jest.mock('../../src/api/Commands', () => {
    return {
        Commands: jest.fn().mockImplementation(() => {
            return {
                getCommands: mockGetCommands
            };
        })
    };
});

describe("Mapper", () => {

        const INPUT_FILE = "fake-file.csv";
        const ESM = "fakeESM";
        const SYSTEM = "fakeLPAR";
        const REGISTRY = "fake://host:1234";

    it("should arguments passed correctly", () => {
        const mapper = new Mapper(INPUT_FILE, ESM, SYSTEM, REGISTRY, new ResponseMock());

        expect(mapper.file).toBe(INPUT_FILE);
        expect(mapper.esm).toBe(ESM);
        expect(mapper.system).toBe(SYSTEM);
        expect(mapper.registry).toBe(REGISTRY);
    });

    describe("ESM function is called when", () => {

        it("is RACF", () => {
            const mapper = new Mapper(INPUT_FILE, "RACF", SYSTEM, REGISTRY, new ResponseMock());
            const commandProcessor = mapper.createSafCommands([]);

            expect(commandProcessor).toHaveLength(1);
            expect(mockGetCommands).toHaveBeenCalledTimes(1);
        });

        it("is ACF2", () => {
            const mapper = new Mapper(INPUT_FILE, "ACF2", SYSTEM, REGISTRY, new ResponseMock());
            const commandProcessor = mapper.createSafCommands([]);

            expect(commandProcessor).toHaveLength(1);
            expect(mockGetCommands).toHaveBeenCalledTimes(1);
        });

        it("is TopSecret", () => {
            const mapper = new Mapper(INPUT_FILE, "TSS", SYSTEM, REGISTRY, new ResponseMock());
            const commandProcessor = mapper.createSafCommands([]);

            expect(commandProcessor).toHaveLength(1);
            expect(mockGetCommands).toHaveBeenCalledTimes(1);
        });

        it('is not supported then throw error ', () => {
            const mapper = new Mapper(INPUT_FILE, ESM, SYSTEM, REGISTRY, new ResponseMock());

            expect(() => mapper.createSafCommands([])).toThrow(ImperativeError);
        });
    });

});
