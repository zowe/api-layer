import flowtype from "eslint-plugin-flowtype";
import header from "eslint-plugin-header";
import jsxA11Y from "eslint-plugin-jsx-a11y";
import prettier from "eslint-plugin-prettier";
import react from "eslint-plugin-react";
import { fixupPluginRules } from "@eslint/compat";
import globals from "globals";
import babelParser from "@babel/eslint-parser";
import path from "node:path";
import { fileURLToPath } from "node:url";
import js from "@eslint/js";
import { FlatCompat } from "@eslint/eslintrc";

// Workaround, as Header/header rules no longer supported https://github.com/Stuk/eslint-plugin-header/issues/57
header.rules.header.meta.schema = false;
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const compat = new FlatCompat({
    baseDirectory: __dirname,
    recommendedConfig: js.configs.recommended,
    allConfig: js.configs.all
});

export default [...compat.extends("airbnb", "prettier"), {
    plugins: {
        flowtype: fixupPluginRules(flowtype),
        header,
        "jsx-a11y": jsxA11Y,
        prettier,
        react,
    },

    languageOptions: {
        globals: {
            ...globals.browser,
            ...globals.node,
            ...globals.jquery,
            ...globals.jest,
        },

        parser: babelParser,
        ecmaVersion: 6,
        sourceType: "module",

        parserOptions: {
            ecmaFeatures: {
                experimentalObjectRestSpread: true,
                impliedStrict: true,
                classes: true,
                jsx: true,
            },
        },
    },

    rules: {
        "class-methods-use-this": 0,
        "header/header": [2, "../../license_header.txt"],

        "import/no-extraneous-dependencies": ["error", {
            devDependencies: true,
        }],

        "import/no-named-as-default": 1,
        "no-template-curly-in-string": "off",

        "no-unused-vars": [1, {
            argsIgnorePattern: "res|next|^err",
        }],

        "arrow-body-style": [1, "as-needed"],

        "no-param-reassign": [2, {
            props: false,
        }],

        "no-console": 2,
        import: 0, // You can remove this if it's not needed
        "func-names": 0,
        "space-before-function-paren": 0,
        "comma-dangle": 0,
        "max-len": 0,
        "import/extensions": 0,
        "no-underscore-dangle": 0,
        "consistent-return": 0,
        "react/display-name": 1,
        "linebreak-style": "off",
        "react/react-in-jsx-scope": 0,
        "react/forbid-prop-types": 0,
        "react/no-unescaped-entities": 0,
        "react/prefer-stateless-function": 0,

        "react/jsx-filename-extension": [1, {
            extensions: [".js", ".jsx"],
        }],

        "react/no-unused-prop-types": 2,
        radix: 0,

        "no-shadow": [2, {
            hoist: "all",
            allow: ["resolve", "reject", "done", "next", "err", "error"],
        }],

        quotes: [2, "single", {
            avoidEscape: true,
            allowTemplateLiterals: true,
        }],

        "prettier/prettier": ["error", {
            endOfLine: "auto",
            trailingComma: "es5",
            singleQuote: true,
            printWidth: 120,
        }],

        "jsx-a11y/href-no-hash": "off",

        "jsx-a11y/anchor-is-valid": ["warn", {
            aspects: ["invalidHref"],
        }],

        "react/prop-types": 0,
        "react/destructuring-assignment": 0,
        "react/jsx-props-no-spreading": 0,
        "react/sort-comp": 0,
        "jsx-a11y/label-has-associated-control": 0,
    },
}];
