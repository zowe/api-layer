/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import { JclWriter } from "../../src/api/JclWriter";
describe("JclWriter", () => {

    describe("parse", () => {

        it("empty input generates empty array", () => {
            expect(new JclWriter().parse("")).toStrictEqual([]);
        });

        it("split to words by spaces", () => {
            expect(new JclWriter().parse("Hello world!")).toStrictEqual([
                'Hello', 'world!'
            ]);
        });

        it("parse words respecting strings within escaped charater", () => {
            expect(new JclWriter().parse("abc'def'ghi 'hello' 'John''s text' x'y''z'''._ end")).toStrictEqual([
                'abc\'def\'ghi', '\'hello\'', '\'John\'\'s text\'', 'x\'y\'\'z\'\'\'._', 'end'
            ]);
        });

        it("parse unfinished string as well", () => {
            expect(new JclWriter().parse("hello 'world!")).toStrictEqual([
                'hello', '\'world!'
            ]);
        });

        it("ignore multiple spaces", () => {
            expect(new JclWriter().parse("a b  c   d e")).toStrictEqual([
                'a', 'b', 'c', 'd', 'e'
            ]);
        });

    });

    describe("format", () => {

        it("the second long word is split on the second line", () => {
            const jclWriter = new JclWriter(4, 2);
            const formated = jclWriter.format(["word1", "word2_".padEnd(200, "1234567890"), "word3"]);
            expect(formated).toBe(
                // (first) indent, word1 and enter for the second long word
                '    word1 -\n' +
                // (next) indent word with first part of word2 (JCL sign X on 72. character to break line)
                '  word2_12345678901234567890123456789012345678901234567890123456789012-\n' +
                // second part of word2
                '3456789012345678901234567890123456789012345678901234567890123456789012-\n' +
                // third part of word2 and word3
                '34567890123456789012345678901234567890123456789012345678901234 word3'
            );
        });

        it("too long word followed with other one with enough room for enter character", () => {
            const jclWriter = new JclWriter(4, 2);
            const formater = jclWriter.format([
                'word1_'.padEnd(2 * 71 - 4 - 3, '1234567890'),
                'word2'
            ]);
            expect(formater).toBe(
                '    word1_123456789012345678901234567890123456789012345678901234567890-\n' +
                '123456789012345678901234567890123456789012345678901234567890123456789 -\n' +
                '  word2'
            );
        });

        it("too long word followed with the last short word", () => {
            const jclWriter = new JclWriter(4, 2);
            const formater = jclWriter.format([
                'word1_'.padEnd(2 * 71 - 4 - 2 - 1, '1234567890'),
                '2'
            ]);
            expect(formater).toBe(
                '    word1_123456789012345678901234567890123456789012345678901234567890-\n' +
                '123456789012345678901234567890123456789012345678901234567890123456789 2'
            );
        });

        it("too long word followed with the two next short word that requires split line", () => {
            const jclWriter = new JclWriter(4, 2);
            const formater = jclWriter.format([
                'word1_'.padEnd(2 * 71 - 4 - 2 - 1, '1234567890'),
                '2', '3'
            ]);
            expect(formater).toBe(
                '    word1_123456789012345678901234567890123456789012345678901234567890-\n' +
                '123456789012345678901234567890123456789012345678901234567890123456789 -\n' +
                '  2 3'
            );
        });

        it("multiple words to be splited to multiple lines", () => {
            const jclWriter = new JclWriter(4, 2);
            const formater = jclWriter.format([
                'Lorem', 'ipsum', 'dolor', 'sit', 'amet,', 'consectetuer', 'adipiscing', 'elit.', 'Nemo',
                'enim', 'ipsam', 'voluptatem', 'quia', 'voluptas', 'sit', 'aspernatur', 'aut', 'odit', 'autodit',
                'fugit'
            ]);
            expect(formater).toBe(
                '    Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Nemo -\n' +
                '  enim ipsam voluptatem quia voluptas sit aspernatur aut odit autodit -\n' +
                '  fugit'
            );
        });

        it("a long word is split one more time to avoid missing enter character", () => {
            const jclWriter = new JclWriter(4, 2);
            const formated = jclWriter.format(["word1_".padEnd(71 - 4 + 70, "1234567890"), "word2"]);
            expect(formated).toBe(
                '    word1_123456789012345678901234567890123456789012345678901234567890-\n' +
                '1234567890123456789012345678901234567890123456789012345678901234567890-\n' +
                '1 word2'
            );
        });

    });

    describe("add", () => {

        it("ignore line break characters", () => {
            const jclWriter = new JclWriter(4, 2);
            jclWriter.add("hello\nworld\r!\n!");
            expect(jclWriter.getText()).toBe(
                '    hello world ! !'
            );
        });

        it("multiple commands", () => {
            const jclWriter = new JclWriter(4, 2);
            jclWriter.add("command no(1) user('user O''neal') attribute_A(111111) attribute_B(222222)");
            jclWriter.add("command no(2) user('zweuser')");
            expect(jclWriter.getText()).toBe(
                "    command no(1) user('user O''neal') attribute_A(111111) -\n" +
                "  attribute_B(222222)\n" +
                "    command no(2) user('zweuser')"
            );
        });

    });

});