/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
/*
 * Copyright 2026 Contributors to the Zowe Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const APIML_BINARY_COMPONENTS = [
    'org.zowe.apiml.apiml-package',
    'org.zowe.apiml.api-catalog-package',
    'org.zowe.apiml.discovery-package',
    'org.zowe.apiml.gateway-package',
    'org.zowe.apiml.zaas-package',
    'org.zowe.apiml.caching-service-package',
    'org.zowe.apiml.apiml-common-lib-package',
    'org.zowe.apiml.sdk.apiml-sample-extension-package',
];

const APIML_IMAGES = [
    {key: 'api-catalog', image: 'ompzowe/api-catalog-services'},
    {key: 'gateway', image: 'ompzowe/gateway-service'},
    {key: 'discovery', image: 'ompzowe/discovery-service'},
    {key: 'caching', image: 'ompzowe/caching-service'},
    {key: 'zaas', image: 'ompzowe/zaas-service'},
];

const SOURCE_COMPONENT_GROUP = 'Zowe API Mediation Layer';
const SOURCE_REPOSITORY = 'api-layer';

const VERSION_PATTERN = /^\d+\.\d+\.\d+(-[0-9A-Za-z.]+)?$/;

function escapeRegExp(text) {
    return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/**
 * Replaces the value of `field` in the JSON object that `anchor` matches into.
 * `[^{}]*?` keeps the match inside the anchored object so it cannot bleed into a
 * sibling entry, and the match count is asserted so an ambiguous or missing
 * anchor is an error rather than a silent no-op.
 */
function replaceFieldNearAnchor(text, anchor, field, newValue, label) {
    const pattern = `(${anchor}[^{}]*?"${field}"\\s*:\\s*")[^"]*(")`;
    const matches = text.match(new RegExp(pattern, 'g'));
    if (!matches) {
        throw new Error(`${label}: could not find "${field}" - has the manifest layout changed?`);
    }
    if (matches.length > 1) {
        throw new Error(`${label}: "${field}" matched ${matches.length} times, expected exactly 1`);
    }
    return text.replace(new RegExp(pattern), (whole, before, after) => before + newValue + after);
}

/**
 * Re-parses the edited manifest and asserts every value this script is
 * responsible for. Catches both a broken replacement and a manifest whose shape
 * has drifted away from what the allow-lists assume.
 */
function verify(manifestText, version) {
    const manifest = JSON.parse(manifestText);

    for (const id of APIML_BINARY_COMPONENTS) {
        const actual = manifest.binaryDependencies?.[id]?.version;
        if (actual !== version) {
            throw new Error(`binaryDependencies["${id}"].version is "${actual}", expected "${version}"`);
        }
    }

    for (const {key} of APIML_IMAGES) {
        const actual = manifest.imageDependencies?.[key]?.tag;
        if (actual !== `${version}-ubuntu`) {
            throw new Error(`imageDependencies["${key}"].tag is "${actual}", expected "${version}-ubuntu"`);
        }
    }

    const group = manifest.sourceDependencies?.find(entry => entry.componentGroup === SOURCE_COMPONENT_GROUP);
    if (!group) {
        throw new Error(`sourceDependencies component group "${SOURCE_COMPONENT_GROUP}" not found`);
    }
    const repository = group.entries?.find(entry => entry.repository === SOURCE_REPOSITORY);
    if (repository?.tag !== `v${version}`) {
        throw new Error(`sourceDependencies "${SOURCE_REPOSITORY}".tag is "${repository?.tag}", expected "v${version}"`);
    }
}

/**
 * Returns the manifest text with every API ML version set to `version`.
 * Throws if any expected entry is missing, ambiguous or left unchanged.
 */
export function bumpApimlVersions(manifestText, version) {
    if (!VERSION_PATTERN.test(version ?? '')) {
        throw new Error(`Invalid release version "${version}" - expected e.g. 3.5.24, with no leading "v"`);
    }

    let text = manifestText;

    for (const id of APIML_BINARY_COMPONENTS) {
        text = replaceFieldNearAnchor(
            text, `"${escapeRegExp(id)}"\\s*:\\s*\\{`, 'version', version, `binaryDependencies["${id}"]`);
    }

    for (const {key, image} of APIML_IMAGES) {
        text = replaceFieldNearAnchor(
            text, `"name"\\s*:\\s*"${escapeRegExp(image)}"`, 'tag', `${version}-ubuntu`, `imageDependencies["${key}"]`);
    }

    text = replaceFieldNearAnchor(
        text, `"repository"\\s*:\\s*"${escapeRegExp(SOURCE_REPOSITORY)}"`, 'tag', `v${version}`,
        `sourceDependencies["${SOURCE_COMPONENT_GROUP}"]`);

    verify(text, version);
    return text;
}
