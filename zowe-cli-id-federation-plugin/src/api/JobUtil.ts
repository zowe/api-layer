/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import {ProfileInfo} from "@zowe/imperative";

export async function getAccount(): Promise<string> {
    let profInfo;
    try {
        profInfo = new ProfileInfo('zowe', {overrideWithEnv: true});
        await profInfo.readProfilesFromDisk();
    } catch (error) {
        return "account";
    }

    const tsoProfAttrs = profInfo.getDefaultProfile("tso");
    if (!tsoProfAttrs) return "account";

    const tsoMergedArgs = profInfo.mergeArgsForProfile(tsoProfAttrs);
    const accountNumberFromProfile = tsoMergedArgs.knownArgs.find(
        arg => arg.argName === "account").argValue as string;

    return accountNumberFromProfile ?? "account";
}
