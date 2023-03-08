/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import {ImperativeError, TextUtils} from "@zowe/imperative";
import { Commands } from "./Commands";
import {getAccount} from "./JobUtil";
import * as fs from "fs";
import {CsvParser, IIdentity} from "./CsvParser";
import {JclWriter} from "./JclWriter";

export class Mapper {
    constructor(
        public file: string,
        public esm: string,
        public system: string,
        public registry: string) {
    }

    async map(): Promise<string> {
        const identities = new CsvParser(this.file).getIdentities();
        const commands = this.createSafCommands(identities);
        const jcl = await this.createJcl(commands);
        const fileMsg = this.system ? `_${this.system}` : "";
        const fileName = `idf_${this.esm}${fileMsg}.jcl`;
        fs.writeFileSync(fileName, jcl);

        const systemMsg = this.system ? ` on the system ${this.system}` : "";
        return `'${fileName}' was created. Review and submit this JCL${systemMsg}.`;
    }

    async createJcl(commands: string[]): Promise<string> {
        const jclTemplate = fs.readFileSync('src/api/templates/job.jcl').toString();
        const account = await getAccount();
        const jclWriter = new JclWriter(1, 2);
        commands.forEach(c => jclWriter.add(c));
        return TextUtils.renderWithMustache(jclTemplate, {
            account: account,
            sysaff: this.system ? `\n/*JOBPARM SYSAFF=${this.system.toUpperCase()}` : "",
            system: this.system ? `system ${this.system}` : "a corresponding system",
            commands: jclWriter.getText()
        });
    }

    createSafCommands(identities: IIdentity[]): string[] {
        let commandProcessor;
        switch (this.esm.toLowerCase()) {
            case "racf": {
                const racfTemplate = fs.readFileSync('src/api/templates/racf.jcl').toString();
                const racfRefreshCommand = fs.readFileSync('src/api/templates/racf_refresh.jcl').toString();
                commandProcessor = new Commands(this.registry, identities, racfTemplate, racfRefreshCommand);
                break;
            }
            case "tss": {
                const tssTemplate = fs.readFileSync('src/api/templates/tss.jcl').toString();
                const tssRefreshCommand = fs.readFileSync('src/api/templates/tss_refresh.jcl').toString();
                commandProcessor = new Commands(this.registry, identities,tssTemplate, tssRefreshCommand);
                break;
            }
            case "acf2": {
                break; //TODO: Here will be the code which generate ACF2 commands
            }
            default: {
                const msg = `Unsupported ESM "${this.esm}".` +
                    `Id Federation Plugin supports only the following security systems: RACF, TSS, and ACF2.`;
                throw new ImperativeError({msg});
            }
        }

        return commandProcessor ? commandProcessor.getCommands() : [];
    }


}
