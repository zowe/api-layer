/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import {getAccount} from "../../src/api/JobUtil";
import {ProfileInfo} from "@zowe/imperative/lib/config/src/ProfileInfo";
import {IProfAttrs} from "@zowe/imperative/lib/config/src/doc/IProfAttrs";
import {ProfLocType} from "@zowe/imperative";
import {IProfMergedArg} from "@zowe/imperative/lib/config/src/doc/IProfMergedArg";
import {expect, jest, describe, it} from '@jest/globals';

describe("JobUtil unit tests", () => {

    const profAttrs: IProfAttrs = {
        profName: "profName1",
        profType: "profType1",
        profLoc: {
            locType: ProfLocType.TEAM_CONFIG,
            osLoc: ["somewhere in the OS 1", "somewhere in the OS 2"],
            jsonLoc: "somewhere in the JSON file"
        },
        isDefaultProfile: true
    };

    it("should return default value when no TSO profile found", async () => {
        jest.spyOn(ProfileInfo.prototype, 'getDefaultProfile').mockReturnValue(null);

        expect(await getAccount()).toBe("account");
    });

    it("should return default value when error is caught", async () => {
        jest.spyOn(ProfileInfo.prototype, 'readProfilesFromDisk').mockRejectedValue("error");

        expect(await getAccount()).toBe("account");
    });

    it("should successfully parse the file", async () => {
        const profMergedArg: IProfMergedArg = {
            knownArgs:
                [
                    {
                        argName: "account",
                        dataType: "string",
                        argValue: "fake",
                        argLoc: { locType: 0, osLoc: ["location"], jsonLoc: "jsonLoc" },
                        secure: false,
                    },
                ],
            missingArgs: []
        };
        jest.spyOn(ProfileInfo.prototype, 'readProfilesFromDisk').mockImplementation(() => Promise.resolve());
        jest.spyOn(ProfileInfo.prototype, 'getDefaultProfile').mockReturnValue(profAttrs);
        jest.spyOn(ProfileInfo.prototype, 'mergeArgsForProfile').mockReturnValue(profMergedArg);

        const result = await getAccount();

        expect(result).toBe("fake");
        expect(result).toMatchSnapshot();
    });

    it("should return account if account number not defined", async () => {
        const profMergedArg: IProfMergedArg = {
            knownArgs:
                [
                    {
                        argName: "account",
                        dataType: "string",
                        argValue: "",
                        argLoc: { locType: 0, osLoc: ["location"], jsonLoc: "jsonLoc" },
                        secure: false,
                    },
                ],
            missingArgs: []
        };
        jest.spyOn(ProfileInfo.prototype, 'readProfilesFromDisk').mockImplementation(() => Promise.resolve());
        jest.spyOn(ProfileInfo.prototype, 'getDefaultProfile').mockReturnValue(profAttrs);
        jest.spyOn(ProfileInfo.prototype, 'mergeArgsForProfile').mockReturnValue(profMergedArg);

        const result = await getAccount();

        expect(result).toBe("account");
        expect(result).toMatchSnapshot();
    });

});
