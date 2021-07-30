/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import * as enzyme from 'enzyme';
import React from 'react';
import YAMLVisualizer from './YAMLVisualizer';

describe('>>> YAML Visualizer tests', () => {
    it('should generate YAML on mount', () => {
        const createYamlObject = jest.fn();
        const dummyData = {
            text: 'Basic info',
            content: {
                testInput: {
                    value: 'input',
                    question: '',
                },
            },
            multiple: false,
        };
        enzyme.shallow(
            <YAMLVisualizer createYamlObject={createYamlObject} inputData={dummyData} />
        );
        expect(createYamlObject).toHaveBeenCalledWith(dummyData);
    });

    it('should display YAML from props', () => {
        const wrapper = enzyme.shallow(
            <YAMLVisualizer createYamlObject={jest.fn()} yamlObject="test:yaml" />
        );
        expect(wrapper.find('.yamlContainer').text()).toEqual('test:yaml\n');
    });
});
