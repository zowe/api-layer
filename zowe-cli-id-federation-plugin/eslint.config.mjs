/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import typescriptEslint from "@typescript-eslint/eslint-plugin";
import jest from "eslint-plugin-jest";
import licenseHeader from "eslint-plugin-license-header";
import unusedImports from "eslint-plugin-unused-imports";
import globals from "globals";
import tsParser from "@typescript-eslint/parser";
import path from "node:path";
import { fileURLToPath } from "node:url";
import js from "@eslint/js";
import { FlatCompat } from "@eslint/eslintrc";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const compat = new FlatCompat({
    baseDirectory: __dirname,
    recommendedConfig: js.configs.recommended,
    allConfig: js.configs.all
});

export default [{
    ignores: ["**/*.js", "**/*.d.ts"],
}, ...compat.extends("eslint:recommended", "plugin:@typescript-eslint/recommended"), {
    plugins: {
        "@typescript-eslint": typescriptEslint,
        jest,
        "license-header": licenseHeader,
        "unused-imports": unusedImports,
    },

    languageOptions: {
        globals: {
            ...globals.node,
        },

        parser: tsParser,
        ecmaVersion: 12,
        sourceType: "module",
    },

    rules: {
        "license-header/header": ["error", "LICENSE_HEADER"],
        "max-len": ["warn", 150],
        "no-console": "error",
        "no-multiple-empty-lines": "warn",
        "no-trailing-spaces": "warn",
        "@typescript-eslint/ban-types": "off",
        "comma-dangle": ["warn", "only-multiline"],
        "@typescript-eslint/explicit-module-boundary-types": "off",
        "indent": ["warn", 4],
        "@typescript-eslint/no-explicit-any": "off",
        "@typescript-eslint/no-inferrable-types": "off",

        "@typescript-eslint/no-magic-numbers": ["warn", {
            ignore: [-1, 0, 1, 2],
            ignoreDefaultValues: true,
            ignoreReadonlyClassProperties: true,
        }],

        "@typescript-eslint/no-unused-vars": "off",
        "@typescript-eslint/no-var-requires": "off",
        "semi": "warn",
        "unused-imports/no-unused-imports": "warn",

        "unused-imports/no-unused-vars": ["warn", {
            args: "none",
        }],
    },
}, ...compat.extends("plugin:jest/recommended").map(config => ({
    ...config,
    files: ["**/__tests__/**/*.ts"],
})), {
    files: ["**/__tests__/**/*.ts"],

    rules: {
        "@typescript-eslint/no-magic-numbers": "off",

        "jest/expect-expect": ["warn", {
            assertFunctionNames: ["expect*", "**.*expect*"],
        }],

        "jest/no-conditional-expect": "off",
        "jest/no-standalone-expect": "off",
        "jest/no-try-expect": "off",
        "unused-imports/no-unused-vars": "off",
    },
}];
