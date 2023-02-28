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
import {RacfCommands} from "./RacfCommands";
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
        const fileName = `idf_${this.esm}${this.system ? `_${this.system}` : ""}.jcl`;
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
                commandProcessor = new RacfCommands(this.registry, identities);
                break;
            }
            case "tss": {
                break; //TODO: Here will be the code which generate TSS commands
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
