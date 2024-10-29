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
import {IHandlerResponseApi} from "@zowe/imperative/lib/cmd/src/doc/response/api/handler/IHandlerResponseApi";
import {Constants} from "./Constants";
import {TssCommands} from "./TssCommands";
import {Acf2Commands} from "./Acf2Commands";

export class Mapper {
    constructor(
        public file: string,
        public esm: string,
        public system: string,
        public registry: string,
        public response: IHandlerResponseApi) {
    }

    async map(): Promise<string> {
        const identities = new CsvParser(this.file,this.response).getIdentities();
        const commands = this.createSafCommands(identities);
        const jcl = await this.createJcl(commands);
        const fileMsg = this.system ? `_${this.system}` : "";
        const fileName = `idf_${this.esm}${fileMsg}.jcl`;
        fs.writeFileSync(fileName, jcl);

        const systemMsg = this.system ? ` on the system ${this.system}` : "";
        return `'${fileName}' was created. Review and submit this JCL${systemMsg}.`;
    }

    async createJcl(commands: string[]): Promise<string> {
        const jclTemplate = fs.readFileSync(`${__dirname}/templates/job.jcl`).toString();
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

        switch (this.esm.toLowerCase()) {
            case "racf": {
                return new RacfCommands(this.registry, identities, this.response).getCommands();
            }
            case "tss": {
                return new TssCommands(this.registry, identities, this.response).getCommands();
            }
            case "acf2": {
                return new Acf2Commands(this.registry, identities, this.response).getCommands();
            }
            default: {
                this.response.data.setExitCode(Constants.FATAL_CODE);
                const msg = `Unsupported ESM "${this.esm}".` +
                    `Id Federation Plugin supports only the following security systems: RACF, TSS, and ACF2.`;
                throw new ImperativeError({msg});
            }
        }
    }
}
