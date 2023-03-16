/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

const MAX_LINE_LENGTH = 71;

export class JclWriter {

    private commands: string = '';

    constructor(
        private firstIdent: number = 4,
        private nextIdent: number = 2
    ) {
    }

    parse(command: string): string[] {
        const words: string[] = [];
        let lastWord = "";
        const status = 0;
        lastWord = JclWriter.getLastWord(command, status, lastWord, words);
        // if lastWord still contains a word add it to response (text without spaces on end or even opened string)
        if (lastWord) words.push(lastWord);
        return words;
    }

    private static getLastWord(command: string, status: number, lastWord: string, words: string[]) {
        let i = 0;
        while (i < command.length) {
            if (status === 0) {// outside the string
                [status, lastWord] = JclWriter.parseCommand(command, i, status, lastWord, words);
            } else if (status === 1) {// inside the string
                lastWord += command.charAt(i);
                if (command.charAt(i) == '\'') {
                    if ((command.length > i + 1) && (command.charAt(i + 1) == '\'')) {
                        // two apos means escaped apos, still parsing the string
                        lastWord += '\'';
                        i++;
                    } else {
                        // ending apos, stop parsing the string
                        status = 0;
                    }
                }
            }
            i++;
        }
        return lastWord;
    }

    private static parseCommand(command: string, i: number, status: number, lastWord: string, words: string[]) {
        if (command.charAt(i) == '\'') {
            // on apos switch to state 1 - parsing a string
            status = 1;
            lastWord += '\'';
        } else if (command.charAt(i) == ' ') {
            // space means to store lastWord as fully parsed and wait for the next word
            if (lastWord) {
                words.push(lastWord);
                lastWord = '';
            }
        } else {
            // everything else is a part of the word
            lastWord += command.charAt(i);
        }
        return [status, lastWord] as const;
    }

    format(words: string[]): string {
        let formatted = "".padStart(this.firstIdent, ' ');
        let position = this.firstIdent;
        let blank = true;

        for (let i = 0; i < words.length; i++) {
            const word = words[i];
            if (!blank) {
                // if previous word is on the same line add separator
                formatted += ' ';
                position++;
            }

            if (position + word.length + (i == words.length - 1 ? 0 : " -".length) <= MAX_LINE_LENGTH) {
                // if there is enough room for the word (including line breaking if necessary)
                formatted += word;
                position += word.length;
                blank = false;
                continue;
            }

            if (!blank) {
                // line is not blank, but the word has not enough room
                if (position <= MAX_LINE_LENGTH - 1) {
                    // on line is room for line breaking, add it and continue on the next line
                    formatted += '-\n' + ''.padEnd(this.nextIdent, ' ');
                    position = this.nextIdent;
                }
                blank = true;
            }

            // try to write the whole long word into lines. The first line could be shorter
            [position, formatted, blank] = JclWriter.formatWord(position, word, i, words, formatted, blank);
        }

        return formatted;
    }

    private static formatWord(position: number, word: string, i: number, words: string[], formatted: string, blank: boolean) {
        let j = MAX_LINE_LENGTH - position - 1;
        while (word.length > 0) {
            if ((i < words.length - 1) && (position + word.length > MAX_LINE_LENGTH - 2) && (word.length < MAX_LINE_LENGTH)) {
                // if the latest piece of word would fulfill the line and it cannot be split, rather split the piece to two lines
                j = word.length - 1;
            }

            if (j >= word.length) {
                // the last piece of word could be written in the line
                formatted += word;
                position = word.length;
                blank = false;
                break;
            } else {
                // there is no enough room for the whole word, put first part and use JCL breaking character
                formatted += word.substring(0, j) + '-\n';
                position = 0;
                word = word.substring(j, word.length);
            }

            j = MAX_LINE_LENGTH - 1;
        }

        return [position, formatted, blank] as const;
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
