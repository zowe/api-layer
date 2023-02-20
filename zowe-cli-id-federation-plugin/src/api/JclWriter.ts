/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

export class JclWriter {

    private commands: string = '';

    constructor(
        private firstIdent: number = 4,
        private nextIdent: number = 2
    ) {
    }

    parse(command: string): string[] {
        const words = [];
        let lastWord = "";
        let status = 0;
        for (let i = 0; i < command.length; i++) {
            switch (status) {
                case 0:
                    if (command.charAt(i) == '\'') {
                        status = 1;
                        lastWord += '\'';
                    } else if (command.charAt(i) == ' ') {
                        if (lastWord) {
                            words.push(lastWord);
                            lastWord = '';
                        }
                    } else {
                        lastWord += command.charAt(i);
                    }
                    break;
                case 1:
                    lastWord += command.charAt(i);
                    if (command.charAt(i) == '\'') {
                        if ((command.length > i + 1) && (command.charAt(i + 1) == '\'')) {
                            lastWord += '\'';
                            i++;
                        } else {
                            status = 0;
                        }
                    }
                    break;
            }
        }
        if (lastWord) words.push(lastWord);
        return words;
    }

    format(words: string[]): string {
        let formatted = "".padStart(this.firstIdent, ' ');
        let position = this.firstIdent;
        let blank = true;

        for (let i = 0; i < words.length; i++) {
            let word = words[i];
            if (!blank) {
                // if previous word is on the same line add separator
                formatted += ' ';
                position++;
            }

            if (position + word.length + (i == words.length - 1 ? 0 : " -".length) < 72) {
                // if there is enough room for the word (including line breaking if necessary)
                formatted += word;
                position += word.length;
                blank = false;
                continue;
            }

            if (!blank) {
                // line is not blank, but the word has not enough room
                if (position <= 71 - 1) {
                    // on line is room for line breaking, add it and continue on the next line
                    formatted += '-\n' + ''.padEnd(this.nextIdent, ' ');
                    position = this.nextIdent;
                } else {
                    // there is no room for line breaking character, use JCL line break character
                    // it can happened immediately after to long (or split) word, see next treatment
                    formatted += ''.padEnd(71 - position, ' ') + 'X\n';
                    position = 0;
                }
                blank = true;
            }

            // try to write the whole long word into lines. The first line could be shorter
            for (let j = 71 - position; word.length > 0; j = 71) {
                if (j >= word.length) {
                    // the last piece of word could be written in the line
                    formatted += word;
                    position = word.length;
                    blank = false;
                    break;
                } else {
                    // there is no enough room for the whole word, put first part and use JCL breaking character
                    formatted += word.substring(0, j) + 'X\n';
                    word = word.substring(j, word.length);
                }
            }
        }

        return formatted;
    }

    add(command: string) {
        command = command.replaceAll("\r", " ").replaceAll("\n", " ");

        const words = this.parse(command);
        const formatted = this.format(words);

        if (this.commands) {
            this.commands += '\n';
        }
        this.commands += formatted;
    }

    getText(): string {
        return this.commands;
    }

}