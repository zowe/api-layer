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
import {IHandlerResponseApi} from "@zowe/imperative/lib/cmd/src/doc/response/api/handler/IHandlerResponseApi";
import {expect, jest, describe, it} from '@jest/globals';
import * as JobUtil from "../../src/api/JobUtil";

const mockGetCommands = jest.fn().mockReturnValue(['abc']);
const mockGetTssCommands = jest.fn().mockReturnValue(['abc']);

jest.mock('../../src/api/RacfCommands', () => {
    return {
        RacfCommands: jest.fn().mockImplementation(() => {
            return {
                getCommands: mockGetCommands
            };
        })
    };
});

jest.mock('../../src/api/TssCommands', () => {
    return {
        TssCommands: jest.fn().mockImplementation(() => {
            return {
                getCommands: mockGetTssCommands
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
        const mapper = new Mapper(INPUT_FILE, ESM, SYSTEM, REGISTRY, {} as IHandlerResponseApi);

        expect(mapper.file).toBe(INPUT_FILE);
        expect(mapper.esm).toBe(ESM);
        expect(mapper.system).toBe(SYSTEM);
        expect(mapper.registry).toBe(REGISTRY);
    });

    describe("ESM function is called when", () => {

        it("is RACF", () => {
            const mapper = new Mapper(INPUT_FILE, "RACF", SYSTEM, REGISTRY, {} as IHandlerResponseApi);
            const commandProcessor = mapper.createSafCommands([]);

            expect(commandProcessor).toHaveLength(1);
            expect(mockGetCommands).toHaveBeenCalledTimes(1);
        });

        it("is ACF2", () => {
            // TODO define when ACF2 is available
        });

        it("is TopSecret", () => {
            const mapper = new Mapper(INPUT_FILE, "TSS", SYSTEM, REGISTRY, {} as IHandlerResponseApi);
            const commandProcessor = mapper.createSafCommands([]);

            expect(commandProcessor).toHaveLength(1);
            expect(mockGetTssCommands).toHaveBeenCalledTimes(1);
        });
    });

    describe("createJcl unit test", () => {

        it("creates with mustache template", async () => {
            const mapper = new Mapper(INPUT_FILE, "RACF", SYSTEM, REGISTRY, {} as IHandlerResponseApi);
            jest.spyOn(JobUtil, "getAccount").mockReturnValue(Promise.resolve("account1"));
            const reply = await mapper.createJcl(["command1", "command2"]);

            expect(reply).toMatchSnapshot();
        });

    });

});
